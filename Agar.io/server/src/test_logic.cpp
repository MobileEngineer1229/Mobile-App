// ─────────────────────────────────────────────────────────────
//  Agar.io Logic Test  —  no network, no MongoDB, no Redis
//
//  Tests:
//   1. Player spawns with correct starting mass/radius
//   2. Physics: cell moves toward mouse target each tick
//   3. Food spawn maintains target count
//   4. Collision: player eats food when overlapping
//   5. Mass decay above threshold
//   6. Split: player cell splits into 2
//
//  Build: add test_logic.cpp to a new Console project,
//         or compile standalone:
//    cl /std:c++17 /EHsc /I..\src /I..\..\server\vendor\entt\single_include
//       /I..\windows\vcpkg_installed\x64-windows\include
//       test_logic.cpp SpatialGrid.cpp Systems.cpp
// ─────────────────────────────────────────────────────────────
#define _SILENCE_ALL_CXX17_DEPRECATION_WARNINGS
#include <entt/entt.hpp>
#include <iostream>
#include <iomanip>
#include <chrono>
#include <cassert>
#include <cmath>

#include "Components.h"
#include "Config.h"
#include "SpatialGrid.h"
#include "Systems.h"
#include "Vec2.h"

// ── Helpers ───────────────────────────────────────────────────
static float mass_to_radius(float m) {
    return std::sqrt(m) * Config::MASS_RADIUS_SCALE;
}

static entt::entity make_player(entt::registry& reg, SpatialGrid& grid,
                                 uint32_t id, const std::string& name,
                                 float x, float y, float mass = Config::PLAYER_START_MASS) {
    PlayerInfo info;
    info.id    = id;
    info.name  = name;
    info.color = 0xFF0000FF;
    info.alive = true;
    info.mouse_target = { x, y };

    return SpawnSystem::spawn_player(reg, grid, info, mass);
}

static void print_player(entt::registry& reg, uint32_t pid) {
    float total = 0.f;
    int   cells = 0;
    for (auto [e, cell, pos, mass] : reg.view<CellComp, Pos, Mass>().each()) {
        if (cell.player_id != pid) continue;
        std::cout << "    cell[" << cell.cell_index << "] "
                  << "pos=(" << std::fixed << std::setprecision(1)
                  << pos.x << "," << pos.y << ")  "
                  << "mass=" << mass.value << "\n";
        total += mass.value;
        cells++;
    }
    std::cout << "    TOTAL  cells=" << cells << "  mass=" << total << "\n";
}

static int food_count(entt::registry& reg) {
    return static_cast<int>(reg.view<FoodTag>().size());
}

// ── Test runner ───────────────────────────────────────────────
int pass = 0, fail = 0;

#define CHECK(cond, msg) \
    if (cond) { std::cout << "  [PASS] " << msg << "\n"; ++pass; } \
    else       { std::cout << "  [FAIL] " << msg << "\n"; ++fail; }

// ── Test 1: Spawn ─────────────────────────────────────────────
void test_spawn() {
    std::cout << "\n=== Test 1: Spawn ===\n";
    entt::registry reg;
    SpatialGrid grid;

    auto e = make_player(reg, grid, 1, "Alice", 5000.f, 5000.f);
    CHECK(reg.valid(e), "spawn returns valid entity");

    bool found = false;
    for (auto [ent, cell, pos, mass, circ] :
         reg.view<CellComp, Pos, Mass, Circle>().each()) {
        if (cell.player_id != 1) continue;
        found = true;
        float expected_r = mass_to_radius(Config::PLAYER_START_MASS);
        CHECK(std::abs(circ.radius - expected_r) < 0.1f,
              "radius = sqrt(mass) * scale");
        CHECK(mass.value == Config::PLAYER_START_MASS, "start mass correct");
        CHECK(cell.recombine_timer > 0.f, "recombine timer set on spawn");
    }
    CHECK(found, "player has at least one cell");
    print_player(reg, 1);
}

// ── Test 2: Physics (movement) ────────────────────────────────
void test_physics() {
    std::cout << "\n=== Test 2: Physics (movement toward target) ===\n";
    entt::registry reg;
    SpatialGrid grid;

    make_player(reg, grid, 2, "Bob", 5000.f, 5000.f);

    // Force cell to a known position, then target far to the right
    for (auto [e, cell, pos] : reg.view<CellComp, Pos>().each()) {
        if (cell.player_id != 2) continue;
        grid.move(static_cast<EntityID>(e), pos.x, pos.y, 100.f, 5000.f);
        pos.x = 100.f; pos.y = 5000.f;
    }
    for (auto [e, info] : reg.view<PlayerInfo>().each()) {
        if (info.id != 2) continue;
        info.mouse_target = { Config::WORLD_W, 5000.f }; // always to the right
    }

    float x0 = 0.f;
    for (auto [e, cell, pos] : reg.view<CellComp, Pos>().each())
        if (cell.player_id == 2) x0 = pos.x;

    float dt = Config::TICK_MS / 1000.f;
    PhysicsSystem::update(reg, grid, dt);

    float x1 = 0.f;
    for (auto [e, cell, pos] : reg.view<CellComp, Pos>().each())
        if (cell.player_id == 2) x1 = pos.x;

    CHECK(x1 > x0, "cell moved right toward target");
    std::cout << "    x: " << x0 << " -> " << x1
              << "  (delta=" << (x1 - x0) << ")\n";
}

