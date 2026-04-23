#pragma once
// ─────────────────────────────────────────────────────────────
//  UDP Server via WebRTC DataChannel (libdatachannel)
//
//  WHY WebRTC DataChannel for UDP:
//    Browsers cannot use raw UDP sockets.
//    WebRTC DataChannel with { ordered: false, maxRetransmits: 0 }
//    gives true UDP semantics (unreliable, unordered) over DTLS/SCTP.
//    libdatachannel is a pure C++ WebRTC implementation, VS2017 compatible.
//
//  FLOW:
//    1. Client connects TCP WebSocket → sends SDP offer
//    2. UdpServer::handle_sdp_offer() → returns SDP answer
//    3. ICE candidates exchanged over TCP WebSocket
//    4. DataChannel opens → battle packets flow directly
//    5. Server receives MoveInput (UDP), sends DeltaUpdate (UDP)
// ─────────────────────────────────────────────────────────────
#include <rtc/rtc.hpp>          // libdatachannel
#include <unordered_map>
#include <memory>
#include <mutex>
#include <functional>
#include <vector>
#include <cstdint>
#include <string>
#include "GameLoop.h"

struct PeerState {
    std::shared_ptr<rtc::PeerConnection>  pc;
    std::shared_ptr<rtc::DataChannel>     dc;   // unreliable, unordered
    uint32_t player_id = 0;
    bool     dc_open   = false;
};

class UdpServer {
public:
    // ice_send_cb: called to send ICE candidates back over TCP
    using IceSendCb = std::function<void(uint32_t player_id,
                                          const std::string& candidate,
                                          const std::string& sdp_mid,
                                          int sdp_m_line)>;

    UdpServer(GameLoop& game_loop, IceSendCb ice_send);
    ~UdpServer();

    // Called by TcpServer when SDP offer arrives from client
    // Returns SDP answer to be sent back over TCP
    std::string handle_sdp_offer(uint32_t player_id,
                                  const std::string& offer_sdp);

    // Called by TcpServer when ICE candidate arrives from client
    void handle_ice_candidate(uint32_t player_id,
                               const std::string& candidate,
                               const std::string& sdp_mid,
                               int sdp_m_line);

    // Called by TcpServer when player disconnects
    void remove_peer(uint32_t player_id);

    // Send delta/snapshot to a player over UDP DataChannel
    void send_delta(uint32_t player_id, const std::vector<uint8_t>& data);
    void send_snapshot(uint32_t player_id, const std::vector<uint8_t>& data);

private:
    void on_data_channel_message(uint32_t player_id,
                                  const uint8_t* data, size_t len);

    GameLoop&  game_loop_;
    IceSendCb  ice_send_cb_;

    std::unordered_map<uint32_t, PeerState> peers_;
    std::mutex                              peers_mtx_;
};
