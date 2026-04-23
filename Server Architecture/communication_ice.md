# Ice MOBA — Communication Architecture

## Overview

Ice MOBA uses a three-layer communication model:

```
Unity Client
    │
    ├── TCP (ACE framework)
    │       ↓
    ├── Connect Server (port 13601) — assigns a Gate
    │
    └── Gate Server (port 13701) — session hub for all lobby messages
            │
            └── ZeroMQ PUSH/PULL (protobuf-serialized RPC)
                    ↓
            Hall Server (port 3901) — central game logic
                    │
                    ├── Room Center (port 4101) — battle allocation
                    │       └── Room Master (port 4201) — room process lifecycle
                    │               └── Battle Room processes (dynamically spawned)
                    │                       ├── ZMQ RPC port: 10000, 10002 … 19998
                    │                       ├── RakNet UDP:   20000, 20002 … (client game ticks)
                    │                       └── TCP:          20001, 20003 … (client reliable events)
                    │
                    ├── Team (4701), Social (4901), Rank (5001), Name (4801)
                    │
                    └── Center (4001) — chat horn, recharge, libao relay
                                │
                                ├── Login Pipe  (4401, Python/Tornado) → DB
                                ├── Libao Pipe  (4501, Python/Tornado) → DB
                                ├── Recharge Pipe (4601, Python/Tornado) → DB
                                └── Back Pipe   (4301, Python/Tornado) → DB
```

---

## Port Reference

| Server | Bind Port (ZMQ RPC) | Accept Port (TCP clients) | Language |
|---|---|---|---|
| Connect | 3601 | **13601** | C++ |
| Login | 3801 | — | C++ |
| Gate | 3701 | **13701** | C++ |
| Hall | 3901 | — | C++ |
| Center | 4001 | — | C++ |
| Room Center | 4101 | — | C++ |
| Room Master | 4201 | — | C++ |
| Back Pipe | 4301 | — | Python |
| Login Pipe | 4401 | — | Python |
| Libao Pipe | 4501 | — | Python |
| Recharge Pipe | 4601 | — | Python |
| Team | 4701 | — | C++ |
| Name | 4801 | — | C++ |
| Social | 4901 | — | C++ |
| Rank | 5001 | — | C++ |
| Battle Room (room.exe) | ZMQ: 10000–19998 (even) | UDP: 20000–29998 / TCP: 20001–29999 | C++ |

All servers bind to `127.0.0.1` by default (configured in `server/conf/server.json`).

---

## Transport Layers

### 1. Client ↔ Server: TCP (ACE Framework)

**Library**: ACE 6.2.7 (`ACE_SOCK_ACCEPTOR`, reactor pattern)  
**Ports**: 13601 (Connect), 13701 (Gate)  
**Source**: `server/src/libservice/tcp_service.{h,cpp}`

**Packet format**:
```
[ 2-byte opcode (little-endian) ][ N-byte protobuf payload ]
```
- Max packet: 8 KB
- Receive buffer per handler: 32 KB
- Client-facing messages are prefixed `CMSG_` (client→server) and `SMSG_` (server→client)

**Opcode ranges** (defined in `server/src/libprotocol/opcodes.h`):

| Range | Prefix | Type |
|---|---|---|
| 0–9 999 | `BEG_SELF_MESSAGE` | Internal server-to-self |
| 10 000–19 999 | `PUSH_*` | One-way push (fire-and-forget RPC) |
| 20 000–29 999 | `REQ_*` / `REP_*` | RPC request / response |
| 30 000+ | `CMSG_*` / `SMSG_*` | Client TCP messages |

### 2. Server ↔ Server: ZeroMQ PUSH/PULL + Protobuf

**Library**: ZeroMQ 4.0.4  
**Serialization**: Protocol Buffers 3.6.1  
**Source**: `server/src/libservice/rpc_service.{h,cpp}`

**Socket pattern**:
- `RpcServer` binds a **ZMQ_PULL** socket — receives all inbound RPC messages
- `RpcClient` connects with a **ZMQ_PUSH** socket — sends to a target server

