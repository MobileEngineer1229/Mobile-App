#pragma once
#include <cstdint>

namespace Config {

    // ── World ──────────────────────────────────────────────────
    constexpr float    WORLD_W              = 14142.0f;  // ~sqrt(2)*10000 (Agar.io scale)
    constexpr float    WORLD_H              = 14142.0f;

    // ── Food (Agar pellets) ────────────────────────────────────
    constexpr int      FOOD_MAX             = 2000;
    constexpr float    FOOD_RADIUS          = 6.0f;
    constexpr float    FOOD_MASS            = 1.0f;

    // ── Ejected mass (W key) ───────────────────────────────────
    constexpr float    EJECT_MASS           = 12.0f;     // mass released per eject
    constexpr float    EJECT_LOSS           = 18.0f;     // mass player loses per eject
    constexpr float    EJECT_SPEED          = 780.0f;    // initial speed of ejected pellet
    constexpr float    EJECT_DECAY          = 0.98f;     // speed multiplier per tick

    // ── Viruses ────────────────────────────────────────────────
    constexpr int      VIRUS_MAX            = 100;
    constexpr float    VIRUS_RADIUS         = 100.0f;
    constexpr float    VIRUS_MASS           = 100.0f;
    // A cell must be > VIRUS_RADIUS to be split by virus
    constexpr float    VIRUS_SPLIT_THRESHOLD = 133.0f;
    // Feed virus 7 ejected pellets → it splits and shoots a new virus
    constexpr int      VIRUS_FEED_COUNT     = 7;

    // ── Player ─────────────────────────────────────────────────
    constexpr float    PLAYER_START_MASS    = 10.0f;
    constexpr int      PLAYER_MAX_SPLITS    = 16;         // max cells after splitting
    constexpr float    PLAYER_EAT_RATIO     = 1.15f;      // must be 15% larger to eat
    constexpr float    PLAYER_MIN_SPLIT_MASS = 35.0f;     // min mass to be allowed to split
    // recombine cooldown (seconds) = base + mass * factor
    constexpr float    RECOMBINE_BASE       = 20.0f;
    constexpr float    RECOMBINE_MASS_FACTOR = 0.02f;

    // Speed formula: speed (world-units/sec) = SPEED_BASE * mass^SPEED_DECAY
    // World is 14142 x 14142.  With zoom≈2 the viewport shows ~960 units.
    // SPEED_BASE=2000 → mass-10 cell moves ≈36 units/tick (726/s) → ~1.3s across screen.
    constexpr float    SPEED_BASE           = 2000.0f;
    constexpr float    SPEED_DECAY          = -0.439f;    // exponent (negative → larger = slower)

    // Passive decay: cells above DECAY_THRESHOLD lose mass per second
    constexpr float    DECAY_THRESHOLD      = 400.0f;
    constexpr float    DECAY_RATE           = 0.002f;     // lose 0.2% per second above threshold

    // mass → radius conversion (same as Agar.io)
    // radius = sqrt(mass) * MASS_RADIUS_SCALE
    constexpr float    MASS_RADIUS_SCALE    = 10.0f;

    // ── Battle Royale ──────────────────────────────────────────
    constexpr float    BR_START_RADIUS      = 7000.0f;
    constexpr float    BR_MIN_RADIUS        = 500.0f;
    constexpr float    BR_SHRINK_RATE       = 20.0f;      // units/second per phase
    constexpr int      BR_PHASE_DURATION    = 30;         // seconds per shrink phase
    constexpr float    BR_DAMAGE_PER_SEC    = 2.0f;       // mass lost per second outside zone

    // ── Server networking ──────────────────────────────────────
    constexpr uint16_t TCP_PORT             = 9001;       // WebSocket (reliable)
    constexpr int      TICK_RATE           = 20;          // ticks/second
    constexpr int      TICK_MS             = 1000 / TICK_RATE;
    constexpr int      LEADERBOARD_TICKS   = 20;          // send leaderboard every N ticks

    // ── Progression ────────────────────────────────────────────
    constexpr uint32_t XP_PER_FOOD         = 1;
    constexpr uint32_t XP_PER_PLAYER_EAT   = 50;
    constexpr uint32_t XP_BASE_TO_LEVEL    = 1000;        // XP needed for level 1→2
    constexpr float    XP_LEVEL_SCALE      = 1.5f;        // multiplier per level

    // ── MongoDB ────────────────────────────────────────────────
    constexpr auto     MONGO_URI           = "mongodb://localhost:27017";
    constexpr auto     MONGO_DB            = "agario";
    constexpr auto     COL_SCORES          = "scores";
    constexpr auto     COL_PLAYERS         = "players";
    constexpr auto     COL_QUESTS          = "quests";
}
