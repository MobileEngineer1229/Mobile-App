#include "GameLoop.h"
#include "Config.h"
#include "PacketSerializer.h"
#include <chrono>
#include <thread>
#include <iostream>
#include <algorithm>

using namespace std::chrono;

GameLoop::GameLoop(Database& db, RedisClient& redis)
    : db_(db), redis_(redis),
      grid_(Config::WORLD_W, Config::WORLD_H) {}

GameLoop::~GameLoop() { stop(); }

void GameLoop::start() {
    running_ = true;
    thread_  = std::thread(&GameLoop::loop, this);
}

void GameLoop::stop() {
    running_ = false;
    if (thread_.joinable()) thread_.join();
}

// ── Main loop (per IO-Server.md pseudocode) ──────────────────
void GameLoop::loop() {
    constexpr auto TICK = milliseconds(Config::TICK_MS);

    while (running_) {
        auto tick_start = steady_clock::now();

        {
            std::lock_guard<std::mutex> lk(world_mtx_);

            float dt = Config::TICK_MS / 1000.f;

            // 1. Network: apply queued UDP inputs
            InputSystem::apply(reg_, input_queue_, input_mtx_, grid_);

            // 2. Logic: ECS systems
            PhysicsSystem::update(reg_, grid_, dt);
            auto events = CollisionSystem::resolve(reg_, grid_);
            DecaySystem::update(reg_, dt);
            RecombineSystem::update(reg_, grid_, dt);
            SpawnSystem::update(reg_, grid_,
                                Config::FOOD_MAX, Config::VIRUS_MAX, events);

            // 3. Handle eat events (XP, death, leaderboard)
            std::unordered_set<uint32_t> xp_dirty; // players whose XP changed this tick
            for (auto& ev : events.eats) {
                if (ev.mass_gained > 0.f && ev.eaten_player_id == 0) {
                    // Ate food — give XP
                    for (auto [e, info] : reg_.view<PlayerInfo>().each())
                        if (info.id == ev.eater_player_id) {
                            info.xp += Config::XP_PER_FOOD;
                            xp_dirty.insert(info.id);
                        }
                }
                if (ev.eaten_player_id != 0) {
                    // Ate a player — check if all cells gone
                    bool all_dead = true;
                    for (auto [e, cell] : reg_.view<CellComp>().each())
                        if (cell.player_id == ev.eaten_player_id) {
                            all_dead = false; break;
                        }

                    if (all_dead) {
                        // Player fully eaten → fire death event
                        uint32_t dead_id   = ev.eaten_player_id;
                        uint32_t killer_id = ev.eater_player_id;

                        // Update XP
                        for (auto [e, info] : reg_.view<PlayerInfo>().each()) {
                            if (info.id == killer_id)
                                info.xp += Config::XP_PER_PLAYER_EAT;
                            if (info.id == dead_id) {
                                // Async save to MongoDB
                                ScoreRecord sr;
                                sr.player_name = info.name;
                                sr.score       = info.score;
                                sr.mode        = "FFA";
                                sr.timestamp   = duration_cast<seconds>(
                                    system_clock::now().time_since_epoch()).count();
                                db_.save_score(sr);

                                // Build death packet and send via TCP
                                if (cbs_.send_tcp) {
                                    std::string killer_name;
                                    for (auto [ke, ki] : reg_.view<PlayerInfo>().each())
                                        if (ki.id == killer_id) killer_name = ki.name;
                                    auto pkt = PacketSerializer::build_death(
                                        killer_name, info.score, 0, 0, false, 0);
                                    cbs_.send_tcp(dead_id, pkt);
                                }
                            }
                        }
                    }
                }
            }

            // Send XP update to each player whose XP changed this tick
            if (cbs_.send_tcp) {
                for (uint32_t pid : xp_dirty) {
                    for (auto [e, info] : reg_.view<PlayerInfo>().each()) {
                        if (info.id != pid) continue;
                        // Simple level-up check
                        uint32_t xp_to_next = Config::XP_BASE_TO_LEVEL;
                        for (uint32_t lv = 1; lv < info.level; ++lv)
                            xp_to_next = static_cast<uint32_t>(xp_to_next * Config::XP_LEVEL_SCALE);
                        while (info.xp >= xp_to_next) {
                            info.xp -= xp_to_next;
                            ++info.level;
                            xp_to_next = static_cast<uint32_t>(xp_to_next * Config::XP_LEVEL_SCALE);
                        }
                        auto pkt = PacketSerializer::build_xp_update(info.level, info.xp, xp_to_next);
                        cbs_.send_tcp(pid, pkt);
                        break;
                    }
                }
            }

            // 4. Send initial snapshots to newly joined players
            {
                std::lock_guard<std::mutex> slk(snapshot_mtx_);
                for (uint32_t pid : pending_snapshot_)
                    build_and_send_snapshot(pid);
                pending_snapshot_.clear();
            }

            // 5. Output: build delta and broadcast via UDP
            build_and_broadcast_delta(events);

            // 6. Leaderboard: push to Redis and broadcast via TCP every N ticks
            if (tick_ % Config::LEADERBOARD_TICKS == 0) {
                update_redis_leaderboard();
            }

            // 7. Async DB snapshot every 10 seconds (600 ticks at 60fps, 200 at 20fps)
            if (tick_ % (Config::TICK_RATE * 10) == 0) {
                async_save_snapshot();
            }

            ++tick_;
        } // release world_mtx_

        // 8. Sleep to maintain fixed tick rate
        auto elapsed = steady_clock::now() - tick_start;
        if (elapsed < TICK)
            std::this_thread::sleep_for(TICK - elapsed);
    }
}