**RPC envelope** (`common/protocol/rpc.proto`):
```protobuf
enum erpc {
    REQUESST = 0;   // request (has id, expects RESPONSE)
    PUSH     = 1;   // fire-and-forget
    RESPONSE = 2;   // reply to a REQUESST (echoes same id)
}

message request  { string name = 1; uint64 id = 2; bytes msg = 3; }
message response { string name = 1; uint64 id = 2; bytes msg = 3; int32 error_code = 4; }
message rpc      { erpc type = 1; request req = 2; response rep = 3; }
```

**Request/response flow**:
```
Sender
  → allocates request_id (auto-increment)
  → stores callback in response_func_map_[id]
  → PUSH { type=REQUESST, req={name, id, payload} }
  ↓
Receiver's PULL socket
  → dispatches to handler by opcode in payload
  → handler calls send_rep_*(name, id, result)
  ↓
Sender's PULL socket
  → receives { type=RESPONSE, rep={id, payload} }
  → calls response_func_map_[id](packet, error_code, ...)
```

### 3. Client ↔ Battle Room: UDP + TCP (RakNet + Protobuf)

**UDP Library**: RakNet (master branch) — used on **both** server and client  
**TCP Library**: ACE 6.2.7 (same as lobby TCP)  
**Serialization**: Protocol Buffers 3.6.1 — **same TPacket format as TCP**, carried inside RakNet BitStream

**Packet format inside RakNet BitStream**:
```
[ 1-byte RakNet header: ID_USER_PACKET_ENUM ][ 2-byte opcode ][ N-byte protobuf payload ]
                                              |_____ TPacket (same as TCP) ______________|
```
RakNet is only the transport envelope. The actual payload is identical to lobby TCP: a `TPacket` with a 2-byte opcode followed by a serialized protobuf message. UDP chunks are capped at 500 bytes per send call (`udp_handler.cpp:35`).

**Protobuf messages used over UDP** (`common/protocol/msg_battle1.proto`):
| Message | Direction | Description |
|---|---|---|
| `cmsg_battle_link` | Client → Room | Join battle room request |
| `smsg_battle_link` | Room → Client | Battle state sync on connect (full snapshot) |
| `cmsg_battle_state` | Client → Room | Client game-tick input (position, actions) |
| `msg_battle_state` | Room → Client | Server authoritative game state broadcast |
| `msg_battle_op` | Room → Client | Individual battle operation (skill, move, hit) |
| `msg_battle_player` | Room → Client | Player entity state |
| `msg_battle_boss` | Room → Client | Boss entity state |
| `msg_battle_monster` | Room → Client | Monster entity state |
| `msg_battle_effect` | Room → Client | Active effect/buff state |

**Server-side** (`server/src/libservice/udp_service.{h,cpp}`, `udp_handler.cpp`):
- `RakNet::RakPeerInterface` — peer object, binds UDP socket
- `RakNet::SocketDescriptor` — describes the UDP port to bind
- `RakNet::BitStream` — wraps the TPacket bytes for transport
- `RakNet::Packet` — received packet, unwrapped back to TPacket → protobuf
- Linked as: `libRakNetLibStatic.a` (Linux) / `RakNet.lib` (Windows)

**Client-side** (`client/Assets/Scripts/FrameSub/UdpSocketClient.cs`):
- Same RakNet API via C# wrapper DLL: `client/Assets/Plugins/x86_64/RakNet.dll`
- `RakNet.RakPeerInterface.GetInstance()` — create peer
- `RakNet.SocketDescriptor` — local socket (port 0 = OS-assigned)
- `RakNet.BitStream` — wraps the serialized protobuf + opcode bytes
- `RakNet.PacketPriority.HIGH_PRIORITY` + `RakNet.PacketReliability.RELIABLE_ORDERED` — send mode

