# MMORPG Game Server Architecture with C++

## Overview
A production-grade MMORPG server is not a single process — it is a **cluster of specialized servers** that each handle one responsibility. This document covers the common pattern seen across major MMORPGs (WoW, MapleStory, Lineage, Lost Ark, etc.) and how to build each component in C++.

---

## Full Server Architecture Diagram

```
                          [ CLIENTS ]
                              │
                    ┌─────────▼──────────┐
                    │    Load Balancer    │   (HAProxy / Nginx)
                    └─────────┬──────────┘
                              │
              ┌───────────────▼────────────────┐
              │         Gateway Server          │   C++ | Entry point
              │  - Auth token validation        │
              │  - Session management           │
              │  - Packet routing to zones      │
              └──────┬───────────┬─────────────┘
                     │           │
          ┌──────────▼──┐   ┌────▼────────────┐
          │  Login /    │   │   Zone Server    │   C++ | One per map/area
          │  Auth Server│   │  (Main Battle)   │
          └──────┬──────┘   │  - Movement      │
                 │          │  - Combat        │
          ┌──────▼──────┐   │  - AI/Mobs       │
          │  Character  │   │  - Skills        │
          │  Server     │   └────┬─────────────┘
          │  - Create   │        │
          │  - Load     │   ┌────▼─────────────┐
          │  - Save     │   │  World / Spatial  │
          └─────────────┘   │  Server           │
                            │  - Player coords  │
                            │  - AoI (Area of   │
                            │    Interest)      │
                            └────┬─────────────┘
                                 │
          ┌──────────────────────┼────────────────────────┐
          │                      │                        │
    ┌─────▼──────┐    ┌──────────▼────────┐    ┌─────────▼──────┐
    │   Chat      │   │   Game Logic /    │    │  Dungeon /      │
    │   Server    │   │   Quest Server    │    │  Instance       │
    │  - Global   │   │  - Quest state    │    │  Server         │
    │  - Guild    │   │  - NPC dialogue   │    │  - Private rooms│
    │  - Party    │   │  - Events         │    │  - Boss logic   │
    └─────────────┘   └───────────────────┘    └─────────────────┘
          │                      │                        │
          └──────────────────────▼────────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │       Message Bus        │   Redis Pub/Sub or Kafka
                    │  Inter-server messaging  │
                    └────────────┬────────────┘
                                 │
               ┌─────────────────┼─────────────────┐
               │                 │                  │
        ┌──────▼──────┐  ┌───────▼──────┐  ┌───────▼──────┐
        │    Redis     │  │  Database    │  │   Monitor    │
        │  - Sessions  │  │  (MySQL /    │  │   Server     │
        │  - Cache     │  │  PostgreSQL) │  │  - Metrics   │
        │  - PubSub    │  │  - Accounts  │  │  - Health    │
        │  - Locks     │  │  - Items     │  │  - Alerts    │
        └─────────────┘  │  - Characters│  └─────────────┘
                         └─────────────┘
```

---

## Server Components — Detail

### 1. Gateway Server
**Role**: The front door. Every client connects here first.

**Responsibilities:**
- Accept raw TCP/UDP connections from clients
- Validate authentication tokens (JWT or session key from Login Server)
- Decompress and decrypt incoming packets
- Route packets to the correct backend server (Zone, Chat, etc.)
- Handle client disconnect and session cleanup
- Rate limiting: reject packet floods from one client

```cpp
// Gateway core loop (simplified)
class GatewayServer {
    boost::asio::io_context io_ctx;
    tcp::acceptor acceptor;
    std::unordered_map<uint64_t, ClientSession> sessions;

    void accept_loop() {
        acceptor.async_accept([this](auto ec, tcp::socket socket) {
            if (!ec) {
                auto session = std::make_shared<ClientSession>(std::move(socket));
                session->start();  // read packets, validate token
            }
            accept_loop();  // keep accepting
        });
    }

    void route_packet(uint64_t client_id, Packet& pkt) {
        switch (pkt.type) {
            case PKT_MOVE:   zone_server.forward(client_id, pkt); break;
            case PKT_CHAT:   chat_server.forward(client_id, pkt); break;
            case PKT_SKILL:  battle_server.forward(client_id, pkt); break;
        }
    }
};
```

