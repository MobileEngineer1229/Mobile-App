# H5Fish Server Communication Architecture

## Table of Contents
1. [Server Overview](#1-server-overview)
2. [Network Topology](#2-network-topology)
3. [Transport Layer](#3-transport-layer)
4. [Packet Format](#4-packet-format)
5. [Protocol Buffers](#5-protocol-buffers)
6. [Inter-Server Communication](#6-inter-server-communication)
7. [Client-Server Communication](#7-client-server-communication)
8. [Redis Cache Layer](#8-redis-cache-layer)
9. [MongoDB Data Layer](#9-mongodb-data-layer)
10. [Startup Sequence](#10-startup-sequence)
11. [Setting Up a New Game Server](#11-setting-up-a-new-game-server)

---

## 1. Server Overview

| Server | ServerId | Port | Role |
|--------|----------|------|------|
| **Monitor** | 65537 | 11001 | Service registry & discovery hub |
| **Center** | 65536 | 20000 | Player session & login gateway |
| **Global** | 70000 | 15000 | Cross-server rankings, airdrops, global activities |
| **World** | 66000+ | 12001+ | Player profiles, shop, quests, social, payments |
| **Gate** | 1, 2, … | 15001, 15002, … | Client WebSocket endpoint & message router |
| **Logic** | 1000000+ | 14014+ | Real-time game room execution (loaded as DLL) |

### Monitor (`server/monitor/`)
Central registry. Every other server TCP-connects to Monitor on startup and sends a `packet_server_register`. Monitor records each server's type, ID, IP, and port, then broadcasts the full server list to everyone via `packet_updata_servers_info`. When any server drops, Monitor broadcasts `packet_other_server_disconnect`.

### Center (`server/center/`)
Auth gateway for client logins. Receives player session from Gate, authenticates, assigns a World server, and forwards the session to it. Manages player lifecycle: connect, login, keep-alive, disconnect.
- **MongoDB**: PlayerDB_DWC5 (DbId 1)

### Global (`server/global/`)
Aggregates data that must be consistent across all servers: grand-prix rankings, global airdrops, cross-server activities, world rankings.
- **MongoDB**: LogDB_DWC5 (3), AccountDB5 (5), ConfigDB5 (6), GlobalDB (7)
- **Redis**: CacheId 2 (global data)

### World (`server/world/`)
Per-player persistence layer. Handles all operations that outlive a game session: bag items, currency, quest progress, mail, friends, shop purchases, payments, VIP, activity data.
- **MongoDB**: PlayerDB_DWC5 (1), PaymentDB5 (2), LogDB_DWC5 (3), GameDB5 (4), AccountDB5 (5), ConfigDB5 (6)
- **Redis**: CacheIds 1–4 (grand-prix, global data, rankings, task sync)

### Gate (`server/gate/`)
Client-facing WebSocket server. Assigns each connection a `sessionid`. Forwards game messages from client to the appropriate Logic or World server, and relays responses back. Does not perform any game logic.
- **MongoDB**: ConfigDB5 (6) — reads server list for load balancing

### Logic (`server/logic/`, game DLL: `games/game_fishlord.dll`)
One or more instances, each hosting a set of game rooms. Runs the real-time fishing game: bullet physics, fish AI, coin accounting, room broadcasts. Loaded game type is specified by `GameDLL` in config.
- **MongoDB**: LogDB_DWC5 (3), GameDB5 (4), ConfigDB5 (6)
- **Redis**: CacheIds 1–4

---

## 2. Network Topology

```
                          ┌──────────────────────────┐
                          │         MONITOR           │
                          │   TCP :11001              │
                          └──────────┬───────────────┘
                                     │  registers / discovers
          ┌──────────┬───────────────┼───────────────┬──────────┐
          │          │               │               │          │
       CENTER     GLOBAL           WORLD           LOGIC      GATE
      :20000      :15000          :12001          :14014     :15001
          │          │               │               │          │
          └──────────┴───────────────┴───────────────┘          │
                    All TCP server-to-server connections         │
                                                                 │ WebSocket (TCP)
                                                          ┌──────┘
                                                       CLIENTS
                                                 (Web H5 / WeChat / App)
```

**Server-to-server**: plain TCP, all connections established after Monitor registers the server.  
**Client-to-server**: WebSocket over TCP (port 15001/15002 on Gate).

---

## 3. Transport Layer

### Server ↔ Server: Raw TCP
- Library: **Boost.Asio** (async I/O, `boost::asio::io_context`)
- Connection class: `peer_tcp` (`share/common/net/peer_tcp.h/.cpp`)
- No WebSocket framing — raw binary stream
- Each message = 12-byte header + Protobuf payload (see §4)
- **No payload encryption** between servers (`packet_head_s::buffer_decryption` is a no-op)

### Client ↔ Gate: WebSocket over TCP
- Class: `WsPeer` (`share/common/ws/WsPeer.h`) subclassed by `web_peer` (`server/gate/web_peer.h`)
- Standard HTTP Upgrade handshake, then binary WebSocket frames
- Inside each WebSocket frame: same 12-byte header + Protobuf payload (see §4)
- **Payload IS encrypted** using XOR cipher (`packet_head_c`, see §4.3)

### UDP
**Not used anywhere** in the current codebase. All communication is TCP (or WebSocket-over-TCP).

---

## 4. Packet Format

Every message (server-to-server and client-to-server) uses the same on-wire layout:

```
┌───────────────────────────────────────────────────────┐
│                  PACKET (variable length)              │
├────────────────────────────┬──────────────────────────┤
│  HEADER  (12 bytes fixed)  │  PAYLOAD  (variable)      │
├──────────┬────────┬────────┼──────────────────────────┤
│tick_time │pkt_id  │pkt_size│  Protobuf serialized msg  │
│ uint32   │ uint16 │ uint16 │  (pkt_size bytes)          │
│  4 bytes │ 2 bytes│ 2 bytes│                            │
├──────────┴────────┴────────┤                            │
│  head_mark[4]              │                            │
│  {'$','3','&','@'}         │                            │
└────────────────────────────┴──────────────────────────┘
```

- `tick_time`: `GetTickCount()` at send time  
- `packet_id`: message type enum (see `server_msg_type.proto`, `client2*_msg_type.proto`)  
- `packet_size`: byte length of the Protobuf payload only  
- `head_mark`: magic bytes `0x24 0x33 0x26 0x40` — validated on receive; packet dropped if wrong

Source: `share/common/net/packet_head.h`, `packet_head.cpp`

### 4.1 Server-to-Server Header (`packet_head_s`)
No encryption. Header and payload transmitted as-is.

### 4.2 Client Header Encoding (`packet_head_c`)
The 12-byte header is XOR-obfuscated before sending to a client:

**Encoding (send side):**
```
Key: {0x12,0x33,0xa8,0x5c, 0x6b,0x86,0x05,0x01, 0xff,0xf3,0x5e,0xec}

Step 1 — rolling XOR bytes 4–11 against bytes 0–3 (repeating):
  buf[i] ^= buf[i % 4]   for i in [4..11]

Step 2 — XOR all 12 bytes with the static key:
  buf[i] ^= key[i]        for i in [0..11]
```

**Decoding (receive side)** — reverse of encoding:
```
Step 1 — XOR all 12 bytes with the static key
Step 2 — rolling XOR bytes 4–11 against bytes 0–3
```

### 4.3 Payload Decryption (Client ↔ Gate)
After decoding the header, the Protobuf payload bytes are XOR'd using the first 4 bytes of the **decoded** header as a rolling 4-byte key:
```
payload[i] ^= decoded_header[i % 4]
```

### 4.4 Compression
**None.** Protobuf binary is sent raw. No zlib, LZ4, or Snappy in the current codebase.

---

## 5. Protocol Buffers

### Version
**Protobuf 3.21** (proto3 syntax). Binary format only — no JSON over the wire.  
Generated `.pb.h`/`.pb.cc` files are checked in alongside `.proto` files.  
Compiler: `protoc.exe` in each protocol directory.

### Proto File Locations

| Directory | Purpose |
|-----------|---------|
| `server/protocol/` | Server-to-server messages |
| `share/protocol/` | Client-to-server messages (also used by the TypeScript client) |

### Server Protocol Files (`server/protocol/`)

| File | Contents |
|------|---------|
| `server_msg_type.proto` | Enum of all server-to-server message type IDs |
| `server_base.proto` | Base types reused across server messages |
| `server_protocol.proto` | Core: register, connect, transmit, broadcast, heartbeat |
| `center2world_protocol.proto` | Center → World player session messages |
| `center2world_msg_type.proto` | Enum for center↔world IDs |
| `center2global_msg_type.proto` | Enum for center↔global IDs |
| `logic2world_protocol.proto` | Logic → World player data sync |
| `logic2world_msg_type.proto` | Enum for logic↔world IDs |
| `logic2world_friend.proto` | Logic → World friend data |
| `logic2world_robot.proto` | Logic → World robot player sync |
| `global2world_protocol.proto` | Global → World broadcast data |
| `global2world_msg_type.proto` | Enum for global↔world IDs |

### Client Protocol Files (`share/protocol/`)

| File | Contents |
|------|---------|
| `msg_type_def.proto` | Master enum of all client message IDs |
| `common_protocol.proto` | Shared types (room info, player info, item info) |
| `client2gate_protocol.proto` | Client ↔ Gate (heartbeat, session) |
| `client2center_protocol.proto` | Client → Center (login auth) |
| `client2world_protocol.proto` | Client ↔ World (login, enter game) |
| `client2world_player_property.proto` | Player stats, currency, level |
| `client2world_shop.proto` | Shop purchase/exchange |
| `client2world_friend.proto` | Friend list, add, delete |
| `client2world_mail.proto` | In-game mail |
| `client2world_chat.proto` | Chat messages |
| `client2world_rank.proto` | Leaderboard queries |
| `client2world_player_quest.proto` | Quest progress/rewards |
| `client2world_activity.proto` | Activity system (v1) |
| `client2world_activity2.proto` | Activity system (v2) |
| `client2world_exchange.proto` | Item exchange |
| `client2world_dial_lottery.proto` | Spin/dial lottery |
| `client2world_star_lottery.proto` | Star lottery |
| `client2world_daily_box_lottery.proto` | Daily box lottery |
| `client2world_online_reward.proto` | Online-time rewards |
| `client2world_safe_deposit_box.proto` | Safe deposit box |
| `client2world_benefits.proto` | VIP perks / benefits |
| `client2world_bind_phone.proto` | Phone-number binding |
| `client2world_qq_platform.proto` | QQ platform integration |
| `client2world_notice.proto` | Server broadcast notices |
| `client2world_wjlw.proto` | Special WJLW activity |
| `client2logic_protocol.proto` | Client ↔ Logic (room actions) |
| `client2logic_msg_type.proto` | Enum for client↔logic IDs |
| `fish_protocol.proto` | Core fishing game messages |
| `fish_logic.proto` | Room/game state sync |
| `fish_def.proto` | Fish type definitions |
| `fish_type_def.proto` | Extended fish type definitions |
| `fish_rank.proto` | In-game kill/score ranking |
| `fish_match.proto` | Tournament/match mode |
| `fish_match_baojin.proto` | Bao-jin match variant |
| `fish_activity.proto` | Fish-specific activities (v1) |
| `fish_activity2.proto` | Fish-specific activities (v2) |
| `fish_dragon_palace.proto` | Dragon Palace special room |
| `fish_monkey_palace.proto` | Monkey Palace special room |
| `fish_mythical_palace.proto` | Mythical Palace special room |
| `fish_legendaryfish_palace.proto` | Legendary Fish Palace |
| `fish_armedshark_palace.proto` | Armed Shark Palace |
| `fish_ticket_palace.proto` | Ticket Palace |
| `fish_bombfairyland.proto` | Bomb Fairyland mode |
| `fish_turntablefish.proto` | Turntable Fish mode |
| `pump_type.proto` | Cannon/pump type definitions |
| `msg_info_def.proto` | Message metadata |

---

## 6. Inter-Server Communication

### 6.1 Server Registration (Monitor)

```
Server boots
  └─► TCP connect to Monitor :11001
        └─► send packet_server_register {server_type, server_id, listen_port, ...}
              └─► Monitor replies packet_server_register_result {server_time, group_id}
                    └─► Monitor broadcasts packet_updata_servers_info to all servers
                              └─► Each server connects to required peers
```

### 6.2 Key Server-to-Server Messages (`server_protocol.proto`)

| Message | Direction | Purpose |
|---------|-----------|---------|
| `packet_server_register` | Any → Monitor | Register on startup |
| `packet_server_register_result` | Monitor → Any | Confirm registration |
| `packet_updata_servers_info` | Monitor → All | Full server list update |
| `packet_other_server_connect` | Monitor → All | New server joined |
| `packet_other_server_disconnect` | Monitor → All | Server dropped |
| `packet_updata_self_info` | Gate → Monitor | Update client count (load balancing) |
| `packet_transmit_msg` | Gate ↔ World/Logic | Route one message to/from a session |
| `packet_broadcast_msg` | World/Logic → Gate | Send one message to many sessions |
| `packet_broadcast_msg2` | World/Logic → Gate | Game-scoped broadcast |
| `packet_player_connect` | Gate → Center | Client WebSocket connected |
| `packet_player_disconnect` | Gate → Center | Client disconnected |
| `packet_player_has_login` | Center → Gate | Login complete, session assigned |
| `packet_player_keep_alive` | Gate → World/Logic | Heartbeat for active player |
| `packet_http_command` | Admin → Any | Runtime admin commands via HTTP |

### 6.3 Message Routing

```
Client sends game action
  Gate receives WebSocket frame
    Gate reads packet_id → determines destination server type
      Gate wraps in packet_transmit_msg {sessionid, packet_id, payload_bytes}
        Gate sends over TCP to World or Logic

World/Logic processes
  Sends packet_transmit_msg back to Gate with response
    Gate unwraps payload, writes WebSocket frame to client session
```

Broadcasts use `packet_broadcast_msg`:
```
Logic computes room state
  Logic sends packet_broadcast_msg {sessionid_list[], packet_id, payload_bytes} to Gate
    Gate fans out to each session in the list
```

### 6.4 Center ↔ World Messages (`center2world_protocol.proto`)

| Message | Purpose |
|---------|---------|
| `packetcenter2world_player_connect` | Notify World of new player session |
| `packetcenter2world_player_disconnect` | Notify World player left |
| `packetworld2center_login_result` | World confirms player login |

### 6.5 Logic ↔ World Messages (`logic2world_protocol.proto`)

| Message | Purpose |
|---------|---------|
| `packetw2l_player_login` | World sends player data to Logic on room entry |
| `packetw2l_player_logout` | Logic notified player left room |
| `packetl2w_player_login_result` | Logic confirms room entry |
| `packetl2w_update_player_data` | Logic syncs coin/score changes back to World |
| `packetl2w_game_log` | Logic sends game event logs to World for DB write |

---

## 7. Client-Server Communication

### 7.1 Connection Flow

```
1. Client HTTP POST → h5fish_web :2570  →  token + gate server address
2. Client WebSocket connect → Gate :15001
3. Gate creates sessionid, sends packet_player_connect → Center
4. Client sends packetc2w_ask_login (Gate routes → World via Center)
5. World verifies account in MongoDB, replies packet_w2c_login_result
6. Client enters game room → Logic handles real-time gameplay
```

### 7.2 Client Message Routing

| Packet prefix | Routed to | Example |
|---------------|-----------|---------|
| `c2g_` | Gate (handled locally) | `packetc2g_heartbeat` |
| `c2center_` | Center | auth messages |
| `c2w_` / `c2world_` | World (Gate → World) | login, shop, quest |
| `c2l_` / `c2logic_` | Logic (Gate → Logic) | shoot bullet, enter room |

### 7.3 Client Heartbeat

```
packetc2g_heartbeat  (client2gate_protocol.proto)
  Client sends every N seconds
    Gate updates last-seen time
      Gate forwards player_keep_alive to World/Logic
```

Connections with no heartbeat are dropped after a configurable `check_time`.

### 7.4 WebSocket Details

- Gate uses `WsPeer` (`share/common/ws/`) — standard RFC 6455
- **Binary frames only** (opcode `0x02`)
- Each frame payload = **encrypted 12-byte header + raw Protobuf bytes** (see §4.2–4.3)
- No sub-protocol negotiation required

---

## 8. Redis Cache Layer

Library: **hiredis** (`share/common/hiredis/hiredis.h`), wrapped by `CRedisClient` (`server/sshare/redis_base.h`).

### 8.1 Redis Instances

| CacheId | Port | Password | Purpose |
|---------|------|----------|---------|
| 1 | **6159** | `qwer1234` | Grand Prix (大奖赛) leaderboard |
| 2 | **6158** | `qwer1234` | Global shared data (全局数据) |
| 3 | **6157** | `qwer1234` | Ranked match / activity rankings (排位赛) |
| 4 | **6156** | `qwer1234` | Cross-server task synchronisation (任务同步) |

All four instances run on `127.0.0.1` by default.

### 8.2 Which Servers Use Redis

| Server | CacheIds Used |
|--------|---------------|
| Logic | 1, 2, 3, 4 |
| World | 1, 2, 3, 4 |
| Global | 2 |
| Gate / Center / Monitor | — |

### 8.3 Cache Classes (`server/sshare/game_cache.h`)

| Class | CacheId | What is stored |
|-------|---------|----------------|
| `grandprix_cache` | 1 | Tournament scores, bracket data |
| `global_cache` | 2 | Airdrop state, global event flags |
| `rank_cache` | 3 | Kill/score rankings, activity rankings |
| `task_cache` | 4 | Daily/weekly task progress synced across servers |

### 8.4 Redis Configuration File

`bin/Debug/Xml/Server_CacheListCFG.xml`

```xml
<Data CacheId="1" CacheType="1" Url="127.0.0.1" Port="6159" Pwd="qwer1234" Desc="Grand Prix ranking" />
<Data CacheId="2" CacheType="2" Url="127.0.0.1" Port="6158" Pwd="qwer1234" Desc="Global data" />
<Data CacheId="3" CacheType="3" Url="127.0.0.1" Port="6157" Pwd="qwer1234" Desc="Ranked match / activity rankings" />
<Data CacheId="4" CacheType="4" Url="127.0.0.1" Port="6156" Pwd="qwer1234" Desc="Task sync" />
```

---

## 9. MongoDB Data Layer

Driver: **mongocxx** (vcpkg `x64-windows-v141`). The public server API uses legacy `mongo::BSONObj` types (`share/dependencies/mongodb/`); the actual wire protocol is handled by mongocxx internally via pimpl in `server/sshare/db_base.cpp`.

All databases run on `127.0.0.1:27017`.

### 9.1 Database Map

| DbId | DbType | Database Name | Contents |
|------|--------|---------------|----------|
| 1 | 1 | `PlayerDB_DWC5` | Player profiles, currency, inventory, activities |
| 2 | 4 | `PaymentDB5` | Payment orders, receipts, recharge history |
| 3 | 3 | `LogDB_DWC5` | Game event logs, audit trail |
| 4 | 2 | `GameDB5` | Room state, game sessions, match results |
| 5 | 5 | `AccountDB5` | Account authentication, channel info |
| 6 | 6 | `ConfigDB5` | Server config, room config, fish config |
| 7 | 7 | `GlobalDB` | Cross-server airdrop data, global rankings |

### 9.2 Server → Database Matrix

| Server | DbIds |
|--------|-------|
| Monitor | — |
| Center | 1 |
| Gate | 6 |
| World | 1, 2, 3, 4, 5, 6 |
| Logic | 3, 4, 6 |
| Global | 3, 5, 6, 7 |

### 9.3 MongoDB Configuration File

`bin/Debug/Xml/Server_DBListCFG.xml`

```xml
<Data DbId="1" DbType="1" DbName="PlayerDB_DWC5" DbURL="127.0.0.1:27017" />
<Data DbId="2" DbType="4" DbName="PaymentDB5"    DbURL="127.0.0.1:27017" />
<Data DbId="3" DbType="3" DbName="LogDB_DWC5"    DbURL="127.0.0.1:27017" />
<Data DbId="4" DbType="2" DbName="GameDB5"        DbURL="127.0.0.1:27017" />
<Data DbId="5" DbType="5" DbName="AccountDB5"     DbURL="127.0.0.1:27017" />
<Data DbId="6" DbType="6" DbName="ConfigDB5"      DbURL="127.0.0.1:27017" />
<Data DbId="7" DbType="7" DbName="GlobalDB"       DbURL="127.0.0.1:27017" />
```

---

## 10. Startup Sequence

Config: `bin/Debug/Xml/Server_ServerListCFG.xml`  
Script: `bin/Debug/start all.bat`

```bat
start "monitor"       monitor.exe  --serverId 65537
start "center"        center.exe   --serverId 65536
start "global"        global.exe   --serverId 70000
start "world-66000"   world.exe    --serverId 66000

ping 127.1 /n 2 >nul        :: ~2 s — wait for world to be ready

start "logic-1000000" logic.exe   --serverId 1000000

ping 127.1 /n 3 >nul        :: ~3 s

start "gate-1"        gate.exe    --serverId 1
```

**Order matters**: Monitor must be up before anything else. Gate starts last because clients may connect immediately after it binds.

### External Services Required First

| Service | Default Port | Notes |
|---------|-------------|-------|
| MongoDB | 27017 | Must have all 7 databases initialised |
| Redis #1 (grand prix) | 6159 | Before Logic / World |
| Redis #2 (global) | 6158 | Before Global / World |
| Redis #3 (rankings) | 6157 | Before Logic / World |
| Redis #4 (tasks) | 6156 | Before Logic / World |

---

## 11. Setting Up a New Game Server

### 11.1 Required Dependencies

| Package | Version | Source | Used for |
|---------|---------|--------|---------|
| **Boost** | 1.90 (DLL) | vcpkg `x64-windows-v141` | ASIO, Thread, Log, Filesystem, Timer |
| **Protobuf** | 3.21 | vcpkg `x64-windows-v141` | Protocol serialization |
| **mongocxx / bsoncxx** | latest | vcpkg `x64-windows-v141` | MongoDB C++ driver |
| **hiredis** | bundled | `share/common/hiredis/` | Redis client |
| **log4cpp** | bundled | vcpkg `x64-windows-v141` | Server logging |
| **Lua** | 5.3.5 | vcpkg `x64-windows-v141` | Game scripting (Logic only) |
| **CryptoPP** | — | vcpkg `x64-windows-v141` | Crypto utilities |
| **MongoDB** | 6+ | OS install | Database server |
| **Redis** | 6+ | OS install | 4× instances on 6156–6159 |

### 11.2 New Server Config Entry

Add a row to `bin/*/Xml/Server_ServerListCFG.xml`:

```xml
<Data
  ServerId="UNIQUE_ID"
  ServerName="myserver"
  ServerIP="127.0.0.1"
  ListenPort="PORT"
  MonitorIP=""    MonitorPort=""
  DbIdList="3,6"
  CacheList="1,2"
  HttpCheck=""
  GameVer="1.0.0"
  GameDLL="games/game_fishlord.dll"
  IsDefault=""
  Channel=""
/>
```

**ServerId ranges**:
| Range | Server type |
|-------|------------|
| 1–99 | Gate |
| 65536 | Center |
| 65537 | Monitor |
| 66000–66999 | World |
| 70000 | Global |
| 1000000+ | Logic |

### 11.3 Minimum Inter-Server Connections for a New Logic Server

1. Logic connects **to Monitor** on startup → registers
2. Monitor sends server list → Logic connects to:
   - **Center** — player session events
   - **World** — player data sync (`packetw2l_*` / `packetl2w_*`)
   - **Gate(s)** — `packet_transmit_msg` / `packet_broadcast_msg`
3. Logic does **not** connect to other Logic servers

### 11.4 Adding a New Message Type

1. Add enum value to `share/protocol/msg_type_def.proto` (client) or `server/protocol/server_msg_type.proto` (server)
2. Add message definition to the appropriate `.proto` file
3. Run `protoc.exe` to regenerate `.pb.h` / `.pb.cc`
4. Register a handler in the receiving server's `packet_service()` dispatch
5. On the client side (TypeScript): add handler in `assets/Script/Net/`
6. Use `packet_transmit_msg` to forward the message across server boundaries

### 11.5 Packet Encryption Checklist (new client implementation)

- Encode the outgoing 12-byte header with the `packet_head_c` algorithm (§4.2)
- Decode the 12-byte response header with the reverse algorithm (§4.2)
- XOR-decrypt the payload using bytes 0–3 of the decoded header as a rolling 4-byte key (§4.3)
- Server validates `head_mark` == `{'$','3','&','@'}` — packet is silently dropped otherwise
- Server-to-server traffic uses `packet_head_s` — **no encryption needed**

### 11.6 Redis Startup (Windows example)

```bat
start redis-server --port 6159 --requirepass qwer1234
start redis-server --port 6158 --requirepass qwer1234
start redis-server --port 6157 --requirepass qwer1234
start redis-server --port 6156 --requirepass qwer1234
```

### 11.7 MongoDB Initialisation

After all services are running, POST to the init endpoint once to seed the databases:

```
http://localhost:2570/InitDB.aspx
```

This creates the test account `a123456789`, channel config, and the default server list entry.