// ── Test 3: Food spawn ────────────────────────────────────────
void test_food_spawn() {
    std::cout << "\n=== Test 3: Food spawn ===\n";
    entt::registry reg;
    SpatialGrid grid;

    CHECK(food_count(reg) == 0, "starts empty");

    // SpawnSystem caps at 20/call — loop until target is reached
    for (int i = 0; i < 200; ++i)
        SpawnSystem::update(reg, grid, Config::FOOD_MAX, Config::VIRUS_MAX);

    int fc = food_count(reg);
    std::cout << "    food count after spawn: " << fc
              << " (target=" << Config::FOOD_MAX << ")\n";
    CHECK(fc == Config::FOOD_MAX, "food count reaches target");
}

// ── Test 4: Eat food ──────────────────────────────────────────
void test_eat_food() {
    std::cout << "\n=== Test 4: Collision — player eats food ===\n";
    entt::registry reg;
    SpatialGrid grid;

    // Place one food at known position
    auto food = reg.create();
    reg.emplace<FoodTag>(food);
    reg.emplace<Pos>(food, 5010.f, 5000.f);
    reg.emplace<Mass>(food, 1.f);
    reg.emplace<Circle>(food, mass_to_radius(1.f));
    grid.insert(static_cast<EntityID>(food), 5010.f, 5000.f);

    // Player at 5000,5000 with large mass to ensure it overlaps
    make_player(reg, grid, 3, "Carol", 5000.f, 5000.f, 500.f);

    // Move player cell exactly onto the food
    for (auto [e, cell, pos, circ] : reg.view<CellComp, Pos, Circle>().each()) {
        if (cell.player_id != 3) continue;
        float old_x = pos.x, old_y = pos.y;
        pos.x = 5010.f; pos.y = 5000.f;
        grid.move(static_cast<EntityID>(e), old_x, old_y, pos.x, pos.y);
    }

    float mass_before = 0.f;
    for (auto [e, cell, mass] : reg.view<CellComp, Mass>().each())
        if (cell.player_id == 3) mass_before += mass.value;

    auto events = CollisionSystem::resolve(reg, grid);

    float mass_after = 0.f;
    for (auto [e, cell, mass] : reg.view<CellComp, Mass>().each())
        if (cell.player_id == 3) mass_after += mass.value;

    bool food_eaten = !reg.valid(food) || !reg.all_of<FoodTag>(food);
    CHECK(food_eaten, "food entity removed after being eaten");
    CHECK(mass_after > mass_before, "player gained mass from eating food");
    std::cout << "    mass: " << mass_before << " -> " << mass_after << "\n";
}

// ── Test 5: Mass decay ────────────────────────────────────────
void test_decay() {
    std::cout << "\n=== Test 5: Mass decay (above threshold) ===\n";
    entt::registry reg;
    SpatialGrid grid;

    // Spawn with very large mass above decay threshold
    make_player(reg, grid, 4, "Dave", 5000.f, 5000.f,
                Config::DECAY_THRESHOLD * 5.f);

    float mass_before = 0.f;
    for (auto [e, cell, mass] : reg.view<CellComp, Mass>().each())
        if (cell.player_id == 4) mass_before += mass.value;

    // Run 60 ticks (= 3 seconds at 50ms/tick)
    float dt = Config::TICK_MS / 1000.f;
    for (int i = 0; i < 60; ++i)
        DecaySystem::update(reg, dt);

    float mass_after = 0.f;
    for (auto [e, cell, mass] : reg.view<CellComp, Mass>().each())
        if (cell.player_id == 4) mass_after += mass.value;

    CHECK(mass_after < mass_before, "mass decays over time");
    std::cout << "    mass after 3s: " << mass_before
              << " -> " << mass_after
              << "  (lost=" << (mass_before - mass_after) << ")\n";
}

// ── Test 6: Split ─────────────────────────────────────────────
void test_split() {
    std::cout << "\n=== Test 6: Split ===\n";
    entt::registry reg;
    SpatialGrid grid;

    // Use enough mass to split (must be > MIN_SPLIT_MASS * 2)
    make_player(reg, grid, 5, "Eve", 5000.f, 5000.f,
                Config::PLAYER_MIN_SPLIT_MASS * 4.f);

    int cells_before = 0;
    for (auto [e, cell] : reg.view<CellComp>().each())
        if (cell.player_id == 5) cells_before++;

    // Signal split
    for (auto [e, info] : reg.view<PlayerInfo>().each()) {
        if (info.id != 5) continue;
        info.wants_split = true;
        info.mouse_target = { 6000.f, 5000.f }; // split toward right
    }

    // Apply input + physics (split happens during input apply)
    std::vector<InputPacket> queue;
    std::mutex mtx;
    InputSystem::apply(reg, queue, mtx, grid);

    float dt = Config::TICK_MS / 1000.f;
    PhysicsSystem::update(reg, grid, dt);

    int cells_after = 0;
    for (auto [e, cell] : reg.view<CellComp>().each())
        if (cell.player_id == 5) cells_after++;

    CHECK(cells_after > cells_before,
          "split increases cell count from " + std::to_string(cells_before) +
          " to " + std::to_string(cells_after));
    print_player(reg, 5);
}

// ── Main ──────────────────────────────────────────────────────
int main() {
    std::cout << "═══════════════════════════════════════\n";
    std::cout << "  Agar.io Server — Logic Unit Tests\n";
    std::cout << "═══════════════════════════════════════\n";

    test_spawn();
    test_physics();
    test_food_spawn();
    test_eat_food();
    test_decay();
    test_split();

    std::cout << "\n═══════════════════════════════════════\n";
    std::cout << "  Results: " << pass << " passed, " << fail << " failed\n";
    std::cout << "═══════════════════════════════════════\n";

    return fail == 0 ? 0 : 1;
}
