#include "TcpServer.h"
#include "PacketSerializer.h"
#include "Config.h"
#include <iostream>
#include <deque>

// ─────────────────────────────────────────────────────────────
//  TcpSession — one per connected client
// ─────────────────────────────────────────────────────────────
class TcpSession : public std::enable_shared_from_this<TcpSession> {
public:
    TcpSession(tcp::socket socket, TcpServer& server)
        : ws_(std::move(socket)), server_(server) {}

    void start() {
        // Accept WebSocket upgrade
        ws_.async_accept([self = shared_from_this()](beast::error_code ec) {
            if (!ec) self->read_loop();
        });
    }

    // Thread-safe send: enqueue the buffer and flush when no write is in-flight.
    // Boost.Beast WebSocket does NOT allow concurrent async_writes on the same
    // stream — everything must be serialised through this queue.
    void send(const std::vector<uint8_t>& data) {
        auto buf = std::make_shared<std::vector<uint8_t>>(data);
        net::post(ws_.get_executor(),
            [self = shared_from_this(), buf]() mutable {
                self->write_queue_.push_back(std::move(buf));
                if (!self->writing_) self->do_write();
            });
    }

    uint32_t player_id() const { return player_id_; }

private:
    void read_loop() {
        ws_.async_read(buf_, [self = shared_from_this()](
                beast::error_code ec, std::size_t) {
            if (ec) {
                // Disconnect
                if (self->player_id_)
                    self->server_.game_loop_.on_player_disconnect(
                        self->player_id_);
                self->server_.on_session_closed(self->player_id_);
                return;
            }
            self->on_message();
            self->buf_.consume(self->buf_.size());
            self->read_loop();
        });
    }

    void on_message() {
        auto data_ptr = reinterpret_cast<const uint8_t*>(
            buf_.data().data());
        size_t len = buf_.size();
        if (len == 0) return;

        // ── 0xFE prefix: MoveInput sent over TCP by client ───────
        // (Client routes game inputs over WebSocket since WebRTC
        //  DataChannel is not used in the local dev setup.)
        if (data_ptr[0] == 0xFE && player_id_) {
            PacketSerializer::MoveInputMsg mv;
            if (PacketSerializer::decode_move_input(data_ptr + 1, len - 1, mv)) {
                InputPacket ip;
                ip.player_id = player_id_;
                ip.seq       = mv.seq;
                ip.mouse_x   = mv.mouse_x;
                ip.mouse_y   = mv.mouse_y;
                ip.split     = mv.split;
                ip.eject     = mv.eject;
                server_.game_loop_.push_input(ip);
            }
            return;
        }

        // ── Standard TcpPacket messages ───────────────────────────

        // JOIN
        PacketSerializer::JoinReq join;
        if (!player_id_ &&
            PacketSerializer::decode_join(data_ptr, len, join)) {
            uint32_t color = 0xFF3498DB; // default blue
            player_id_ = server_.game_loop_.on_player_join(
                join.name, join.skin, color, 0, join.mode);

            // Register session FIRST so send_to() works when snapshot fires
            {
                std::lock_guard<std::mutex> lk(server_.sessions_mtx_);
                server_.sessions_[player_id_] = shared_from_this();
            }

            // Send initial snapshot immediately — session is now registered
            server_.game_loop_.send_initial_snapshot(player_id_);

            auto welcome = PacketSerializer::build_welcome(
                player_id_, Config::WORLD_W, Config::WORLD_H,
                join.mode, 0, "", 1, 0, 1000);
            send(welcome);
            return;
        }

        // SDP OFFER — ignore (DataChannel not used; game data goes over TCP)
        PacketSerializer::SdpOfferMsg sdp;
        if (PacketSerializer::decode_sdp_offer(data_ptr, len, sdp)) {
            return; // no-op in TCP-only mode
        }

        // ICE CANDIDATE — ignore
        PacketSerializer::IceCandMsg ice;
        if (PacketSerializer::decode_ice(data_ptr, len, ice)) {
            return;
        }
    }

    // Serialised write queue — Boost.Beast forbids concurrent async_writes
    void do_write() {
        if (write_queue_.empty()) { writing_ = false; return; }
        writing_ = true;
        auto buf = write_queue_.front();  // shared_ptr copy keeps buf alive
        ws_.binary(true);
        ws_.async_write(
            net::buffer(*buf),
            [self = shared_from_this(), buf](beast::error_code, std::size_t) {
                self->write_queue_.pop_front();
                self->do_write();
            });
    }

    websocket::stream<tcp::socket>                  ws_;
    beast::flat_buffer                              buf_;
    TcpServer&                                      server_;
    uint32_t                                        player_id_ = 0;
    std::deque<std::shared_ptr<std::vector<uint8_t>>> write_queue_;
    bool                                            writing_ = false;
};

// ─────────────────────────────────────────────────────────────
//  TcpServer
// ─────────────────────────────────────────────────────────────
TcpServer::TcpServer(GameLoop& game_loop, uint16_t port)
    : game_loop_(game_loop), port_(port) {

    // Wire send callbacks into GameLoop.
    // Game state (snapshot/delta) is routed over TCP WebSocket with a
    // 0xFD prefix so the client can distinguish UdpPacket blobs from
    // TcpPacket blobs without changing the proto schema.
    auto udp_over_tcp_to = [this](uint32_t pid, const std::vector<uint8_t>& data) {
        std::vector<uint8_t> w;
        w.reserve(data.size() + 1);
        w.push_back(0xFD);
        w.insert(w.end(), data.begin(), data.end());
        send_to(pid, w);
    };

    game_loop_.set_callbacks({
        udp_over_tcp_to,   // send_udp_delta   → TCP with 0xFD prefix
        udp_over_tcp_to,   // send_udp_snapshot → TCP with 0xFD prefix
        [this](uint32_t pid, const std::vector<uint8_t>& data) {
            send_to(pid, data);
        },
        [this](const std::vector<uint8_t>& data) {
            broadcast(data);
        }
    });
}

TcpServer::~TcpServer() { stop(); }

void TcpServer::start() {
    tcp::endpoint ep{ tcp::v4(), port_ };
    acceptor_.open(ep.protocol());
    acceptor_.set_option(net::socket_base::reuse_address(true));
    acceptor_.bind(ep);
    acceptor_.listen();
    std::cout << "[TCP] Listening on port " << port_ << "\n";
    accept_loop();
    ioc_.run();
}

void TcpServer::stop() {
    ioc_.stop();
}

void TcpServer::accept_loop() {
    acceptor_.async_accept(ioc_,
        [this](beast::error_code ec, tcp::socket socket) {
            if (!ec) {
                auto session = std::make_shared<TcpSession>(
                    std::move(socket), *this);
                session->start();
            }
            accept_loop();
        });
}

void TcpServer::send_to(uint32_t player_id,
                         const std::vector<uint8_t>& data) {
    std::lock_guard<std::mutex> lk(sessions_mtx_);
    auto it = sessions_.find(player_id);
    if (it != sessions_.end())
        it->second->send(data);
}

void TcpServer::broadcast(const std::vector<uint8_t>& data) {
    std::lock_guard<std::mutex> lk(sessions_mtx_);
    for (auto& [id, session] : sessions_)
        session->send(data);
}

void TcpServer::on_session_closed(uint32_t player_id) {
    std::lock_guard<std::mutex> lk(sessions_mtx_);
    sessions_.erase(player_id);
}
