#pragma once
// ─────────────────────────────────────────────────────────────
//  Redis Client  (per IO-Server.md "Hot Connector" patterns)
//
//  Uses hiredis (C library wrapper).
//
//  Patterns used:
//  1. Tick-Sync Leaderboard  — ZADD every 5s from each instance
//  2. Cross-server chat      — Pub/Sub on "chat:global" channel
//  3. Player presence        — SET player:{id} "srv_01" EX 10
// ─────────────────────────────────────────────────────────────
#include <string>
#include <vector>
#include <functional>
#include <thread>
#include <atomic>
#include <cstdint>

struct LeaderboardEntry {
    std::string name;
    double      score = 0.0;
};

class RedisClient {
public:
    RedisClient(const std::string& host = "127.0.0.1", int port = 6379);
    ~RedisClient();

    bool connect();
    void disconnect();

    // ── Leaderboard (ZADD / ZREVRANGE) ───────────────────────
    void zadd_score(const std::string& key,
                    const std::string& member, double score);
    std::vector<LeaderboardEntry> zrevrange(const std::string& key,
                                             int start, int stop);

    // ── Presence heartbeat ────────────────────────────────────
    // SET player:{id} "server_id" EX 10
    void set_presence(uint32_t player_id, const std::string& server_id,
                      int ttl_seconds = 10);
    void del_presence(uint32_t player_id);

    // ── Pub/Sub (cross-server chat) ───────────────────────────
    void publish(const std::string& channel, const std::string& message);

    // Subscribe runs on a background thread; cb called for each message
    void subscribe(const std::string& channel,
                   std::function<void(std::string msg)> cb);
    void unsubscribe();

    // ── Generic SET/GET ───────────────────────────────────────
    void  set(const std::string& key, const std::string& val,
              int ex_seconds = 0);
    std::string get(const std::string& key);

private:
    std::string host_;
    int         port_;
    void*       ctx_   = nullptr;  // redisContext* (opaque)
    void*       sub_ctx_ = nullptr;

    std::thread                     sub_thread_;
    std::atomic<bool>               sub_running_{ false };
    std::function<void(std::string)> sub_callback_;
    std::string                     sub_channel_;
};