**Libraries:** Boost.Asio (async I/O), OpenSSL (TLS)

---

### 2. Login / Auth Server
**Role**: Authenticate players, issue session tokens.

**Responsibilities:**
- Verify username + password (bcrypt hash compare against DB)
- Generate session token (UUID or JWT)
- Store session → Redis with TTL (e.g., 24 hours)
- Return server list (which Gateway IP to connect to)
- Handle duplicate login (kick old session)

```cpp
// Session stored in Redis
void LoginServer::on_login(const LoginRequest& req) {
    // 1. Query DB for account
    auto account = db.query("SELECT * FROM accounts WHERE username = ?", req.username);

    // 2. Verify password
    if (!bcrypt::verify(req.password, account.password_hash)) {
        send_error(req.client, ERR_WRONG_PASSWORD);
        return;
    }

    // 3. Generate token
    std::string token = generate_uuid();

    // 4. Store in Redis: KEY = "session:{token}", VALUE = account_id, TTL = 86400s
    redis.setex("session:" + token, 86400, std::to_string(account.id));

    // 5. If already logged in — kick old session
    if (redis.exists("online:" + std::to_string(account.id))) {
        kick_session(account.id);
    }

    // 6. Respond with token + server list
    send_login_success(req.client, token, get_server_list());
}
```

---

### 3. Zone Server (Main Battle Server)
**Role**: The core gameplay server. One Zone Server per map or region.

**Responsibilities:**
- Maintain all entities in the zone: players, NPCs, mobs, items on ground
- Process player movement and broadcast to nearby players
- Run combat: damage calculation, skill effects, hit detection
- Run NPC/mob AI (state machines or behavior trees)
- Manage Area of Interest (AoI) — only send updates to nearby players
- Tick-based game loop (e.g., 30 ticks/second = update every 33ms)

```cpp
class ZoneServer {
    std::unordered_map<uint64_t, Player>  players;
    std::unordered_map<uint64_t, Monster> monsters;
    SpatialGrid grid;           // spatial partitioning for AoI
    uint64_t    tick = 0;

    void game_loop() {
        auto timer = steady_clock::now();
        while (running) {
            auto now = steady_clock::now();
            float dt  = duration<float>(now - timer).count();
            timer     = now;

            process_input_queue();    // handle all pending player packets
            update_monsters(dt);      // move mobs, run AI
            update_skills(dt);        // tick active skill effects
            update_physics(dt);       // collision, knockback
            broadcast_state();        // send delta updates to players in AoI
            ++tick;

            // sleep to maintain fixed tick rate (33ms per tick)
            std::this_thread::sleep_until(timer + milliseconds(33));
        }
    }

    void process_move(Player& player, MovePacket& pkt) {
        if (!is_valid_move(player.pos, pkt.new_pos)) return;
        player.pos = pkt.new_pos;
        grid.update(player.id, pkt.new_pos);

        // Broadcast to players within AoI radius (e.g., 100 units)
        auto nearby = grid.query(player.pos, AOI_RADIUS);
        for (auto& id : nearby)
            players[id].send(build_move_packet(player));
    }
};
```

---

### 4. World / Spatial Server
**Role**: Global position registry across all zones.

**Responsibilities:**
- Track which zone every player is currently in
- Handle zone transitions (player walks from Zone A to Zone B)
- Global AoI queries across zone boundaries
- Provide player location lookup for party, guild, friend systems

```cpp
// Redis stores global positions
// KEY = "pos:{player_id}"   VALUE = "zone_id:x:y:z"
void WorldServer::update_position(uint64_t player_id, ZonePos pos) {
    std::string val = std::to_string(pos.zone_id) + ":" +
                      std::to_string(pos.x) + ":" +
                      std::to_string(pos.y) + ":" +
                      std::to_string(pos.z);
    redis.set("pos:" + std::to_string(player_id), val);
}

ZonePos WorldServer::get_position(uint64_t player_id) {
    auto val = redis.get("pos:" + std::to_string(player_id));
    // parse zone_id:x:y:z
    return parse_pos(val);
}
```

