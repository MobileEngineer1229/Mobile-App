#include "PacketSerializer.h"
#include "GameLoop.h"   // EntitySnapshot
// Generated protobuf headers (run protoc first — see CMakeLists.txt)
#include "agario.pb.h"

// ─────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────
static float mass_to_radius(float m) {
    return std::sqrt(m) * 10.0f; // Config::MASS_RADIUS_SCALE
}

static agario::PlayerState fill_player_state(
        entt::registry& reg, uint32_t player_id) {
    agario::PlayerState ps;
    bool found_info = false;

    for (auto [e, info] : reg.view<PlayerInfo>().each()) {
        if (info.id != player_id) continue;
        ps.set_player_id(info.id);
        ps.set_name(info.name);
        ps.set_color(info.color);
        ps.set_skin_id(info.skin_id);
        ps.set_team(static_cast<agario::Team>(info.team));
        ps.set_level(info.level);
        found_info = true;
        break;
    }

    float total = 0.f;
    for (auto [e, cell, pos, mass] :
         reg.view<CellComp, Pos, Mass>().each()) {
        if (cell.player_id != player_id) continue;
        auto* c = ps.add_cells();
        c->set_cell_id(cell.cell_index);
        c->set_x(pos.x);
        c->set_y(pos.y);
        c->set_mass(mass.value);
        c->set_radius(mass_to_radius(mass.value));
        total += mass.value;
    }
    ps.set_total_mass(total);
    return ps;
}

// ─────────────────────────────────────────────────────────────
//  UDP — Full Snapshot
// ─────────────────────────────────────────────────────────────
std::vector<uint8_t> PacketSerializer::build_full_snapshot(
        entt::registry& reg, uint64_t tick) {
    agario::UdpPacket wrapper;
    wrapper.set_type(agario::UdpPacket::FULL_SNAPSHOT);
    auto* snap = wrapper.mutable_full_snapshot();
    snap->set_tick(static_cast<uint32_t>(tick));

    // Players
    std::unordered_set<uint32_t> seen_players;
    for (auto [e, cell] : reg.view<CellComp>().each()) {
        if (seen_players.count(cell.player_id)) continue;
        seen_players.insert(cell.player_id);
        *snap->add_players() = fill_player_state(reg, cell.player_id);
    }

    // Foods
    for (auto [e, pos, mass] : reg.view<FoodTag, Pos, Mass>().each()) {
        auto* f = snap->add_foods();
        f->set_id(static_cast<uint32_t>(e));
        f->set_x(pos.x); f->set_y(pos.y);
        f->set_mass(mass.value);
        f->set_color(0xFF00CC44);
    }

    // Viruses
    for (auto [e, vtag, pos, circ] : reg.view<VirusTag, Pos, Circle>().each()) {
        auto* v = snap->add_viruses();
        v->set_id(static_cast<uint32_t>(e));
        v->set_x(pos.x); v->set_y(pos.y);
        v->set_radius(circ.radius);
        v->set_is_red(vtag.is_red);
    }

    std::string bytes;
    wrapper.SerializeToString(&bytes);
    return std::vector<uint8_t>(bytes.begin(), bytes.end());
}

