#include "Systems.h"
#include <algorithm>
#include <cmath>
#include <random>

static std::mt19937 rng{ std::random_device{}() };

// ─────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────
static float mass_to_radius(float m) {
    return std::sqrt(m) * Config::MASS_RADIUS_SCALE;
}
static float radius_to_mass(float r) {
    float s = r / Config::MASS_RADIUS_SCALE;
    return s * s;
}
static float cell_speed(float mass) {
    return Config::SPEED_BASE * std::pow(mass, Config::SPEED_DECAY);
}
static Vec2 rand_pos() {
    std::uniform_real_distribution<float> dx(0.f, Config::WORLD_W);
    std::uniform_real_distribution<float> dy(0.f, Config::WORLD_H);
    return { dx(rng), dy(rng) };
}
static uint32_t rand_color() {
    static const uint32_t palette[] = {
        0xFFE74C3C, 0xFF3498DB, 0xFF2ECC71, 0xFFF39C12,
        0xFF9B59B6, 0xFF1ABC9C, 0xFFE91E63, 0xFFFF5722,
        0xFF00BCD4, 0xFF8BC34A, 0xFFFF9800, 0xFF607D8B
    };
    std::uniform_int_distribution<int> d(0, 11);
    return palette[d(rng)];
}

// ─────────────────────────────────────────────────────────────
//  Input System
// ─────────────────────────────────────────────────────────────
void InputSystem::apply(entt::registry& reg,
                         std::vector<InputPacket>& queue,
                         std::mutex& mtx,
                         SpatialGrid& grid) {
    std::vector<InputPacket> local;
    {
        std::lock_guard<std::mutex> lk(mtx);
        local.swap(queue);
    }

    // Build latest-per-player map (drop stale seq)
    std::unordered_map<uint32_t, InputPacket> latest;
    for (auto& pkt : local) {
        auto it = latest.find(pkt.player_id);
        if (it == latest.end() || pkt.seq > it->second.seq)
            latest[pkt.player_id] = pkt;
    }

    // Write to PlayerInfo components
    auto view = reg.view<PlayerInfo>();
    for (auto [entity, info] : view.each()) {
        auto it = latest.find(info.id);
        if (it == latest.end()) continue;
        auto& pkt = it->second;
        if (pkt.seq <= info.last_seq) continue; // out-of-order, discard
        info.last_seq     = pkt.seq;
        info.mouse_target = { pkt.mouse_x, pkt.mouse_y };
        info.wants_split  = pkt.split;
        info.wants_eject  = pkt.eject;
    }

    // Process split requests from PlayerInfo (set directly or via input)
    for (auto [entity, info] : reg.view<PlayerInfo>().each()) {
        if (!info.wants_split || !info.alive) continue;
        info.wants_split = false;

        // Count existing cells for this player
        std::vector<entt::entity> my_cells;
        for (auto [e, cell] : reg.view<CellComp>().each())
            if (cell.player_id == info.id) my_cells.push_back(e);

        if (static_cast<int>(my_cells.size()) >= Config::PLAYER_MAX_SPLITS) continue;

        // Split each eligible cell (large enough, room in split count)
        std::vector<entt::entity> new_cells;
        for (auto ce : my_cells) {
            if (static_cast<int>(my_cells.size() + new_cells.size())
                >= Config::PLAYER_MAX_SPLITS) break;
            if (!reg.valid(ce)) continue;

            auto& cmass = reg.get<Mass>(ce);
            if (cmass.value < Config::PLAYER_MIN_SPLIT_MASS * 2.f) continue;

            float half = cmass.value * 0.5f;
            cmass.value = half;
            reg.get<Circle>(ce).radius = mass_to_radius(half);

            // Direction toward mouse
            auto& cpos = reg.get<Pos>(ce);
            Vec2 dir = { info.mouse_target.x - cpos.x,
                         info.mouse_target.y - cpos.y };
            float len = std::sqrt(dir.x*dir.x + dir.y*dir.y);
            if (len < 1.f) { dir = {1.f, 0.f}; } else { dir.x /= len; dir.y /= len; }

            // Create split cell
            auto ne = reg.create();
            auto& ccell = reg.get<CellComp>(ce);
            CellComp ncc;
            ncc.player_id       = info.id;
            ncc.cell_index      = static_cast<uint32_t>(my_cells.size() + new_cells.size());
            ncc.recombine_timer = Config::RECOMBINE_BASE + half * Config::RECOMBINE_MASS_FACTOR;
            reg.emplace<CellComp>(ne, ncc);
            // Reset parent recombine timer too
            ccell.recombine_timer = ncc.recombine_timer;

            float split_speed = Config::SPEED_BASE * 10.f;
            float nx = cpos.x + dir.x * reg.get<Circle>(ce).radius * 2.f;
            float ny = cpos.y + dir.y * reg.get<Circle>(ce).radius * 2.f;
            nx = std::clamp(nx, 0.f, Config::WORLD_W);
            ny = std::clamp(ny, 0.f, Config::WORLD_H);

            reg.emplace<Pos>(ne, nx, ny);
            reg.emplace<Vel>(ne, dir.x * split_speed, dir.y * split_speed);
            reg.emplace<Mass>(ne, half);
            reg.emplace<Circle>(ne, mass_to_radius(half));

            grid.insert(static_cast<EntityID>(ne), nx, ny);
            new_cells.push_back(ne);
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Physics System
// ─────────────────────────────────────────────────────────────
void PhysicsSystem::update(entt::registry& reg, SpatialGrid& grid, float dt) {
    // Build player_id → mouse_target map from PlayerInfo
    std::unordered_map<uint32_t, Vec2> targets;
    for (auto [e, info] : reg.view<PlayerInfo>().each())
        if (info.alive) targets[info.id] = info.mouse_target;

    // Move every CellComp
    auto view = reg.view<CellComp, Pos, Vel, Mass>();
    for (auto [entity, cell, pos, vel, mass] : view.each()) {
        auto it = targets.find(cell.player_id);
        if (it == targets.end()) continue;

        Vec2 target = it->second;
        Vec2 diff   = { target.x - pos.x, target.y - pos.y };
        float dist  = std::sqrt(diff.x*diff.x + diff.y*diff.y);

        if (dist < 1.f) continue; // already at target

        float speed = cell_speed(mass.value);
        float move  = std::min(speed * dt, dist);
        float nx = pos.x + (diff.x / dist) * move;
        float ny = pos.y + (diff.y / dist) * move;

        // Clamp to world
        float r  = mass_to_radius(mass.value);
        nx = std::clamp(nx, r, Config::WORLD_W - r);
        ny = std::clamp(ny, r, Config::WORLD_H - r);

        // Update spatial grid
        grid.move(static_cast<uint32_t>(entity), pos.x, pos.y, nx, ny);
        pos.x = nx;
        pos.y = ny;
    }

    // Move ejected mass (decelerates)
    auto ejview = reg.view<EjectedTag, Pos, Vel, Mass>();
    for (auto [entity, etag, pos, vel, mass] : ejview.each()) {
        etag.speed *= Config::EJECT_DECAY;
        float nx = pos.x + vel.vx * etag.speed * dt;
        float ny = pos.y + vel.vy * etag.speed * dt;
        nx = std::clamp(nx, 0.f, Config::WORLD_W);
        ny = std::clamp(ny, 0.f, Config::WORLD_H);
        grid.move(static_cast<uint32_t>(entity), pos.x, pos.y, nx, ny);
        pos.x = nx;
        pos.y = ny;
    }
}

// ─────────────────────────────────────────────────────────────
//  Collision System
// ─────────────────────────────────────────────────────────────
WorldEvents CollisionSystem::resolve(entt::registry& reg, SpatialGrid& grid) {
    WorldEvents events;

    auto cell_view = reg.view<CellComp, Pos, Mass, Circle>();

    for (auto [entity, cell, pos, mass, circ] : cell_view.each()) {
        float cx = pos.x, cy = pos.y;
        auto candidates = grid.query_aoi(cx, cy);

        for (EntityID cand_id : candidates) {
            auto cand = static_cast<entt::entity>(cand_id);
            if (cand == entity) continue;
            if (!reg.valid(cand)) continue;

            float dx, dy, dist2;
            {
                auto* cpos = reg.try_get<Pos>(cand);
                if (!cpos) continue;
                dx = cx - cpos->x;
                dy = cy - cpos->y;
                dist2 = dx*dx + dy*dy;
            }

            // ── Eat food / ejected mass ───────────────────────
            if (reg.any_of<FoodTag, EjectedTag>(cand)) {
                auto* cmass = reg.try_get<Mass>(cand);
                if (!cmass) continue;
                float food_r = mass_to_radius(cmass->value);
                if (dist2 < (circ.radius - food_r * 0.5f) *
                            (circ.radius - food_r * 0.5f)) {
                    EatEvent ev;
                    ev.eater_player_id = cell.player_id;
                    ev.eaten_player_id = 0;
                    ev.eaten_entity    = cand;
                    ev.mass_gained     = cmass->value;
                    events.eats.push_back(ev);

                    // Tell the delta serializer to remove this food on clients
                    events.eaten_food_ids.push_back(cand_id);

                    mass.value += cmass->value;
                    circ.radius = mass_to_radius(mass.value);
                    grid.remove(cand_id, reg.get<Pos>(cand).x,
                                         reg.get<Pos>(cand).y);
                    reg.destroy(cand);
                }
                continue;
            }

            // ── Eat another player's cell ─────────────────────
            if (auto* other_cell = reg.try_get<CellComp>(cand)) {
                if (other_cell->player_id == cell.player_id) continue; // own cell
                auto* other_mass   = reg.try_get<Mass>(cand);
                auto* other_circle = reg.try_get<Circle>(cand);
                if (!other_mass || !other_circle) continue;

                // Must be inside + 15% larger
                float eat_dist = circ.radius - other_circle->radius * 0.5f;
                if (dist2 < eat_dist * eat_dist &&
                    circ.radius > other_circle->radius * Config::PLAYER_EAT_RATIO) {

                    EatEvent ev;
                    ev.eater_player_id = cell.player_id;
                    ev.eaten_player_id = other_cell->player_id;
                    ev.eaten_entity    = cand;
                    ev.mass_gained     = other_mass->value;
                    events.eats.push_back(ev);

                    mass.value  += other_mass->value;
                    circ.radius  = mass_to_radius(mass.value);
                    grid.remove(cand_id, reg.get<Pos>(cand).x,
                                         reg.get<Pos>(cand).y);
                    reg.destroy(cand);
                }
                continue;
            }

            // ── Hit virus ─────────────────────────────────────
            if (auto* virus = reg.try_get<VirusTag>(cand)) {
                auto* vcircle = reg.try_get<Circle>(cand);
                float r_sum   = circ.radius + (vcircle ? vcircle->radius : Config::VIRUS_RADIUS);
                if (dist2 < r_sum * r_sum &&
                    circ.radius > Config::VIRUS_SPLIT_THRESHOLD) {

                    // Explode: split into small pieces
                    // Count how many cells player already has
                    auto& info_view = reg.view<PlayerInfo>();
                    for (auto [pe, pinfo] : info_view.each()) {
                        if (pinfo.id != cell.player_id) continue;
                        // handled in RecombineSystem / split logic
                        // Mark event: virus hit
                        EatEvent ev;
                        ev.eater_player_id = cell.player_id;
                        ev.eaten_player_id = 0;
                        ev.eaten_entity    = cand;
                        ev.mass_gained     = 0.f; // no mass from virus
                        events.eats.push_back(ev);
                        break;
                    }
                }
            }
        }
    }

    return events;
}

// ─────────────────────────────────────────────────────────────
//  Decay System
// ─────────────────────────────────────────────────────────────
void DecaySystem::update(entt::registry& reg, float dt) {
    auto view = reg.view<CellComp, Mass, Circle>();
    for (auto [entity, cell, mass, circ] : view.each()) {
        if (mass.value <= Config::DECAY_THRESHOLD) continue;
        float loss = mass.value * Config::DECAY_RATE * dt;
        mass.value -= loss;
        if (mass.value < Config::DECAY_THRESHOLD)
            mass.value = Config::DECAY_THRESHOLD;
        circ.radius = mass_to_radius(mass.value);
    }
}

// ─────────────────────────────────────────────────────────────
//  Recombine System
// ─────────────────────────────────────────────────────────────
void RecombineSystem::update(entt::registry& reg, SpatialGrid& grid, float dt) {
    auto view = reg.view<CellComp, Pos, Mass, Circle>();

    // Tick timers
    for (auto [entity, cell, pos, mass, circ] : view.each())
        if (cell.recombine_timer > 0.f) cell.recombine_timer -= dt;

    // Merge overlapping cells of same player where timer <= 0
    // Collect all cells per player
    std::unordered_map<uint32_t, std::vector<entt::entity>> player_cells;
    for (auto [entity, cell, pos, mass, circ] : view.each())
        player_cells[cell.player_id].push_back(entity);

    for (auto& [pid, entities] : player_cells) {
        for (size_t i = 0; i < entities.size(); ++i) {
            for (size_t j = i + 1; j < entities.size(); ++j) {
                auto e1 = entities[i], e2 = entities[j];
                if (!reg.valid(e1) || !reg.valid(e2)) continue;

                auto& cell1 = reg.get<CellComp>(e1);
                auto& cell2 = reg.get<CellComp>(e2);
                if (cell1.recombine_timer > 0.f || cell2.recombine_timer > 0.f) continue;

                auto& p1 = reg.get<Pos>(e1);
                auto& p2 = reg.get<Pos>(e2);
                float dx = p1.x - p2.x, dy = p1.y - p2.y;
                float dist2 = dx*dx + dy*dy;

                auto& m1 = reg.get<Mass>(e1);
                auto& m2 = reg.get<Mass>(e2);
                auto& c1 = reg.get<Circle>(e1);

                if (dist2 < c1.radius * c1.radius) {
                    // Merge e2 into e1
                    m1.value += m2.value;
                    c1.radius = mass_to_radius(m1.value);
                    grid.remove(static_cast<uint32_t>(e2), p2.x, p2.y);
                    reg.destroy(e2);
                    entities[j] = entt::null;
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Spawn System
// ─────────────────────────────────────────────────────────────
void SpawnSystem::update(entt::registry& reg, SpatialGrid& grid,
                          int target_food, int target_viruses,
                          WorldEvents& events) {
    // Count existing food and viruses
    int food_count  = static_cast<int>(reg.view<FoodTag>().size());
    int virus_count = static_cast<int>(reg.view<VirusTag>().size());

    // Spawn missing food (batch, up to 20 per tick)
    int to_spawn = std::min(target_food - food_count, 20);
    for (int i = 0; i < to_spawn; ++i) {
        auto e   = reg.create();
        auto pos = rand_pos();
        float m  = Config::FOOD_MASS;
        reg.emplace<Pos>(e, pos.x, pos.y);
        reg.emplace<Mass>(e, m);
        reg.emplace<Circle>(e, Config::FOOD_RADIUS);
        reg.emplace<FoodTag>(e);
        grid.insert(static_cast<uint32_t>(e), pos.x, pos.y);
        events.new_food_ids.push_back(static_cast<uint32_t>(e));
    }

    // Spawn missing viruses
    int to_spawn_v = std::min(target_viruses - virus_count, 2);
    for (int i = 0; i < to_spawn_v; ++i) {
        auto e   = reg.create();
        auto pos = rand_pos();
        reg.emplace<Pos>(e, pos.x, pos.y);
        reg.emplace<Mass>(e, Config::VIRUS_MASS);
        reg.emplace<Circle>(e, Config::VIRUS_RADIUS);
        reg.emplace<VirusTag>(e);
        grid.insert(static_cast<uint32_t>(e), pos.x, pos.y);
    }
}

entt::entity SpawnSystem::spawn_player(entt::registry& reg, SpatialGrid& grid,
                                        PlayerInfo info, float starting_mass) {
    auto e   = reg.create();
    auto pos = rand_pos();
    float r  = mass_to_radius(starting_mass);

    reg.emplace<PlayerInfo>(e, std::move(info));
    reg.emplace<Pos>(e, pos.x, pos.y);
    reg.emplace<Vel>(e, 0.f, 0.f);
    reg.emplace<Mass>(e, starting_mass);
    reg.emplace<Circle>(e, r);

    CellComp cc;
    cc.player_id       = reg.get<PlayerInfo>(e).id;
    cc.cell_index      = 0;
    cc.recombine_timer = Config::RECOMBINE_BASE
                       + starting_mass * Config::RECOMBINE_MASS_FACTOR;
    reg.emplace<CellComp>(e, cc);

    reg.get<PlayerInfo>(e).alive = true;

    grid.insert(static_cast<uint32_t>(e), pos.x, pos.y);
    return e;
}

void SpawnSystem::kill_player(entt::registry& reg, SpatialGrid& grid,
                               uint32_t player_id) {
    std::vector<entt::entity> to_remove;
    for (auto [e, cell, pos] : reg.view<CellComp, Pos>().each())
        if (cell.player_id == player_id)
            to_remove.push_back(e);

    for (auto e : to_remove) {
        auto& pos = reg.get<Pos>(e);
        grid.remove(static_cast<uint32_t>(e), pos.x, pos.y);
        reg.destroy(e);
    }

    // Mark PlayerInfo dead
    for (auto [e, info] : reg.view<PlayerInfo>().each())
        if (info.id == player_id) info.alive = false;
}

// ─────────────────────────────────────────────────────────────
//  Battle Royale System
// ─────────────────────────────────────────────────────────────
void BRSystem::update(SafeZone& zone, entt::registry& reg,
                       SpatialGrid& grid, float dt) {
    if (zone.radius <= Config::BR_MIN_RADIUS) return;

    zone.phase_timer += dt;
    if (zone.phase_timer >= Config::BR_PHASE_DURATION) {
        zone.phase_timer = 0.f;
        ++zone.phase;
        zone.shrink_rate = Config::BR_SHRINK_RATE * (1.f + zone.phase * 0.5f);
    }

    zone.radius = std::max(zone.radius - zone.shrink_rate * dt,
                           Config::BR_MIN_RADIUS);

    // Damage cells outside zone
    auto view = reg.view<CellComp, Pos, Mass, Circle>();
    for (auto [e, cell, pos, mass, circ] : view.each()) {
        float dx = pos.x - zone.cx, dy = pos.y - zone.cy;
        float dist = std::sqrt(dx*dx + dy*dy);
        if (dist > zone.radius) {
            float damage = Config::BR_DAMAGE_PER_SEC * dt;
            mass.value = std::max(mass.value - damage, 1.f);
            circ.radius = mass_to_radius(mass.value);
        }
    }
}