---

### 5. Chat Server
**Role**: All in-game communication channels.

**Channels:**
- Global / World chat
- Zone / Local chat (proximity-based)
- Party chat
- Guild chat
- Private message (DM)
- System announcements

```cpp
// Chat routing via Redis Pub/Sub
void ChatServer::send_message(ChatMessage& msg) {
    switch (msg.channel) {
        case CHAT_GLOBAL:
            redis.publish("chat:global", msg.serialize());
            break;
        case CHAT_PARTY:
            redis.publish("chat:party:" + msg.party_id, msg.serialize());
            break;
        case CHAT_GUILD:
            redis.publish("chat:guild:" + msg.guild_id, msg.serialize());
            break;
        case CHAT_PRIVATE:
            deliver_direct(msg.target_id, msg);
            break;
        case CHAT_ZONE:
            // Only broadcast to players in same zone
            zone_server.broadcast_local(msg.sender_zone, msg.serialize());
            break;
    }
}
```

---

### 6. Dungeon / Instance Server
**Role**: Private isolated instances for dungeons, raids, and PvP arenas.

**Responsibilities:**
- Spawn a new isolated game world per party/group
- Run the same logic as ZoneServer but for 1 party only
- Boss AI with phase transitions
- Instance timer (auto-close after X minutes)
- Persist instance state (for reconnect)
- Clean up and release resources when instance ends

```cpp
class InstanceManager {
    std::unordered_map<uint64_t, std::unique_ptr<ZoneServer>> instances;

    uint64_t create_instance(uint32_t dungeon_id, std::vector<uint64_t> party) {
        uint64_t instance_id = generate_id();
        auto zone = std::make_unique<ZoneServer>(dungeon_id, instance_id);
        zone->set_max_players(party.size());
        zone->start();
        instances[instance_id] = std::move(zone);
        return instance_id;
    }

    void destroy_instance(uint64_t instance_id) {
        instances[instance_id]->stop();
        instances.erase(instance_id);
    }
};
```

---

### 7. Monitor Server
**Role**: Observability, health checks, and alerts.

**Tracks:**
- All server processes: CPU, RAM, packet queue depth, tick rate
- Connected players per zone
- DB query latency
- Redis latency
- Error rates and crash logs
- Auto-restart crashed servers

```cpp
// Each server reports metrics every second
struct ServerMetrics {
    std::string server_id;
    float       cpu_pct;
    uint64_t    mem_bytes;
    uint32_t    connected_clients;
    uint32_t    packet_queue_size;
    float       tick_rate;          // actual ticks/sec vs. target
    uint64_t    timestamp_ms;
};

void MonitorServer::receive_heartbeat(ServerMetrics& m) {
    redis.hset("metrics:" + m.server_id, {
        {"cpu",     std::to_string(m.cpu_pct)},
        {"mem",     std::to_string(m.mem_bytes)},
        {"clients", std::to_string(m.connected_clients)},
        {"tick",    std::to_string(m.tick_rate)}
    });

    // Alert if tick rate drops below 80% of target
    if (m.tick_rate < TARGET_TICK * 0.8f)
        send_alert("TICK_RATE_LOW", m.server_id, m.tick_rate);

    // Alert if server stops sending heartbeats (crash detection)
    redis.setex("alive:" + m.server_id, 5, "1");  // TTL = 5 seconds
}
```

**Tools:** Prometheus (metrics scraping) + Grafana (dashboards) + PagerDuty (alerts)

---

### 8. Redis — Roles in the Server Cluster

| Redis Use | Key Pattern | Description |
|-----------|------------|-------------|
| Session store | `session:{token}` | Auth token → account_id, TTL 24h |
| Online registry | `online:{account_id}` | Is this account logged in right now |
| Player position | `pos:{player_id}` | Global zone + coordinates |
| Pub/Sub chat | `chat:global`, `chat:guild:{id}` | Message broadcasting between servers |
| Distributed lock | `lock:item:{item_id}` | Prevent duplicate item pickup |
| Rate limiting | `rl:{client_ip}` | Packet flood protection |
| Leaderboard | `leaderboard:pvp` | Sorted set for real-time rankings |
| Server metrics | `metrics:{server_id}` | Hash of CPU/mem/tick data |
| Server alive | `alive:{server_id}` | Heartbeat key, TTL 5s |
| Cooldown | `cd:{player_id}:{skill_id}` | Skill cooldown, TTL = cooldown duration |

