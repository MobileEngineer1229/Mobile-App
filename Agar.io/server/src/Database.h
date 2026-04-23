#pragma once
// ─────────────────────────────────────────────────────────────
//  MongoDB async batch writer  (per IO-Server.md)
//
//  Rule: NEVER call MongoDB inside the main Update() loop.
//  All writes go into a std::queue<DBTask>.
//  A dedicated worker thread drains the queue and executes
//  MongoDB operations without touching the game loop.
// ─────────────────────────────────────────────────────────────
#include <string>
#include <queue>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <functional>
#include <atomic>
#include <cstdint>

struct PlayerRecord {
    std::string player_name;
    uint32_t    level        = 1;
    uint32_t    xp           = 0;
    uint32_t    kills        = 0;
    uint32_t    deaths       = 0;
    uint32_t    high_score   = 0;
    std::string skin_id;
};

struct ScoreRecord {
    std::string player_name;
    uint32_t    score;
    std::string mode;
    long long   timestamp;
};

class Database {
public:
    Database();
    ~Database();

    void start();
    void stop();

    // ── Push tasks (non-blocking, safe from game loop) ────────
    void save_score(const ScoreRecord& rec);
    void save_player(const PlayerRecord& rec);
    void update_kills(const std::string& name, uint32_t kills, uint32_t deaths);

    // ── Synchronous reads (called only at login, not in loop) ─
    PlayerRecord load_player(const std::string& name);
    std::vector<ScoreRecord> top_scores(int limit = 10);

private:
    struct DBTask {
        std::function<void()> fn;
    };

    void worker_loop();
    void push_task(std::function<void()> fn);

    std::queue<DBTask>          queue_;
    std::mutex                  mtx_;
    std::condition_variable     cv_;
    std::thread                 worker_;
    std::atomic<bool>           running_{ false };

    // mongocxx handles held by worker thread only
    // (mongocxx client is not thread-safe; one per thread)
    void* mongo_client_   = nullptr; // opaque — cast in .cpp
    void* mongo_db_       = nullptr;
};