// ─────────────────────────────────────────────────────────────
//  UDP — Delta Update (only changed entities)
// ─────────────────────────────────────────────────────────────
std::vector<uint8_t> PacketSerializer::build_delta(
        entt::registry& reg,
        const WorldEvents& events,
        uint64_t tick,
        std::unordered_map<uint32_t, EntitySnapshot>& prev) {
    agario::UdpPacket wrapper;
    wrapper.set_type(agario::UdpPacket::DELTA_UPDATE);
    auto* delta = wrapper.mutable_delta_update();
    delta->set_tick(static_cast<uint32_t>(tick));

    // Changed players (moved or mass changed)
    std::unordered_set<uint32_t> changed_pids;
    for (auto [e, cell, pos, mass, circ] :
         reg.view<CellComp, Pos, Mass, Circle>().each()) {
        uint32_t eid = static_cast<uint32_t>(e);
        auto it = prev.find(eid);
        bool changed = (it == prev.end())
            || std::abs(it->second.x - pos.x) > 0.1f
            || std::abs(it->second.y - pos.y) > 0.1f
            || std::abs(it->second.mass - mass.value) > 0.1f;

        if (changed) {
            changed_pids.insert(cell.player_id);
            prev[eid] = { pos.x, pos.y, circ.radius, mass.value, true };
        }
    }
    for (uint32_t pid : changed_pids)
        *delta->add_changed_players() = fill_player_state(reg, pid);

    // Removed players
    for (auto& eid : events.removed_player_ids)
        delta->add_removed_players(eid);

    // New / eaten foods
    for (auto eid : events.new_food_ids) {
        auto e = static_cast<entt::entity>(eid);
        if (!reg.valid(e)) continue;
        auto* f = delta->add_new_foods();
        auto& p = reg.get<Pos>(e);
        auto& m = reg.get<Mass>(e);
        f->set_id(eid); f->set_x(p.x); f->set_y(p.y);
        f->set_mass(m.value); f->set_color(0xFF00CC44);
    }
    for (auto eid : events.eaten_food_ids)
        delta->add_eaten_food_ids(eid);

    std::string bytes;
    wrapper.SerializeToString(&bytes);
    return std::vector<uint8_t>(bytes.begin(), bytes.end());
}

// ─────────────────────────────────────────────────────────────
//  TCP — Welcome
// ─────────────────────────────────────────────────────────────
std::vector<uint8_t> PacketSerializer::build_welcome(
        uint32_t player_id, float ww, float wh,
        uint8_t mode, uint8_t team,
        const std::string& sdp_answer,
        uint32_t level, uint32_t xp, uint32_t xp_to_next) {
    agario::TcpPacket wrapper;
    wrapper.set_type(agario::TcpPacket::WELCOME);
    auto* w = wrapper.mutable_welcome();
    w->set_player_id(player_id);
    w->set_world_w(ww); w->set_world_h(wh);
    w->set_mode(static_cast<agario::GameMode>(mode));
    w->set_team(static_cast<agario::Team>(team));
    w->set_sdp_answer(sdp_answer);
    w->set_level(level); w->set_xp(xp); w->set_xp_to_next(xp_to_next);
    std::string bytes;
    wrapper.SerializeToString(&bytes);
    return std::vector<uint8_t>(bytes.begin(), bytes.end());
}

// ─────────────────────────────────────────────────────────────
//  TCP — Death
// ─────────────────────────────────────────────────────────────
std::vector<uint8_t> PacketSerializer::build_death(
        const std::string& killer, uint32_t score,
        uint32_t rank, uint32_t xp_gained,
        bool level_up, uint32_t new_level) {
    agario::TcpPacket wrapper;
    wrapper.set_type(agario::TcpPacket::DEATH);
    auto* d = wrapper.mutable_death();
    d->set_killer_name(killer);
    d->set_final_score(score);
    d->set_rank(rank);
    d->set_xp_gained(xp_gained);
    d->set_level_up(level_up);
    d->set_new_level(new_level);
    std::string bytes;
    wrapper.SerializeToString(&bytes);
    return std::vector<uint8_t>(bytes.begin(), bytes.end());
}

// ─────────────────────────────────────────────────────────────
//  TCP — Leaderboard
// ─────────────────────────────────────────────────────────────
std::vector<uint8_t> PacketSerializer::build_leaderboard(
        const std::vector<LeaderboardEntry>& entries,
        uint32_t my_rank, float my_mass) {
    agario::TcpPacket wrapper;
    wrapper.set_type(agario::TcpPacket::LEADERBOARD);
    auto* lb = wrapper.mutable_leaderboard();
    lb->set_my_rank(my_rank);
    lb->set_my_mass(my_mass);
    for (size_t i = 0; i < entries.size(); ++i) {
        auto* e = lb->add_entries();
        e->set_rank(static_cast<uint32_t>(i + 1));
        e->set_name(entries[i].name);
        e->set_total_mass(static_cast<float>(entries[i].score));
    }
    std::string bytes;
    wrapper.SerializeToString(&bytes);
    return std::vector<uint8_t>(bytes.begin(), bytes.end());
}