---

## Packet Design

### Packet Structure (Binary)

```
┌──────────┬──────────┬──────────┬─────────────────────┐
│ Size (2B)│ Type (2B)│ Seq  (4B)│ Payload (variable)  │
└──────────┴──────────┴──────────┴─────────────────────┘
```

```cpp
#pragma pack(push, 1)
struct PacketHeader {
    uint16_t size;      // total packet size including header
    uint16_t type;      // packet type ID
    uint32_t sequence;  // sequence number for ordering
};
#pragma pack(pop)

// Example: Move packet
struct MovePacket : PacketHeader {
    float x, y, z;
    float yaw;
    uint8_t move_state;  // 0=idle, 1=walk, 2=run, 3=dash
};
```

### Packet Type IDs (Example)

```cpp
enum PacketType : uint16_t {
    // Client → Server
    PKT_CS_LOGIN        = 0x0001,
    PKT_CS_MOVE         = 0x0101,
    PKT_CS_ATTACK       = 0x0102,
    PKT_CS_USE_SKILL    = 0x0103,
    PKT_CS_CHAT         = 0x0201,
    PKT_CS_BUY_ITEM     = 0x0301,

    // Server → Client
    PKT_SC_LOGIN_OK     = 0x1001,
    PKT_SC_SPAWN_PLAYER = 0x1101,
    PKT_SC_MOVE         = 0x1102,
    PKT_SC_DAMAGE       = 0x1103,
    PKT_SC_CHAT         = 0x1201,
    PKT_SC_INVENTORY    = 0x1301,
};
```

---

## Combat System Logic

```cpp
struct DamageResult {
    uint32_t damage;
    bool     is_critical;
    bool     is_miss;
};

DamageResult CombatEngine::calculate_damage(const Player& attacker,
                                             const Monster& defender,
                                             const Skill& skill) {
    DamageResult result{};

    // 1. Base damage
    float base = attacker.attack * skill.multiplier;

    // 2. Critical hit check
    float crit_roll = random_float(0.f, 100.f);
    result.is_critical = (crit_roll < attacker.crit_rate);
    if (result.is_critical) base *= attacker.crit_damage;

    // 3. Miss check
    float hit_roll = random_float(0.f, 100.f);
    float hit_chance = attacker.accuracy - defender.evasion;
    hit_chance = std::clamp(hit_chance, 5.f, 95.f);  // always 5%-95%
    if (hit_roll > hit_chance) { result.is_miss = true; return result; }

    // 4. Defense reduction
    float def_factor = 1.f - (defender.defense / (defender.defense + 500.f));
    base *= def_factor;

    // 5. Elemental modifier
    base *= get_element_modifier(skill.element, defender.element_weakness);

    // 6. Random variance ±10%
    base *= random_float(0.9f, 1.1f);

    result.damage = static_cast<uint32_t>(base);
    return result;
}
```

---

## Area of Interest (AoI) — Spatial Grid

```cpp
class SpatialGrid {
    static constexpr int CELL_SIZE = 50;  // 50 units per cell
    std::unordered_map<uint64_t, std::unordered_set<uint64_t>> cells;

    uint64_t cell_key(float x, float y) {
        int cx = static_cast<int>(x) / CELL_SIZE;
        int cy = static_cast<int>(y) / CELL_SIZE;
        return (uint64_t(cx) << 32) | uint64_t(cy);
    }

public:
    void update(uint64_t entity_id, float x, float y) {
        // Remove from old cell, add to new cell
        auto key = cell_key(x, y);
        cells[key].insert(entity_id);
    }

    // Return all entity IDs within radius
    std::vector<uint64_t> query(float x, float y, float radius) {
        std::vector<uint64_t> result;
        int range = static_cast<int>(radius) / CELL_SIZE + 1;
        int cx = static_cast<int>(x) / CELL_SIZE;
        int cy = static_cast<int>(y) / CELL_SIZE;

        for (int dx = -range; dx <= range; dx++)
        for (int dy = -range; dy <= range; dy++) {
            uint64_t key = (uint64_t(cx+dx) << 32) | uint64_t(cy+dy);
            if (cells.count(key))
                for (auto id : cells[key])
                    result.push_back(id);
        }
        return result;
    }
};
```

