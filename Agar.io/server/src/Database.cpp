#include "Database.h"
#include "Config.h"
#include <mongocxx/client.hpp>
#include <mongocxx/instance.hpp>
#include <mongocxx/uri.hpp>
#include <bsoncxx/builder/stream/document.hpp>
#include <bsoncxx/json.hpp>
#include <chrono>
#include <iostream>

using bsoncxx::builder::stream::document;
using bsoncxx::builder::stream::finalize;

// mongocxx instance must be created exactly once per process
static mongocxx::instance _mongo_instance{};

Database::Database()  = default;
Database::~Database() { stop(); }

void Database::start() {
    running_ = true;
    worker_  = std::thread(&Database::worker_loop, this);
}

void Database::stop() {
    running_ = false;
    cv_.notify_all();
    if (worker_.joinable()) worker_.join();
}

void Database::push_task(std::function<void()> fn) {
    {
        std::lock_guard<std::mutex> lk(mtx_);
        queue_.push({ std::move(fn) });
    }
    cv_.notify_one();
}

// ── Worker thread ─────────────────────────────────────────────
void Database::worker_loop() {
    // Each thread gets its own mongocxx::client (not thread-safe)
    mongocxx::client client{ mongocxx::uri{ Config::MONGO_URI } };
    auto db = client[Config::MONGO_DB];

    while (running_ || !queue_.empty()) {
        std::unique_lock<std::mutex> lk(mtx_);
        cv_.wait(lk, [this] { return !queue_.empty() || !running_; });

        while (!queue_.empty()) {
            auto task = std::move(queue_.front());
            queue_.pop();
            lk.unlock();
            try {
                // Bind captured mongo handles via closure
                // (closures capture client/db by ref from this scope)
                task.fn();
            } catch (const std::exception& e) {
                std::cerr << "[DB] Error: " << e.what() << "\n";
            }
            lk.lock();
        }
    }
}

// ── Public push methods ───────────────────────────────────────
void Database::save_score(const ScoreRecord& rec) {
    push_task([rec]() {
        mongocxx::client client{ mongocxx::uri{ Config::MONGO_URI } };
        auto col = client[Config::MONGO_DB][Config::COL_SCORES];
        auto doc = document{}
            << "name"      << rec.player_name
            << "score"     << static_cast<int32_t>(rec.score)
            << "mode"      << rec.mode
            << "timestamp" << rec.timestamp
            << finalize;
        col.insert_one(doc.view());
    });
}

void Database::save_player(const PlayerRecord& rec) {
    push_task([rec]() {
        mongocxx::client client{ mongocxx::uri{ Config::MONGO_URI } };
        auto col = client[Config::MONGO_DB][Config::COL_PLAYERS];

        auto filter = document{} << "name" << rec.player_name << finalize;
        auto update = document{}
            << "$set" << bsoncxx::builder::stream::open_document
                << "level"      << static_cast<int32_t>(rec.level)
                << "xp"         << static_cast<int32_t>(rec.xp)
                << "high_score" << static_cast<int32_t>(rec.high_score)
                << "skin_id"    << rec.skin_id
            << bsoncxx::builder::stream::close_document
            << "$inc" << bsoncxx::builder::stream::open_document
                << "kills"  << static_cast<int32_t>(rec.kills)
                << "deaths" << static_cast<int32_t>(rec.deaths)
            << bsoncxx::builder::stream::close_document
            << finalize;

        mongocxx::options::update opts;
        opts.upsert(true);
        col.update_one(filter.view(), update.view(), opts);
    });
}

void Database::update_kills(const std::string& name,
                             uint32_t kills, uint32_t deaths) {
    push_task([name, kills, deaths]() {
        mongocxx::client client{ mongocxx::uri{ Config::MONGO_URI } };
        auto col = client[Config::MONGO_DB][Config::COL_PLAYERS];
        auto filter = document{} << "name" << name << finalize;
        auto update = document{}
            << "$inc" << bsoncxx::builder::stream::open_document
                << "stats.kills"  << static_cast<int32_t>(kills)
                << "stats.deaths" << static_cast<int32_t>(deaths)
            << bsoncxx::builder::stream::close_document
            << finalize;
        col.update_one(filter.view(), update.view());
    });
}

// ── Synchronous reads (login only, never in game loop) ────────
PlayerRecord Database::load_player(const std::string& name) {
    mongocxx::client client{ mongocxx::uri{ Config::MONGO_URI } };
    auto col = client[Config::MONGO_DB][Config::COL_PLAYERS];
    auto filter = document{} << "name" << name << finalize;
    auto result = col.find_one(filter.view());

    PlayerRecord rec;
    rec.player_name = name;
    if (!result) return rec;

    auto view = result->view();
    if (view["level"])      rec.level      = view["level"].get_int32().value;
    if (view["xp"])         rec.xp         = view["xp"].get_int32().value;
    if (view["high_score"]) rec.high_score = view["high_score"].get_int32().value;
    if (view["skin_id"])    rec.skin_id    = std::string(view["skin_id"].get_string().value);
    return rec;
}

std::vector<ScoreRecord> Database::top_scores(int limit) {
    mongocxx::client client{ mongocxx::uri{ Config::MONGO_URI } };
    auto col = client[Config::MONGO_DB][Config::COL_SCORES];

    mongocxx::options::find opts;
    opts.limit(limit);
    opts.sort(document{} << "score" << -1 << finalize);

    std::vector<ScoreRecord> results;
    auto cursor = col.find({}, opts);
    for (auto& doc : cursor) {
        ScoreRecord r;
        if (doc["name"])  r.player_name = std::string(doc["name"].get_string().value);
        if (doc["score"]) r.score       = doc["score"].get_int32().value;
        if (doc["mode"])  r.mode        = std::string(doc["mode"].get_string().value);
        results.push_back(r);
    }
    return results;
}