**Client send example** (from `UdpSocketClient.cs:91–94`):
```csharp
// buffer1 = [ 2-byte opcode ][ protobuf bytes ] (TPacket format)
RakNet.BitStream bs = new RakNet.BitStream();
bs.Write((byte)RakNet.DefaultMessageIDTypes.ID_USER_PACKET_ENUM);
bs.Write(buffer1, (uint)buffer1.Length);  // protobuf payload inside
client.Send(bs, PacketPriority.HIGH_PRIORITY, PacketReliability.RELIABLE_ORDERED, (char)0, m_addr, false);
```

Each `room.exe` process is spawned by Room Master with three dynamically assigned ports:

| Port formula | Example | Used for |
|---|---|---|
| `base` (even, 10000–19998) | 10000 | ZMQ RPC: Room Master → Room process |
| `base + 10000` | 20000 | **RakNet UDP**: client real-time game data (positions, skills, snowballs) |
| `base + 10001` | 20001 | **ACE TCP**: client reliable events (`CMSG_ENTER_WORLD_UDP`, `CMSG_LEAVE_WORLD_UDP`) |

The client connects to both the UDP and TCP ports simultaneously during a battle. The lobby Gate TCP connection is paused (not dropped) while in battle.

### 4. Python Sub-Servers ↔ Database: PyMySQL via Tornado

**Framework**: Tornado 4.4.3 (async HTTP server)  
**DB driver**: PyMySQL  
**Receive socket**: ZMQ PULL (each pipe server binds its own port above)

Each Python pipe server exposes an HTTP endpoint and listens for work via ZMQ PUSH from C++ servers.

---

## Server Roles & Communication

### Connect Server (C++, port 3601 / TCP 13601)

**Role**: Entry point and load balancer — every client connects here first.

**Client flow**:
1. Client opens TCP to `13601`
2. Sends `CMSG_REQUEST_GATE` (opcode ~30100)
3. Connect checks registered Gate servers, picks the one with fewest players
4. Replies `SMSG_REQUEST_GATE` with the Gate's `ip:port`
5. Client disconnects from Connect and reconnects to Gate

**Upstream**:
- Receives periodic `PUSH_GATE_CONNECT_PLAYER_NUM` (ZMQ PUSH) from each Gate every 30 s
- Uses this count for load balancing

**Relevant proto**: `common/protocol/msg_connect.proto`

---

### Gate Server (C++, port 3701 / TCP 13701)

**Role**: Per-session hub. All non-battle lobby messages flow through Gate.

**Responsibilities**:
- Maintain `hid → player_guid` and `guid → account` maps
- Forward `CMSG_*` messages from clients to Hall via ZMQ
- Forward `SMSG_*` push messages from Hall/Center back to the right client
- Keepalive heartbeats (kick idle clients after ~10 s)

**Upstream**:
- → Hall: `PUSH_GATE_HALL_LOGIN`, `PUSH_GATE_HALL_*` (player actions)
- ← Hall: `PUSH_HALL_GATE_*` (results pushed back to client)
- → Login: `REQ_GATE_LOGIN_*` (authentication)
- ← Connect: none (Gate pushes player count to Connect)
- → Connect: `PUSH_GATE_CONNECT_PLAYER_NUM`

**Relevant proto**: `common/protocol/msg_gate.proto`

---

### Login Server (C++, port 3801)

**Role**: Thin authentication relay between Gate and Login Pipe.

**Flow**:
```
Gate → REQ_GATE_LOGIN_* (ZMQ) → Login Server → PUSH (ZMQ) → Login Pipe (Python)
Login Pipe → HTTP POST /login → external auth or DB → response
Login Pipe → REP (ZMQ) → Login Server → REP → Gate → SMSG_LOGIN_PLAYER → Client
```

**Relevant proto**: `common/protocol/msg_login.proto`

---

### Hall Server (C++, port 3901)

**Role**: Central game logic. The busiest server — almost every player action passes through it.

**Features**: Player profile loading/saving, inventory, role selection, battle matchmaking requests, chat, daily tasks, mail (post), leaderboard queries.

**Upstream (ZMQ PUSH/REQ to)**:

