#pragma once
#include <unordered_map>
#include <vector>
#include <mutex>
#include <random>
#include <functional>
#include "Player.h"
#include "Food.h"
#include "Config.h"

enum class GameMode { FFA=0, BATTLE_ROYALE=1, TEAMS=2, EXPERIMENTAL=3, PARTY=4, RUSH=5 };

// Callbacks the World fires to inform the server layer
struct WorldCallbacks {
    std::function<void(uint32_t killer_id, uint32_t dead_id)> on_player_eaten;
    std::function<void(uint32_t player_id, uint32_t xp)>      on_xp_gained;
};

class World {
public:
    explicit World(GameMode mode = GameMode::FFA);

    // ── Tick ──────────────────────────────────────────────────
    void tick(float dt);   // called every TICK_MS

    // ── Player management ─────────────────────────────────────
    uint32_t add_player(const std::string& name, const std::string& skin,
                        uint32_t color, Team team, float starting_mass);
    void     remove_player(uint32_t id);
    void     apply_input(uint32_t player_id, float mx, float my,
                         bool split, bool eject, uint32_t seq);

    // ── Read state (for snapshot / delta) ─────────────────────
    std::vector<Player>  get_players()  const;
    std::vector<Food>    get_foods()    const;
    std::vector<Virus>   get_viruses()  const;
    uint32_t             get_tick()     const { return tick_; }

    // Changed-since-last-tick sets (cleared at end of tick)
    std::vector<uint32_t> changed_player_ids;
    std::vector<uint32_t> removed_player_ids;
    std::vector<uint32_t> new_food_ids;
    std::vector<uint32_t> eaten_food_ids;
    std::vector<uint32_t> changed_virus_ids;
    std::vector<uint32_t> removed_virus_ids;
    std::vector<uint32_t> new_ejected_ids;

    // Battle Royale safe zone
    struct SafeZone {
        Vec2  center;
        float radius     = Config::BR_START_RADIUS;
        int   phase      = 0;
        float phase_timer = 0.f;
    } safe_zone;

    WorldCallbacks callbacks;

    mutable std::mutex mtx;  // protects all state

private:
    // ── Internal update steps ─────────────────────────────────
    void update_player_movement(Player& p, float dt);
    void update_player_decay(Player& p, float dt);
    void update_recombine_timers(Player& p, float dt);
    void update_ejected_mass(float dt);
    void check_player_eat_food(Player& p);
    void check_player_eat_virus(Player& p);
    void check_player_eat_player(Player& p);
    void process_split(Player& p);
    void process_eject(Player& p);
    void respawn_food(int count);
    void respawn_virus();
    void update_battle_royale(float dt);
    void apply_safe_zone_damage(float dt);

    Vec2     random_pos();
    uint32_t new_id() { return ++id_counter_; }

    // ── State ─────────────────────────────────────────────────
    GameMode mode_;
    uint32_t tick_      = 0;
    uint32_t id_counter_ = 0;

    std::unordered_map<uint32_t, Player> players_;
    std::unordered_map<uint32_t, Food>   foods_;
    std::unordered_map<uint32_t, Food>   ejected_; // ejected mass pellets
    std::unordered_map<uint32_t, Virus>  viruses_;

    std::mt19937                          rng_;
    std::uniform_real_distribution<float> dist_x_;
    std::uniform_real_distribution<float> dist_y_;
    std::uniform_int_distribution<int>    dist_color_;
};
