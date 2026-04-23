#pragma once
// ─────────────────────────────────────────────────────────────
//  Game Loop  (per IO-Server.md pseudocode)
//
//  while (running) {
//    1. Network: process incoming UDP inputs
//    2. Logic:   ECS systems (Input → Physics → Collision → Decay)
//    3. DB:      async save every 10s (push to worker queue)
//    4. Output:  build delta, broadcast via UDP + TCP
//    5. Sleep:   maintain fixed tick rate
//  }
// ─────────────────────────────────────────────────────────────
#include <entt/entt.hpp>
#include <atomic>
#include <thread>
#include <mutex>
#include <vector>
#include <unordered_map>
#include <functional>
#include <cstdint>
#include "Components.h"
#include "SpatialGrid.h"
#include "Systems.h"
#include "Database.h"
#include "RedisClient.h"

// Snapshot of one entity for delta comparison
struct EntitySnapshot {
    float x, y, radius, mass;
    bool  alive = true;
};

// Callbacks to send serialized packets — set by TcpServer/UdpServer
struct NetworkCallbacks {
    // Send delta update (protobuf binary) over UDP DataChannel to one player
    std::function<void(uint32_t player_id, const std::vector<uint8_t>& data)>
        send_udp_delta;

    // Send full snapshot over UDP to one player (on first join)
    std::function<void(uint32_t player_id, const std::vector<uint8_t>& data)>
        send_udp_snapshot;

    // Send TCP packet (death, leaderboard, chat, etc.)
    std::function<void(uint32_t player_id, const std::vector<uint8_t>& data)>
        send_tcp;

    // Broadcast TCP to all players
    std::function<void(const std::vector<uint8_t>& data)>
        broadcast_tcp;
};

class GameLoop {
public:
    GameLoop(Database& db, RedisClient& redis);
    ~GameLoop();

    void set_callbacks(NetworkCallbacks cbs) { cbs_ = std::move(cbs); }

    void start();
    void stop();

    // Called by TcpServer when a player joins
    uint32_t on_player_join(const std::string& name,
                             const std::string& skin,
                             uint32_t color, uint8_t team,
                             uint8_t game_mode);

    // Called by TcpServer when a player disconnects
    void on_player_disconnect(uint32_t player_id);

    // Push UDP input (non-blocking, safe from network thread)
    void push_input(InputPacket pkt);

    // Send initial snapshot immediately (call AFTER session is registered)
    void send_initial_snapshot(uint32_t player_id);

    // Public registry access (read-only snapshot for first UDP packet)
    void with_registry(std::function<void(entt::registry&, SpatialGrid&)> fn);

    entt::registry& registry() { return reg_; }
    std::mutex&     world_mutex() { return world_mtx_; }

private:
    void loop();
    void build_and_broadcast_delta(const WorldEvents& events);
    void build_and_send_snapshot(uint32_t player_id);
    void update_redis_leaderboard();
    void async_save_snapshot();

    entt::registry   reg_;
    SpatialGrid      grid_;
    std::mutex       world_mtx_;

    std::vector<InputPacket> input_queue_;
    std::mutex               input_mtx_;

    // Players waiting for initial snapshot
    std::vector<uint32_t>    pending_snapshot_;
    std::mutex               snapshot_mtx_;

    Database&        db_;
    RedisClient&     redis_;
    NetworkCallbacks cbs_;

    std::thread      thread_;
    std::atomic<bool> running_{ false };

    uint64_t tick_       = 0;
    uint32_t id_counter_ = 0;

    // Previous tick snapshots for delta computation
    std::unordered_map<uint32_t, EntitySnapshot> prev_snapshots_;
};