| Target | Messages | Pattern |
|---|---|---|
| Gate | `PUSH_HALL_GATE_*` — relay results to client | PUSH |
| Room Center | `REQ_HALL_RC_SINGLE_BATTLE` — request new battle | REQ |
| Room Center | `REQ_HALL_RC_HAS_BATTLE` — check if player is in battle | REQ |
| Center | `REQ_HALL_CENTER_PLAYER_LOOK` — fetch another player's data | REQ |
| Center | `REQ_HALL_CENTER_LIBAO` — redeem gift code | REQ |
| Center | `REQ_HALL_CENTER_RECHARGE` — validate payment | REQ |
| Center | `PUSH_HALL_CENTER_CHAT_HORN` — broadcast chat horn | PUSH |
| Team | `PUSH_HALL_TEAM_*` — team join/leave events | PUSH |
| Social | `PUSH_HALL_SOCIAL_FIGHT` — fight notification | PUSH |

**Downstream (receives ZMQ PUSH from)**:
- Room Center/Master: `PUSH_RC_HALL_BATTLE_END` — battle results & rewards

**Relevant proto**: `common/protocol/msg_hall.proto`

---

### Center Server (C++, port 4001)

**Role**: Broadcast hub and relay for cross-server events and external pipe servers.

**Responsibilities**:
- Receive chat horn pushes from Hall; fan-out to all Gates
- Relay recharge and gift-code requests to Recharge/Libao Python pipes
- Cache player data for cross-player lookups

**Upstream**:
- → Gates: `PUSH_CENTER_GATE_CHAT_HORN` (fan-out to all)
- → Recharge Pipe (4601): ZMQ PUSH validation request
- → Libao Pipe (4501): ZMQ PUSH gift code validation

---

### Room Center (C++, port 4101)

**Role**: Battle queue manager — receives match requests, allocates them to a Room Master.

**Flow**:
```
Hall → REQ_HALL_RC_SINGLE_BATTLE ──→ Room Center
                                        │
                                        ├─ picks least-loaded Room Master
                                        └─→ REQ_RC_RM_CREATE_ROOM → Room Master
                                                    │
                                                    └─ REP: battle_guid + UDP port
                                        │
                                        └─→ REP back to Hall (room endpoints)
```

**Tracks**: `battle_guid → RoomInfo { master, UDP port, team composition }` for all active battles.

**Relevant proto**: `common/protocol/msg_room.proto`

---

### Room Master (C++, port 4201)

**Role**: Manages battle room processes — spawns, monitors, and cleans up individual room processes.

**Flow**:
```
Room Center → REQ_RC_RM_CREATE_ROOM → Room Master
Room Master → spawns battle room child process (dynamic UDP port 5000+)
Battle ends → Room Master → PUSH_RM_RC_BATTLE_END → Room Center
           → Room Master → PUSH_RC_HALL_BATTLE_END → Hall (rewards)
```

**Relevant proto**: `common/protocol/msg_room.proto`

---

### Battle Room Processes (C++, UDP dynamic ports)

**Role**: Real-time game simulation for one match. Spawned as a child process by Room Master (`ACE_Process_Manager::spawn`).

