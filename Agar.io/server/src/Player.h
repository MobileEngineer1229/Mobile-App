#pragma once
#include <cstdint>
#include <string>
#include <vector>
#include <chrono>
#include "Vec2.h"
#include "Config.h"

enum class Team { NONE = 0, RED = 1, GREEN = 2, BLUE = 3 };

// One split cell belonging to a player
struct Cell {
    uint32_t cell_id;
    Vec2     pos;
    float    mass;
    float    radius;
    Vec2     velocity;              // post-split impulse velocity
    float    recombine_timer = 0.f; // seconds until allowed to merge back
    bool     alive = true;
};

struct Player {
    uint32_t     id;
    std::string  name;
    uint32_t     color;
    std::string  skin_id;
    Team         team = Team::NONE;
    bool         alive = false;

    std::vector<Cell> cells;        // all split cells (max 16)

    // Input state (set by PacketHandler each tick from latest MoveInput)
    Vec2     mouse_target;          // world-space mouse position
    bool     wants_split  = false;
    bool     wants_eject  = false;
    uint32_t last_input_seq = 0;

    // Progression
    uint32_t level     = 1;
    uint32_t xp        = 0;
    uint32_t score     = 0;         // mass eaten this life
    uint32_t total_score = 0;       // all-time score (persisted)

    // Computed helpers
    float total_mass() const {
        float m = 0.f;
        for (auto& c : cells) if (c.alive) m += c.mass;
        return m;
    }

    Vec2 centroid() const {
        Vec2 c;
        int count = 0;
        for (auto& cell : cells) {
            if (cell.alive) { c += cell.pos; ++count; }
        }
        if (count > 0) { c.x /= count; c.y /= count; }
        return c;
    }

    static float massToRadius(float mass) {
        return std::sqrt(mass) * Config::MASS_RADIUS_SCALE;
    }

    static float radiusToMass(float radius) {
        float r = radius / Config::MASS_RADIUS_SCALE;
        return r * r;
    }

    static float speedForMass(float mass) {
        // Agar.io speed formula: speed = BASE * mass^DECAY
        return Config::SPEED_BASE * std::pow(mass, Config::SPEED_DECAY);
    }
};