// ─────────────────────────────────────────────────────────────
//  TCP — XP Update  (LEVEL_UP packet reused for incremental XP ticks)
// ─────────────────────────────────────────────────────────────
std::vector<uint8_t> PacketSerializer::build_xp_update(
        uint32_t level, uint32_t xp, uint32_t xp_to_next) {
    agario::TcpPacket wrapper;
    wrapper.set_type(agario::TcpPacket::LEVEL_UP);
    auto* lu = wrapper.mutable_level_up();
    lu->set_new_level(level);
    lu->set_xp(xp);
    lu->set_xp_to_next(xp_to_next);
    std::string bytes;
    wrapper.SerializeToString(&bytes);
    return std::vector<uint8_t>(bytes.begin(), bytes.end());
}

// ─────────────────────────────────────────────────────────────
//  TCP — Chat
// ─────────────────────────────────────────────────────────────
std::vector<uint8_t> PacketSerializer::build_chat(
        uint32_t player_id, const std::string& sender,
        const std::string& content, uint8_t team) {
    agario::TcpPacket wrapper;
    wrapper.set_type(agario::TcpPacket::CHAT_MSG);
    auto* c = wrapper.mutable_chat_msg();
    c->set_player_id(player_id);
    c->set_sender(sender);
    c->set_content(content);
    c->set_team(static_cast<agario::Team>(team));
    std::string bytes;
    wrapper.SerializeToString(&bytes);
    return std::vector<uint8_t>(bytes.begin(), bytes.end());
}

// ─────────────────────────────────────────────────────────────
//  Decode helpers
// ─────────────────────────────────────────────────────────────
bool PacketSerializer::decode_join(const uint8_t* data, size_t len,
                                    JoinReq& out) {
    agario::TcpPacket wrapper;
    if (!wrapper.ParseFromArray(data, static_cast<int>(len))) return false;
    if (wrapper.type() != agario::TcpPacket::JOIN_REQUEST) return false;
    auto& jr = wrapper.join_request();
    out.name       = jr.name();
    out.skin       = jr.skin_id();
    out.party_code = jr.party_code();
    out.mode       = static_cast<uint8_t>(jr.mode());
    return true;
}

bool PacketSerializer::decode_sdp_offer(const uint8_t* data, size_t len,
                                         SdpOfferMsg& out) {
    agario::TcpPacket wrapper;
    if (!wrapper.ParseFromArray(data, static_cast<int>(len))) return false;
    if (wrapper.type() != agario::TcpPacket::SDP_OFFER) return false;
    out.sdp = wrapper.sdp_offer().sdp();
    return true;
}

bool PacketSerializer::decode_ice(const uint8_t* data, size_t len,
                                   IceCandMsg& out) {
    agario::TcpPacket wrapper;
    if (!wrapper.ParseFromArray(data, static_cast<int>(len))) return false;
    if (wrapper.type() != agario::TcpPacket::ICE_CANDIDATE) return false;
    out.candidate  = wrapper.ice_candidate().candidate();
    out.sdp_mid    = wrapper.ice_candidate().sdp_mid();
    out.sdp_m_line = wrapper.ice_candidate().sdp_m_line();
    return true;
}

bool PacketSerializer::decode_move_input(const uint8_t* data, size_t len,
                                          MoveInputMsg& out) {
    agario::UdpPacket wrapper;
    if (!wrapper.ParseFromArray(data, static_cast<int>(len))) return false;
    if (wrapper.type() != agario::UdpPacket::MOVE_INPUT) return false;
    auto& mi = wrapper.move_input();
    out.player_id = mi.player_id();
    out.seq       = mi.seq();
    out.mouse_x   = mi.mouse_x();
    out.mouse_y   = mi.mouse_y();
    out.split     = mi.split();
    out.eject     = mi.eject();
    return true;
}
