#pragma once
// ─────────────────────────────────────────────────────────────
//  ECS Components  (Data-Oriented Design per IO-Server.md)
//  Stored in contiguous flat arrays via EnTT registry.
//  No virtual functions, no pointers in hot path.
// ─────────────────────────────────────────────────────────────
#include <cstdint>
#include <string>
#include "Vec2.h"

// ── Spatial ──────────────────────────────────────────────────
struct Pos    { float x = 0.f, y = 0.f; };
struct Vel    { float vx = 0.f, vy = 0.f; };
struct Circle { float radius = 1.f; };
struct Mass   { float value  = 1.f; };

// ── Player cell ───────────────────────────────────────────────
// Each split cell is a separate EnTT entity with these tags.
struct CellComp {
    uint32_t player_id;    // which player owns this cell
    uint32_t cell_index;   // 0..15 (for recombine matching)
    float    recombine_timer = 0.f; // seconds until can merge
};

// ── Player meta (only on the "primary" entity per player) ─────
struct PlayerInfo {
    uint32_t    id;
    std::string name;
    uint32_t    color;
    std::string skin_id;
    uint8_t     team     = 0;   // 0=none 1=red 2=green 3=blue
    uint32_t    level    = 1;
    uint32_t    xp       = 0;
    uint32_t    score    = 0;   // this life
    bool        alive    = false;

    // Latest input from UDP channel (written by UdpServer, read by Systems)
    Vec2     mouse_target;
    bool     wants_split  = false;
    bool     wants_eject  = false;
    uint32_t last_seq     = 0;
};

// ── Tags ──────────────────────────────────────────────────────
struct FoodTag    {};   // agar pellet
struct VirusTag   {
    bool is_red      = false;
    int  feed_count  = 0;     // ejected pellets received
    Vec2 shoot_dir;
};
struct EjectedTag {
    uint32_t owner_id;
    float    speed = 0.f;
};
struct DecayingTag {};  // flagged when mass > DECAY_THRESHOLD
