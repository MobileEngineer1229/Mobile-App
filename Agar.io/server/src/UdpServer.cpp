#include "UdpServer.h"
#include "PacketSerializer.h"
#include <iostream>

UdpServer::UdpServer(GameLoop& game_loop, IceSendCb ice_send)
    : game_loop_(game_loop), ice_send_cb_(std::move(ice_send)) {

    // Wire UDP send callbacks into GameLoop
    NetworkCallbacks cbs = {};
    cbs.send_udp_delta    = [this](uint32_t pid, const std::vector<uint8_t>& d) {
        send_delta(pid, d);
    };
    cbs.send_udp_snapshot = [this](uint32_t pid, const std::vector<uint8_t>& d) {
        send_snapshot(pid, d);
    };
    // TCP callbacks will be set by TcpServer; merge here
    game_loop_.set_callbacks(cbs);
}

UdpServer::~UdpServer() {
    std::lock_guard<std::mutex> lk(peers_mtx_);
    peers_.clear();
}

// ─────────────────────────────────────────────────────────────
std::string UdpServer::handle_sdp_offer(uint32_t player_id,
                                          const std::string& offer_sdp) {
    std::lock_guard<std::mutex> lk(peers_mtx_);

    rtc::Configuration config;
    // Use Google STUN for ICE
    config.iceServers.emplace_back("stun:stun.l.google.com:19302");

    auto pc = std::make_shared<rtc::PeerConnection>(config);
    PeerState state;
    state.player_id = player_id;
    state.pc        = pc;

    // ICE candidates → forward over TCP to client
    pc->onLocalCandidate([this, player_id](rtc::Candidate cand) {
        ice_send_cb_(player_id,
                     cand.candidate(),
                     cand.mid(),
                     0);
    });

    // When DataChannel is created by client (browser creates it)
    pc->onDataChannel([this, player_id, &state](
                          std::shared_ptr<rtc::DataChannel> dc) {
        dc->onOpen([player_id, &state]() {
            std::cout << "[UDP] DataChannel open for player "
                      << player_id << "\n";
            state.dc_open = true;
        });

        dc->onMessage([this, player_id](std::variant<rtc::binary, rtc::string> msg) {
            if (std::holds_alternative<rtc::binary>(msg)) {
                auto& bin = std::get<rtc::binary>(msg);
                on_data_channel_message(
                    player_id,
                    reinterpret_cast<const uint8_t*>(bin.data()),
                    bin.size());
            }
        });

        state.dc = dc;
    });

    // Set remote SDP offer and get local answer
    pc->setRemoteDescription(rtc::Description(offer_sdp, "offer"));
    std::string answer;
    pc->onLocalDescription([&answer](rtc::Description desc) {
        answer = std::string(desc);
    });
    pc->setLocalDescription();

    peers_[player_id] = std::move(state);
    return answer;
}

void UdpServer::handle_ice_candidate(uint32_t player_id,
                                      const std::string& candidate,
                                      const std::string& sdp_mid,
                                      int sdp_m_line) {
    std::lock_guard<std::mutex> lk(peers_mtx_);
    auto it = peers_.find(player_id);
    if (it == peers_.end()) return;
    it->second.pc->addRemoteCandidate(
        rtc::Candidate(candidate, sdp_mid));
}

void UdpServer::remove_peer(uint32_t player_id) {
    std::lock_guard<std::mutex> lk(peers_mtx_);
    peers_.erase(player_id);
}

// ─────────────────────────────────────────────────────────────
void UdpServer::send_delta(uint32_t player_id,
                            const std::vector<uint8_t>& data) {
    std::lock_guard<std::mutex> lk(peers_mtx_);
    auto it = peers_.find(player_id);
    if (it == peers_.end() || !it->second.dc_open) return;
    it->second.dc->send(
        reinterpret_cast<const std::byte*>(data.data()), data.size());
}

void UdpServer::send_snapshot(uint32_t player_id,
                               const std::vector<uint8_t>& data) {
    // Same channel as delta, but sent reliably for the first packet
    std::lock_guard<std::mutex> lk(peers_mtx_);
    auto it = peers_.find(player_id);
    if (it == peers_.end() || !it->second.dc_open) return;
    it->second.dc->send(
        reinterpret_cast<const std::byte*>(data.data()), data.size());
}

// ─────────────────────────────────────────────────────────────
void UdpServer::on_data_channel_message(uint32_t player_id,
                                          const uint8_t* data, size_t len) {
    PacketSerializer::MoveInputMsg mi;
    if (PacketSerializer::decode_move_input(data, len, mi)) {
        InputPacket pkt;
        pkt.player_id = player_id;  // authoritative: use TCP-assigned ID
        pkt.seq       = mi.seq;
        pkt.mouse_x   = mi.mouse_x;
        pkt.mouse_y   = mi.mouse_y;
        pkt.split     = mi.split;
        pkt.eject     = mi.eject;
        game_loop_.push_input(pkt);
    }
}