---

## Monster AI — State Machine

```cpp
enum class AIState { IDLE, PATROL, CHASE, ATTACK, FLEE, DEAD };

class MonsterAI {
    AIState state = AIState::IDLE;
    uint64_t target_id = 0;

    void update(Monster& mob, float dt) {
        switch (state) {
            case AIState::IDLE:
                if (auto player = find_player_in_range(mob, AGGRO_RANGE)) {
                    target_id = player->id;
                    state = AIState::CHASE;
                } else {
                    patrol(mob, dt);
                }
                break;

            case AIState::CHASE:
                if (!target_in_range(mob, target_id, AGGRO_RANGE)) {
                    state = AIState::IDLE;  // target too far — give up
                    break;
                }
                move_toward(mob, target_id, dt);
                if (target_in_range(mob, target_id, ATTACK_RANGE))
                    state = AIState::ATTACK;
                break;

            case AIState::ATTACK:
                if (can_attack(mob))
                    perform_attack(mob, target_id);
                if (!target_in_range(mob, target_id, ATTACK_RANGE))
                    state = AIState::CHASE;
                break;

            case AIState::FLEE:
                if (mob.hp_pct() > 0.3f) state = AIState::ATTACK;
                else flee_from_target(mob, target_id, dt);
                break;

            case AIState::DEAD:
                on_death(mob);
                break;
        }
    }
};
```

---

## Inter-Server Communication

Servers communicate via **Redis Pub/Sub** for events and **direct TCP** for high-frequency data.

```
Low frequency events (chat, login, disconnect):
  Server A  →  redis.publish("channel", message)  →  Server B subscribes

High frequency data (position sync between Zone Servers):
  Zone A  →  direct TCP socket  →  Zone B
```

```cpp
// Zone Server subscribes to player transfer events
void ZoneServer::subscribe_events() {
    redis_sub.subscribe("zone:" + zone_id + ":transfer", [this](auto msg) {
        auto transfer = parse<TransferRequest>(msg);
        accept_player(transfer.player_id, transfer.entry_point);
    });

    redis_sub.subscribe("zone:" + zone_id + ":broadcast", [this](auto msg) {
        // System announcements, server-wide events
        broadcast_all(msg);
    });
}
```

---

## Project File Structure

```
mmorpg-server/
├── common/                     # Shared code across all servers
│   ├── packet.h                # Packet definitions and serialization
│   ├── redis_client.h          # Redis wrapper
│   ├── db_pool.h               # Database connection pool
│   ├── logger.h                # Logging utility
│   └── config.h                # Config loader (JSON/YAML)
│
├── gateway/                    # Gateway Server
│   ├── main.cpp
│   ├── gateway_server.h/.cpp
│   ├── client_session.h/.cpp
│   └── packet_router.h/.cpp
│
├── login/                      # Login / Auth Server
│   ├── main.cpp
│   ├── login_server.h/.cpp
│   └── auth_handler.h/.cpp
│
├── zone/                       # Zone / Battle Server
│   ├── main.cpp
│   ├── zone_server.h/.cpp
│   ├── game_loop.h/.cpp
│   ├── combat_engine.h/.cpp
│   ├── skill_system.h/.cpp
│   ├── spatial_grid.h/.cpp
│   └── aoi_manager.h/.cpp
│
├── world/                      # World / Spatial Server
│   ├── main.cpp
│   └── world_server.h/.cpp
│
├── chat/                       # Chat Server
│   ├── main.cpp
│   └── chat_server.h/.cpp
│
├── dungeon/                    # Instance / Dungeon Server
│   ├── main.cpp
│   ├── instance_manager.h/.cpp
│   └── boss_ai.h/.cpp
│
├── monitor/                    # Monitor Server
│   ├── main.cpp
│   └── monitor_server.h/.cpp
│
├── monster_ai/                 # AI module (shared by zone + dungeon)
│   ├── ai_state_machine.h/.cpp
│   ├── behavior_tree.h/.cpp
│   └── pathfinding.h/.cpp      # A* or NavMesh
│
├── database/
│   └── schema.sql              # Full DB schema
│
├── config/
│   ├── gateway.json
│   ├── zone.json
│   └── monitor.json
│
└── CMakeLists.txt
```