**Ports** (per room, assigned from Room Master's pool):
- ZMQ base port (10000–19998, even steps) — Room Master monitors/communicates with room
- **UDP base+10000** — RakNet socket; clients send game ticks (positions, skill casts, snowball physics) here
- **TCP base+10001** — ACE TCP socket; clients send reliable events (`CMSG_ENTER_WORLD_UDP`, `CMSG_LEAVE_WORLD_UDP`)

**Lifecycle**: Max runtime = 8 min 15 s (`ROOM_TIME`), or until all players leave. On exit, signals Room Master via ZMQ which notifies Room Center and Hall.

**Relevant proto**: `common/protocol/msg_battle.proto`

---

### Team Server (C++, port 4701)

**Role**: Team/party management — creating, joining, and leaving teams before battle.

**Communicates with**:
- Hall (receives PUSH_HALL_TEAM_*, replies via PUSH back to Hall/Gate)

---

### Name Server (C++, port 4801)

**Role**: Name uniqueness registry — players register names here; Hall queries on character creation.

---

### Social Server (C++, port 4901)

**Role**: Friend list, follow/unfollow, and fight notification.

**DB access**: MySQL via MySQL++ (reads/writes `t_social` table).

**Upstream**:
- Hall → `PUSH_HALL_SOCIAL_FIGHT` (when two friends match up)
- Social → `REQ_SOCIAL_*` internally for graph queries (opcodes 20100–20108)

---

### Rank Server (C++, port 5001)

**Role**: Leaderboard — stores and serves ranked player scores.

**DB access**: MySQL via MySQL++ (reads/writes `t_rank` table).

**Upstream**:
- Hall → queries after battle end
- Rank → Hall/Gate → client via PUSH chain

---

### Python Pipe Sub-Servers

All Python servers use **Tornado** async HTTP + **ZMQ PULL** to receive work from C++ servers.

| Server | Port | ZMQ Socket | DB Table | Responsibility |
|---|---|---|---|---|
| Login Pipe | 4401 | PULL | `user` (snowball_user DB) | Authenticate user credentials |
| Libao Pipe | 4501 | PULL | `t_libao` | Validate & consume gift codes |
| Recharge Pipe | 4601 | PULL | `t_recharge` | Validate payment receipts (Apple/Google/AliPay) |
| Back Pipe | 4301 | PULL | various | Background tasks (maintenance) |

**Login Pipe detail**:
```python
# server/sub_server/login_pipe/login_pipe.py
zmq_socket = ctx.socket(zmq.PULL)
zmq_socket.bind("tcp://127.0.0.1:4401")

# On message: verify user in MySQL
db = pymysql.connect(user='root', passwd='password123', db='snowball_user', host='127.0.0.1')
# Responds via ZMQ PUSH back to Login server
```

---

## Full Message Flow Examples

### Login Flow

```
Client ──TCP──→ Connect (13601): CMSG_REQUEST_GATE
Connect ──TCP──→ Client: SMSG_REQUEST_GATE { gate_ip, gate_port=13701 }

Client ──TCP──→ Gate (13701): CMSG_LOGIN { account, password, token }
Gate ──ZMQ PUSH──→ Login: PUSH_GATE_LOGIN_* { account, password }
Login ──ZMQ PUSH──→ Login Pipe: { account, password }
Login Pipe ──pymysql──→ MySQL snowball_user.user (SELECT)
Login Pipe ──ZMQ PUSH──→ Login: result
Login ──ZMQ PUSH──→ Gate: result
Gate ──TCP──→ Client: SMSG_LOGIN_PLAYER { player_data }
```

### Matchmaking & Battle Start Flow

```
Client ──TCP──→ Gate: CMSG_HALL_SINGLE_BATTLE
Gate ──ZMQ PUSH──→ Hall: PUSH_GATE_HALL_SINGLE_BATTLE { player_guid }
Hall ──ZMQ REQ──→ Room Center: REQ_HALL_RC_SINGLE_BATTLE { guid, role, mode }
Room Center ──ZMQ REQ──→ Room Master: REQ_RC_RM_CREATE_ROOM { battle_guid, type }
Room Master spawns battle room child process → binds UDP port (e.g. 5047)
Room Master ──ZMQ REP──→ Room Center: { battle_guid, udp_port=5047 }
Room Center ──ZMQ REP──→ Hall: { battle_guid, udp_port=5047 }
Hall ──ZMQ PUSH──→ Gate: PUSH_HALL_GATE_SINGLE_BATTLE_REP { battle_guid, udp_port }
Gate ──TCP──→ Client: SMSG_SINGLE_BATTLE { udp_port=5047 }

Client ──UDP (RakNet)──→ Battle Room (:5047): game packets
```

### Battle End Flow

```
Battle Room exits / sends end signal
Room Master ──ZMQ PUSH──→ Room Center: PUSH_RM_RC_BATTLE_END { battle_guid, results }
Room Master ──ZMQ PUSH──→ Hall: PUSH_RC_HALL_BATTLE_END { player_guids, rewards }
Hall updates player stats → MySQL
Hall ──ZMQ PUSH──→ Gate: PUSH_HALL_GATE_BATTLE_END
Gate ──TCP──→ Client: SMSG_BATTLE_END { rewards }
Hall ──ZMQ PUSH──→ Rank: update scores
```

---

## Protocol Buffer Files

All `.proto` files live in `common/protocol/`. Generated code:
- C++: `common/protocpp/*.pb.{h,cc}` (`protoc_cpp.bat`)
- C#: Unity client (`protoc_csharp.bat`)
- Lua: client scripting (`protoc_lua.bat`)
- Python: pipe sub-servers (`protoc_py.bat`)

| File | Used by |
|---|---|
| `rpc.proto` | All C++ servers (RPC envelope) |
| `msg_connect.proto` | Connect ↔ Gate ↔ Client |
| `msg_gate.proto` | Gate ↔ Hall |
| `msg_login.proto` | Gate ↔ Login ↔ Login Pipe |
| `msg_hall.proto` | Hall ↔ Gate ↔ all sub-servers |
| `msg_battle.proto` | Battle Room ↔ Client — player info, join packet |
| `msg_battle1.proto` | Battle Room ↔ Client (UDP) — game state, ops, entities (`msg_battle_op`, `smsg_battle_link`, `msg_battle_state`) |
| `msg_room.proto` | Hall ↔ Room Center ↔ Room Master |
| `msg_team.proto` | Hall ↔ Team |
| `msg_social.proto` | Hall ↔ Social |
| `msg_rank.proto` | Hall ↔ Rank |
| `msg_center.proto` | Hall ↔ Center ↔ Pipes |
| `player.proto` | Shared player model |
| `role.proto` | Shared hero/role model |
| `post.proto` / `post_new.proto` | Mail system |

---

## Key Libraries Summary

| Layer | Library | Version | Protocol |
|---|---|---|---|
| Client TCP (lobby) | ACE | 6.2.7 | Raw TCP stream + 2-byte opcode header |
| Client UDP (battle, game ticks) | RakNet | master | `RakPeerInterface` + `BitStream`; `RakNet.dll` on Unity side |
| Client TCP (battle, reliable) | ACE | 6.2.7 | TCP stream for `CMSG_ENTER/LEAVE_WORLD_UDP` confirmations |
| Inter-server messaging | ZeroMQ | 4.0.4 | PUSH/PULL socket pairs |
| Serialization | Protocol Buffers | 3.6.1 | Binary protobuf |
| Python async | Tornado | 4.4.3 | Async HTTP + ZMQ |
| DB (C++) | MySQL++ | 3.2.1 | MySQL wire protocol |
| DB (Python) | PyMySQL | latest | MySQL wire protocol |
| DB server | MySQL | 5.x | — |

---

## Source File Index

| What | Where |
|---|---|
| RPC engine | `server/src/libservice/rpc_service.{h,cpp}` |
| TCP engine | `server/src/libservice/tcp_service.{h,cpp}` |
| UDP engine (server) | `server/src/libservice/udp_service.{h,cpp}`, `udp_handler.{h,cpp}` |
| UDP client (Unity) | `client/Assets/Scripts/FrameSub/UdpSocketClient.cs` |
| RakNet DLL (Unity) | `client/Assets/Plugins/x86_64/RakNet.dll` |
| Opcodes | `server/src/libprotocol/opcodes.h` |
| Proto definitions | `common/protocol/*.proto` |
| Server config (ports) | `server/conf/server.json` |
| Login Pipe | `server/sub_server/login_pipe/login_pipe.py` |
| Libao Pipe | `server/sub_server/libao_pipe/libao_pipe.py` |
| Recharge Pipe | `server/sub_server/recharge_pipe/recharge_pipe.py` |
| Unity networking | `Assets/Scripts/Framework/NetworkManager.cs` |
| Lua network layer | `Assets/Lua/Net/` |
