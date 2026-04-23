#include <iostream>
#include <thread>
#include <csignal>
#include "Config.h"
#include "Database.h"
#include "RedisClient.h"
#include "GameLoop.h"
#include "TcpServer.h"
#include "UdpServer.h"

// ─────────────────────────────────────────────────────────────
//  Main — follows IO-Server.md game loop structure
//
//  Thread layout:
//    main thread    → TCP WebSocket accept loop (Boost.Asio ioc.run())
//    game_thread    → Fixed-rate ECS game loop (GameLoop::loop)
//    db_thread      → MongoDB async worker (Database::worker_loop)
//    redis_sub_thread → Redis Pub/Sub listener (RedisClient::subscribe)
// ─────────────────────────────────────────────────────────────

static std::atomic<bool> g_running{ true };

void signal_handler(int) {
    std::cout << "\n[main] Shutting down...\n";
    g_running = false;
}

int main() {
    std::signal(SIGINT,  signal_handler);
    std::signal(SIGTERM, signal_handler);

    std::cout << "=== Agar.io C++ Server ===\n";
    std::cout << "World: " << Config::WORLD_W << " x " << Config::WORLD_H << "\n";
    std::cout << "Tick rate: " << Config::TICK_RATE << " Hz\n";
    std::cout << "TCP port: " << Config::TCP_PORT << "\n";

    // ── 1. Database (MongoDB async writer) ────────────────────
    Database db;
    db.start();
    std::cout << "[DB] MongoDB worker started\n";

    // ── 2. Redis ──────────────────────────────────────────────
    RedisClient redis;
    if (!redis.connect()) {
        std::cerr << "[Redis] WARNING: Could not connect to Redis\n";
    } else {
        std::cout << "[Redis] Connected\n";
        // Subscribe to global chat from other server instances
        redis.subscribe("chat:global", [](std::string msg) {
            std::cout << "[Redis] Global chat: " << msg << "\n";
            // TODO: broadcast to local clients via TcpServer
        });
    }

    // ── 3. Game loop ──────────────────────────────────────────
    GameLoop game_loop(db, redis);

    // ── 4. UDP server (WebRTC DataChannels) ───────────────────
    UdpServer udp_server(game_loop,
        // ICE forward callback — sends ICE candidate back over TCP
        [](uint32_t, const std::string&, const std::string&, int) {
            // Wired up inside TcpServer::on_message after session ref is available
        });

    // ── 5. TCP server (WebSocket) ─────────────────────────────
    TcpServer tcp_server(game_loop, Config::TCP_PORT);

    // NOTE: NetworkCallbacks (snapshot/delta over TCP with 0xFD prefix,
    //       send_tcp, broadcast_tcp) are already set inside TcpServer ctor.
    //       No second set_callbacks call here — that would overwrite them
    //       with udp_server routes that the client never reads.

    // ── 6. Start game loop on its own thread ──────────────────
    game_loop.start();
    std::cout << "[Game] Loop started at " << Config::TICK_RATE << " Hz\n";

    // ── 7. Start TCP on main thread (blocks) ──────────────────
    std::thread tcp_thread([&tcp_server]() {
        tcp_server.start();
    });

    // ── Wait for shutdown signal ──────────────────────────────
    while (g_running) {
        std::this_thread::sleep_for(std::chrono::milliseconds(200));
    }

    std::cout << "[main] Stopping all systems...\n";
    tcp_server.stop();
    game_loop.stop();
    redis.unsubscribe();
    db.stop();

    if (tcp_thread.joinable()) tcp_thread.join();

    std::cout << "[main] Clean shutdown complete.\n";
    return 0;
}
