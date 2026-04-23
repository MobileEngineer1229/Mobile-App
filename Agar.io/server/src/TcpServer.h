#pragma once
// ─────────────────────────────────────────────────────────────
//  TCP WebSocket Server  (Boost.Beast)
//  Handles: Join, SDP signaling, Death, Leaderboard, Chat
//  All messages serialized with Protobuf.
// ─────────────────────────────────────────────────────────────
#include <boost/asio.hpp>
#include <boost/beast.hpp>
#include <boost/beast/websocket.hpp>
#include <unordered_map>
#include <memory>
#include <thread>
#include <mutex>
#include <functional>
#include <cstdint>
#include <vector>
#include "GameLoop.h"

namespace beast     = boost::beast;
namespace http      = beast::http;
namespace websocket = beast::websocket;
namespace net       = boost::asio;
using tcp           = net::ip::tcp;

class TcpSession;

class TcpServer {
public:
    TcpServer(GameLoop& game_loop, uint16_t port = 9001);
    ~TcpServer();

    void start();   // blocks; call on its own thread or with run_in_background
    void stop();

    // Called by GameLoop to send to specific client
    void send_to(uint32_t player_id, const std::vector<uint8_t>& data);
    void broadcast(const std::vector<uint8_t>& data);

private:
    void accept_loop();
    void on_session_closed(uint32_t player_id);

    GameLoop&          game_loop_;
    uint16_t           port_;
    net::io_context    ioc_;
    tcp::acceptor      acceptor_{ ioc_ };

    std::unordered_map<uint32_t, std::shared_ptr<TcpSession>> sessions_;
    std::mutex sessions_mtx_;

    friend class TcpSession;
};
