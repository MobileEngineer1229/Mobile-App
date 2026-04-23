#pragma once
// ─────────────────────────────────────────────────────────────
//  ECS Systems  (per IO-Server.md game loop structure)
//  1. InputSystem    — apply UDP inputs to components
//  2. PhysicsSystem  — move cells toward mouse target
//  3. CollisionSystem — eat food/viruses/players, split-on-virus
//  4. DecaySystem    — passive mass loss for large cells
//  5. RecombineSystem — merge split cells after timer
//  6. SpawnSystem    — respawn food/viruses to maintain counts
// ─────────────────────────────────────────────────────────────
#include <entt/entt.hpp>
#include <vector>
#include <functional>
#include <mutex>
#include "Components.h"
#include "SpatialGrid.h"
#include "Vec2.h"
#include "Config.h"

struct InputPacket {
    uint32_t player_id;
    uint32_t seq;
    float    mouse_x, mouse_y;
    bool     split, eject;
};

struct EatEvent {
    uint32_t eater_player_id;
    uint32_t eaten_player_id;   // 0 if food/virus
    entt::entity eaten_entity;
    float    mass_gained;
};

struct WorldEvents {
    std::vector<EatEvent>      eats;
    std::vector<entt::entity>  spawned_food;
    std::vector<entt::entity>  spawned_viruses;
    std::vector<entt::entity>  ejected_pellets;

    // Derived lists used by PacketSerializer for delta packets
    std::vector<uint32_t>      removed_player_ids; // player IDs that died this tick
    std::vector<uint32_t>      new_food_ids;        // entity IDs of newly spawned food
    std::vector<uint32_t>      eaten_food_ids;      // entity IDs of eaten food
};

// ── Input System ─────────────────────────────────────────────
namespace InputSystem {
    // Drain input_queue and write latest input to PlayerInfo components.
    // Only keeps newest seq per player (drops stale out-of-order packets).
    void apply(entt::registry& reg,
               std::vector<InputPacket>& input_queue,
               std::mutex& queue_mtx,
               SpatialGrid& grid);
}

// ── Physics System ───────────────────────────────────────────
namespace PhysicsSystem {
    // Move each CellComp toward owner's PlayerInfo::mouse_target.
    // Speed = Config::SPEED_BASE * mass^SPEED_DECAY.
    // Clamp to world bounds.
    // Update SpatialGrid positions.
    void update(entt::registry& reg, SpatialGrid& grid, float dt);
}

// ── Collision System ─────────────────────────────────────────
namespace CollisionSystem {
    // Broad phase via SpatialGrid, narrow phase by radius overlap.
    // Eating rules:
    //   cell eats food if cell.radius > food.radius
    //   cell eats player cell if cell.radius > other.radius * PLAYER_EAT_RATIO
    //   cell hits virus if cell.radius > VIRUS_SPLIT_THRESHOLD → explode
    WorldEvents resolve(entt::registry& reg, SpatialGrid& grid);
}

// ── Decay System ─────────────────────────────────────────────
namespace DecaySystem {
    // Cells with mass > DECAY_THRESHOLD lose DECAY_RATE * mass per second.
    void update(entt::registry& reg, float dt);
}

// ── Recombine System ─────────────────────────────────────────
namespace RecombineSystem {
    // Tick recombine timers.  When timer <= 0 and two cells of same
    // player overlap, merge them (sum mass, remove one entity).
    void update(entt::registry& reg, SpatialGrid& grid, float dt);
}

// ── Spawn System ─────────────────────────────────────────────
namespace SpawnSystem {
    // Maintain food count and virus count.
    // Newly spawned food entity IDs are appended to events.new_food_ids.
    void update(entt::registry& reg, SpatialGrid& grid,
                int target_food, int target_viruses, WorldEvents& events);

    // Create a new player's starting cell(s); returns entt::entity of primary cell.
    entt::entity spawn_player(entt::registry& reg, SpatialGrid& grid,
                               PlayerInfo info, float starting_mass);

    // Kill all cells for a player (called on death or disconnect).
    void kill_player(entt::registry& reg, SpatialGrid& grid, uint32_t player_id);
}

// ── Battle Royale System ─────────────────────────────────────
namespace BRSystem {
    struct SafeZone { float cx, cy, radius, shrink_rate; int phase; float phase_timer; };

    void update(SafeZone& zone, entt::registry& reg, SpatialGrid& grid, float dt);
}