---

## Libraries to Use

| Library | Purpose |
|---------|---------|
| **Boost.Asio** | Async TCP/UDP networking |
| **hiredis** | Redis client for C++ |
| **mysql-connector-cpp** or **libpqxx** | MySQL / PostgreSQL |
| **protobuf** or **flatbuffers** | Packet serialization |
| **spdlog** | Fast logging |
| **nlohmann/json** | JSON config parsing |
| **recastnavigation** | NavMesh pathfinding for AI |
| **OpenSSL** | TLS encryption for Gateway |
| **prometheus-cpp** | Metrics for Monitor Server |
| **Catch2 / GoogleTest** | Unit testing |

---

## Database Schema (Core Tables)

```sql
-- Accounts
CREATE TABLE accounts (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(32) UNIQUE NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    email         VARCHAR(128),
    created_at    TIMESTAMP DEFAULT NOW(),
    banned        BOOLEAN DEFAULT FALSE
);

-- Characters
CREATE TABLE characters (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT REFERENCES accounts(id),
    name       VARCHAR(32) UNIQUE NOT NULL,
    class_id   INT,
    level      INT DEFAULT 1,
    exp        BIGINT DEFAULT 0,
    hp         INT,
    mp         INT,
    map_id     INT,
    pos_x      FLOAT,
    pos_y      FLOAT,
    pos_z      FLOAT
);

-- Inventory
CREATE TABLE inventory (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    character_id BIGINT REFERENCES characters(id),
    item_id      INT,
    slot         INT,
    quantity     INT DEFAULT 1,
    enchant_lvl  INT DEFAULT 0
);

-- Skills
CREATE TABLE character_skills (
    character_id BIGINT REFERENCES characters(id),
    skill_id     INT,
    skill_level  INT DEFAULT 1,
    PRIMARY KEY (character_id, skill_id)
);

-- Quest progress
CREATE TABLE quest_progress (
    character_id BIGINT REFERENCES characters(id),
    quest_id     INT,
    status       ENUM('active','completed','failed'),
    progress     JSON,
    PRIMARY KEY (character_id, quest_id)
);
```

---

## Build & Run

```bash
# Dependencies (Ubuntu/Debian)
sudo apt install cmake libboost-all-dev libssl-dev libhiredis-dev \
                 libmysqlclient-dev libprotobuf-dev protobuf-compiler

# Build
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$(nproc)

# Run each server (separate terminals or Docker containers)
./bin/login_server   --config config/login.json
./bin/gateway        --config config/gateway.json
./bin/zone_server    --config config/zone.json --zone-id 1001
./bin/chat_server    --config config/chat.json
./bin/world_server   --config config/world.json
./bin/monitor        --config config/monitor.json
```

---

## Challenges

| Challenge | Description |
|-----------|-------------|
| Tick Rate Stability | Game loop must maintain consistent 30/60 ticks/sec under load |
| AoI at Scale | 1000+ players in one zone — broadcasting naively is O(n²) |
| Cheat Prevention | Client-side movement must be validated server-side (anti-speedhack) |
| Distributed Transactions | Item trade between two players across servers must be atomic |
| Hot Reloading | Update monster stats or skill data without taking server down |
| Zone Handoff | Transfer player from Zone A server process to Zone B smoothly |
| Crash Recovery | Server crash must not lose player progress (write-ahead log) |
| DDoS Protection | Game servers are frequent DDoS targets — need traffic filtering |
| Memory Management | C++ manual memory; use pools and RAII to prevent leaks under load |
| Latency Compensation | Handle 100–200ms client latency fairly in hit detection |