// ─────────────────────────────────────────────────────────────
uint32_t GameLoop::on_player_join(const std::string& name,
                                   const std::string& skin,
                                   uint32_t color, uint8_t team,
                                   uint8_t mode) {
    std::lock_guard<std::mutex> lk(world_mtx_);

    // Load from DB (called at join, not in loop)
    auto rec = db_.load_player(name);

    PlayerInfo info;
    info.id      = ++id_counter_;
    info.name    = name;
    info.skin_id = skin.empty() ? rec.skin_id : skin;
    info.color   = color;
    info.team    = team;
    info.level   = rec.level;
    info.xp      = rec.xp;
    info.alive   = true;

    float starting_mass = Config::PLAYER_START_MASS +
                          (rec.level - 1) * 2.0f; // level bonus

    SpawnSystem::spawn_player(reg_, grid_, info, starting_mass);
    redis_.set_presence(info.id, "server_01");

    // Queue snapshot to be sent on next tick
    {
        std::lock_guard<std::mutex> slk(snapshot_mtx_);
        pending_snapshot_.push_back(info.id);
    }

    return info.id;
}

void GameLoop::on_player_disconnect(uint32_t player_id) {
    std::lock_guard<std::mutex> lk(world_mtx_);
    SpawnSystem::kill_player(reg_, grid_, player_id);
    redis_.del_presence(player_id);
}

void GameLoop::push_input(InputPacket pkt) {
    std::lock_guard<std::mutex> lk(input_mtx_);
    input_queue_.push_back(pkt);
}

void GameLoop::with_registry(
        std::function<void(entt::registry&, SpatialGrid&)> fn) {
    std::lock_guard<std::mutex> lk(world_mtx_);
    fn(reg_, grid_);
}

// ─────────────────────────────────────────────────────────────
void GameLoop::build_and_send_snapshot(uint32_t player_id) {
    auto pkt = PacketSerializer::build_full_snapshot(reg_, tick_);
    if (cbs_.send_udp_snapshot)
        cbs_.send_udp_snapshot(player_id, pkt);
}

void GameLoop::send_initial_snapshot(uint32_t player_id) {
    // Called from TcpServer AFTER the session is registered in sessions_,
    // so send_to() will find it. Acquires world_mtx_ to safely read the registry.
    std::lock_guard<std::mutex> lk(world_mtx_);
    build_and_send_snapshot(player_id);
}

void GameLoop::build_and_broadcast_delta(const WorldEvents& events) {
    auto pkt = PacketSerializer::build_delta(reg_, events, tick_,
                                              prev_snapshots_);
    if (!cbs_.send_udp_delta) return;

    // Send to every alive player
    for (auto [e, info] : reg_.view<PlayerInfo>().each())
        if (info.alive)
            cbs_.send_udp_delta(info.id, pkt);
}

void GameLoop::update_redis_leaderboard() {
    // Collect top 10 by total mass
    struct Entry { std::string name; float mass; };
    std::vector<Entry> entries;
    for (auto [e, info] : reg_.view<PlayerInfo>().each()) {
        if (!info.alive) continue;
        float tm = 0.f;
        for (auto [ce, cc, m] : reg_.view<CellComp, Mass>().each())
            if (cc.player_id == info.id) tm += m.value;
        entries.push_back({ info.name, tm });
    }
    std::sort(entries.begin(), entries.end(),
              [](auto& a, auto& b) { return a.mass > b.mass; });

    // Push to Redis sorted set
    for (auto& e : entries)
        redis_.zadd_score("leaderboard:ffa", e.name, e.mass);

    // Pull global top 10 (merged from all server instances)
    auto global = redis_.zrevrange("leaderboard:ffa", 0, 9);

    auto pkt = PacketSerializer::build_leaderboard(global, 0, 0.f);
    if (cbs_.broadcast_tcp)
        cbs_.broadcast_tcp(pkt);
}

void GameLoop::async_save_snapshot() {
    // Push all alive player stats to DB worker queue
    for (auto [e, info] : reg_.view<PlayerInfo>().each()) {
        if (!info.alive) continue;
        PlayerRecord rec;
        rec.player_name = info.name;
        rec.level       = info.level;
        rec.xp          = info.xp;
        rec.high_score  = info.score;
        rec.skin_id     = info.skin_id;
        db_.save_player(rec);
    }
}
