#pragma once
// ─────────────────────────────────────────────────────────────
//  Packet Serializer — Protobuf encode/decode
//  TCP packets  → TcpPacket wrapper  (reliable events)
//  UDP packets  → UdpPacket wrapper  (battle state, delta)
// ─────────────────────────────────────────────────────────────
#include <vector>
#include <cstdint>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <entt/entt.hpp>
#include "Components.h"
#include "Systems.h"
#include "RedisClient.h"

struct EntitySnapshot;

class PacketSerializer {
public:
    // ── UDP (battle channel) ──────────────────────────────────
    static std::vector<uint8_t> build_full_snapshot(
        entt::registry& reg, uint64_t tick);

    static std::vector<uint8_t> build_delta(
        entt::registry& reg,
        const WorldEvents& events,
        uint64_t tick,
        std::unordered_map<uint32_t, EntitySnapshot>& prev);

    // ── TCP (reliable channel) ────────────────────────────────
    static std::vector<uint8_t> build_welcome(
        uint32_t player_id, float world_w, float world_h,
        uint8_t mode, uint8_t team,
        const std::string& sdp_answer,
        uint32_t level, uint32_t xp, uint32_t xp_to_next);

    static std::vector<uint8_t> build_death(
        const std::string& killer_name, uint32_t score,
        uint32_t rank, uint32_t xp_gained,
        bool level_up, uint32_t new_level);

    static std::vector<uint8_t> build_leaderboard(
        const std::vector<LeaderboardEntry>& entries,
        uint32_t my_rank, float my_mass);

    static std::vector<uint8_t> build_xp_update(
        uint32_t level, uint32_t xp, uint32_t xp_to_next);

    static std::vector<uint8_t> build_chat(
        uint32_t player_id, const std::string& sender,
        const std::string& content, uint8_t team);

    // ── Decode helpers (called by TcpServer / UdpServer) ──────
    struct JoinReq {
        std::string name, skin, party_code;
        uint8_t     mode = 0;
    };
    static bool decode_join(const uint8_t* data, size_t len, JoinReq& out);

    struct SdpOfferMsg { std::string sdp; };
    static bool decode_sdp_offer(const uint8_t* data, size_t len,
                                  SdpOfferMsg& out);

    struct IceCandMsg { std::string candidate, sdp_mid; int sdp_m_line; };
    static bool decode_ice(const uint8_t* data, size_t len, IceCandMsg& out);

    struct MoveInputMsg {
        uint32_t player_id, seq;
        float    mouse_x, mouse_y;
        bool     split, eject;
    };
    static bool decode_move_input(const uint8_t* data, size_t len,
                                   MoveInputMsg& out);
};
