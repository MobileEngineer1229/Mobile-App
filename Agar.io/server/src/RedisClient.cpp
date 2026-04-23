#include "RedisClient.h"
#include <hiredis/hiredis.h>
#include <stdexcept>
#include <iostream>
#include <sstream>

RedisClient::RedisClient(const std::string& host, int port)
    : host_(host), port_(port) {}

RedisClient::~RedisClient() {
    disconnect();
}

bool RedisClient::connect() {
    ctx_ = redisConnect(host_.c_str(), port_);
    if (!ctx_ || reinterpret_cast<redisContext*>(ctx_)->err) {
        std::cerr << "[Redis] Connect failed\n";
        return false;
    }
    return true;
}

void RedisClient::disconnect() {
    unsubscribe();
    if (ctx_) {
        redisFree(reinterpret_cast<redisContext*>(ctx_));
        ctx_ = nullptr;
    }
}

// ── Leaderboard ───────────────────────────────────────────────
void RedisClient::zadd_score(const std::string& key,
                              const std::string& member, double score) {
    auto* c = reinterpret_cast<redisContext*>(ctx_);
    auto* reply = reinterpret_cast<redisReply*>(
        redisCommand(c, "ZADD %s %f %s", key.c_str(), score, member.c_str()));
    if (reply) freeReplyObject(reply);
}

std::vector<LeaderboardEntry> RedisClient::zrevrange(
        const std::string& key, int start, int stop) {
    auto* c = reinterpret_cast<redisContext*>(ctx_);
    auto* reply = reinterpret_cast<redisReply*>(
        redisCommand(c, "ZREVRANGE %s %d %d WITHSCORES",
                     key.c_str(), start, stop));

    std::vector<LeaderboardEntry> result;
    if (!reply || reply->type != REDIS_REPLY_ARRAY) {
        if (reply) freeReplyObject(reply);
        return result;
    }
    for (size_t i = 0; i + 1 < reply->elements; i += 2) {
        LeaderboardEntry e;
        e.name  = reply->element[i]->str;
        e.score = std::stod(reply->element[i + 1]->str);
        result.push_back(e);
    }
    freeReplyObject(reply);
    return result;
}

// ── Presence ──────────────────────────────────────────────────
void RedisClient::set_presence(uint32_t player_id,
                                const std::string& server_id,
                                int ttl) {
    std::string key = "player:" + std::to_string(player_id);
    auto* c = reinterpret_cast<redisContext*>(ctx_);
    auto* reply = reinterpret_cast<redisReply*>(
        redisCommand(c, "SET %s %s EX %d",
                     key.c_str(), server_id.c_str(), ttl));
    if (reply) freeReplyObject(reply);
}

void RedisClient::del_presence(uint32_t player_id) {
    std::string key = "player:" + std::to_string(player_id);
    auto* c = reinterpret_cast<redisContext*>(ctx_);
    auto* reply = reinterpret_cast<redisReply*>(
        redisCommand(c, "DEL %s", key.c_str()));
    if (reply) freeReplyObject(reply);
}

// ── Pub/Sub ───────────────────────────────────────────────────
void RedisClient::publish(const std::string& channel,
                           const std::string& message) {
    auto* c = reinterpret_cast<redisContext*>(ctx_);
    auto* reply = reinterpret_cast<redisReply*>(
        redisCommand(c, "PUBLISH %s %s", channel.c_str(), message.c_str()));
    if (reply) freeReplyObject(reply);
}

void RedisClient::subscribe(const std::string& channel,
                             std::function<void(std::string)> cb) {
    sub_channel_  = channel;
    sub_callback_ = std::move(cb);
    sub_running_  = true;

    sub_thread_ = std::thread([this]() {
        // Each subscribe needs its own connection
        auto* sc = redisConnect(host_.c_str(), port_);
        if (!sc || sc->err) return;
        sub_ctx_ = sc;

        auto* reply = reinterpret_cast<redisReply*>(
            redisCommand(sc, "SUBSCRIBE %s", sub_channel_.c_str()));
        if (reply) freeReplyObject(reply);

        while (sub_running_) {
            redisReply* msg = nullptr;
            if (redisGetReply(sc, reinterpret_cast<void**>(&msg)) == REDIS_OK
                && msg) {
                if (msg->type == REDIS_REPLY_ARRAY && msg->elements == 3
                    && std::string(msg->element[0]->str) == "message") {
                    sub_callback_(msg->element[2]->str);
                }
                freeReplyObject(msg);
            }
        }
        redisFree(sc);
        sub_ctx_ = nullptr;
    });
}

void RedisClient::unsubscribe() {
    sub_running_ = false;
    if (sub_thread_.joinable()) sub_thread_.join();
}

// ── Generic SET/GET ───────────────────────────────────────────
void RedisClient::set(const std::string& key, const std::string& val,
                       int ex_seconds) {
    auto* c = reinterpret_cast<redisContext*>(ctx_);
    redisReply* r;
    if (ex_seconds > 0)
        r = reinterpret_cast<redisReply*>(
            redisCommand(c, "SET %s %s EX %d",
                         key.c_str(), val.c_str(), ex_seconds));
    else
        r = reinterpret_cast<redisReply*>(
            redisCommand(c, "SET %s %s", key.c_str(), val.c_str()));
    if (r) freeReplyObject(r);
}

std::string RedisClient::get(const std::string& key) {
    auto* c = reinterpret_cast<redisContext*>(ctx_);
    auto* r = reinterpret_cast<redisReply*>(
        redisCommand(c, "GET %s", key.c_str()));
    std::string val;
    if (r && r->type == REDIS_REPLY_STRING) val = r->str;
    if (r) freeReplyObject(r);
    return val;
}
