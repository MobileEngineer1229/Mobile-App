#pragma once
#include <cstdint>
#include "Vec2.h"
#include "Config.h"

struct Food {
    uint32_t id;
    Vec2     pos;
    float    mass   = Config::FOOD_MASS;
    float    radius = Config::FOOD_RADIUS;
    uint32_t color  = 0xFF00CC44;   // packed ARGB
    bool     alive  = true;
    bool     is_ejected = false;    // ejected mass moves differently
    Vec2     velocity;              // only used for ejected mass
};

struct Virus {
    uint32_t id;
    Vec2     pos;
    float    radius  = Config::VIRUS_RADIUS;
    float    mass    = Config::VIRUS_MASS;
    uint32_t color   = 0xFF33FF33;
    bool     is_red  = false;
    bool     alive   = true;
    int      feed_count = 0;        // how many ejected pellets hit this virus
    Vec2     shoot_dir;             // direction to shoot new virus when splitting
};
