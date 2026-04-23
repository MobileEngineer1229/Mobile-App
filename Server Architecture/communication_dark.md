# Dark Lord Server Communication Architecture

## Technology At a Glance

| Technology | Used? | Details |
|------------|-------|---------|
| **TCP** | ✅ YES | Used exclusively for all communication — client↔server and server↔server |
| **UDP** | ❌ NO | Not used anywhere in the codebase |
| **Protobuf** | ❌ NO | Replaced by hand-written `#pragma pack(1)` C structs cast directly to byte buffers |
| **LZ4** | ✅ YES | Active compression for server→client packets (threshold: 256 bytes) |
| **ZLib** | ⚠️ LINKED | Compiled in but game packet compression calls are commented out — inactive |
| **Redis** | ⚠️ DISABLED | Libraries present (`hiredis`, `x_redis`) but all code commented out |
| **JSON** | ✅ YES | jsoncpp — used for HTTP bodies and config files only, not game packets |
| **Lua** | ✅ YES | Lua 5.3 server-side scripting (scene/world logic), XLua on Unity client |
| **Unix Domain Socket** | ✅ YES (Linux) | Inter-server transport on Linux (faster than TCP loopback) |
| **libevent** | ✅ YES | Async event loop for all inter-server connections (epoll on Linux) |
| **libcurl** | ✅ YES | Outbound HTTP for platform auth and payment verification |
| **MySQL++** | ✅ YES | MySQL ORM used exclusively by datasrv |
| **XOR cipher** | ✅ YES | Applied after LZ4 compression on all external (client-facing) packets |

> **Key point:** There is no protobuf, no flatbuffers, no msgpack. The wire format is raw binary C structs with a custom 16-byte header. TCP is the only transport — no UDP anywhere.

---

## Table of Contents
1. [Overview](#overview)
2. [Transport Layer: TCP vs UDP](#transport-layer)
3. [Packet Format (Client ↔ Server)](#packet-format)
4. [Compression & Encryption](#compression--encryption)
5. [Inter-Server Communication (RPC)](#inter-server-communication)
6. [Server Ports & Roles](#server-ports--roles)
7. [Client Login Flow](#client-login-flow)
8. [Message Routing (connexsrv)](#message-routing)
9. [HTTP Services](#http-services)
10. [Redis](#redis)
11. [Library Dependency Summary](#library-dependency-summary)
12. [Setting Up a New Server](#setting-up-a-new-server)

---

## Overview

```
[Game Client]
     │  TCP:8200 (binary packets, LZ4+XOR)
     ▼
[connsrv / connexsrv]   ← sole external-facing server
     │  Unix Domain Socket (Linux) / TCP (Windows)
     │  Custom binary RPC packets
     ├──► [loginsrv  :7200]  ← auth, account creation
     ├──► [worldsrv  :9200]  ← world logic, rankings, groups
     ├──► [scenesrv  :6200]  ← game zones, combat, monsters
     ├──► [crosssrv  :10001] ← cross-server features
     └──► [bridgesrv]         ← gateway bridging
              │
              ▼
         [datasrv :5200]     ← MySQL proxy (only server that touches DB)
              │
              ▼
         [MySQL   :3306]     ← ahlz database
              │
         [logsrv  :10201]   ← logging (all servers write here)
```

All traffic between servers is **custom binary RPC over TCP** (or Unix Domain Sockets on Linux). There is **no UDP anywhere** in the codebase.

---

## Transport Layer

### Client ↔ Server: TCP only

- Protocol: **TCP** exclusively. No UDP is used anywhere.
- Client connects to **connsrv** (or **connexsrv**) on port **8200**.
- Socket options applied: `SO_KEEPALIVE`, `TCP_NODELAY` (Nagle disabled), `SO_LINGER`, non-blocking.
- Client-facing I/O uses raw POSIX sockets with a custom reactor thread model (`TitanSocketThread`, `TitanListenThread`), **not** libevent.
- Thread count for client connections: **4** (configured via `<thread>4</thread>` in `srvconf.xml`).

### Server ↔ Server: TCP or Unix Domain Socket

- All inter-server communication uses **libevent 2.0.21** (`TitanEventSocket`, `TitanEventTcpServer`, `TitanEventTcpClient`).
- On **Linux**: `AF_UNIX` Unix Domain Sockets (enabled by `-D_UNIX_DOMAIN_SOCKET` compile flag in every server's `CMakeLists.txt`). This avoids network stack overhead for same-host communication.
- On **Windows**: regular TCP is used instead (the compile flag is absent on Windows builds).
- Reactor backend: **epoll** on Linux (`SocketReactorImplEPOLL`), **IOCP** on Windows (`SocketReactorImplIocp`).

---

## Packet Format

Defined in `Server/common/include/TitanEventPacket.h` and `TitanSocketCommon.h`. All structs use `#pragma pack(1)` (no padding).

### Client Packet (`client_packet` / `EventClientPacket`)

| Offset | Field | Type | Description |
|--------|-------|------|-------------|
| 0 | `m_i2Begin` | `short` (2B) | Magic number: `0x52FA` |
| 2 | `m_i2DataSize` | `int` (4B) | Body length (bytes) |
| 6 | `m_i1RandSeed` | `char` (1B) | Random seed used in checksum |
| 7 | `m_i1CheckSum` | `char` (1B) | XOR checksum over body |
| 8 | `m_i4Compress` | `int` (4B) | Upper 8 bits = compress type; lower 24 bits = original size |
| 12 | `m_i2PacketID` | `short` (2B) | Message type ID |
| 14 | `m_i2Index` | `short` (2B) | Sequence number (1–9999 rolling) |
| 16 | `m_szData[0]` | `char[]` | Variable-length body |

**Header total: 16 bytes + body.**  
Max body size: `8 * 1024` bytes = 8 KB (`MAX_CLIENT_PACK_SIZE`).

### Server/Inter-Server Packet (`server_packet` / `EventNetPacket`)

Same header as above, plus two extra fields appended:

| Field | Type | Description |
|-------|------|-------------|
| `m_i8Guid` | `long long` (8B) | Player/role GUID |
| `m_i4SrvID` | `short` (2B) | Source server ID |

Max body size: `8 * 1024 * 1024` bytes = 8 MB (`PACKET_BODY_MAX`).  
Max single send: 32766 bytes (`MAX_SEND_PACKET_SIZE`).

### Checksum Algorithm

Uses a 30-byte static `encrypt_key` XOR table.  
Checksum is computed over `[PacketID + Index + Body]`.  
ConnSrv validates every incoming client packet and **kicks** the client on mismatch.  
Sequence number `m_i2Index` is also validated — out-of-order packets result in a kick.

---

## Compression & Encryption

### Compression: LZ4

- Library: **LZ4 1.8** (`thirdpartlibs/lz4-1.8/`)
- ZLib is linked (`-lz`) but the game packet compression calls are commented out — LZ4 is the active compressor.
- Compression is applied **server → client only** (outbound from connsrv).
- Threshold: configurable via `hotconf.xml` (`GetCompress_Threshold()`), default **256 bytes**.
- API calls used:
  ```c
  LZ4_compressBound(srcSize)
  LZ4_compress_default(src, dst, srcSize, dstMaxSize)
  LZ4_decompress_fast(src, dst, originalSize)   // on receive
  ```
- Compression type encoding in `m_i4Compress`:
  - Upper 8 bits: `PCT_LZ4 = 2`
  - Lower 24 bits: original (uncompressed) size

### Encryption: XOR cipher

- Applied **after** compression (outbound) and **before** decompression (inbound).
- Only on **external** (client-facing) connections (`IsExternal()` check).
- Uses the same 30-byte `encrypt_key` table as the checksum.
- Functions: `encrypt_packet()` / `decrypt_packet()`.
- Inter-server packets are **not** encrypted (they travel over Unix domain sockets or trusted local TCP).

**Order (send):** Body → LZ4 compress → XOR encrypt → prepend header → send  
**Order (receive):** Strip header → XOR decrypt → LZ4 decompress → dispatch

---

## Inter-Server Communication

### Custom RPC over TCP/Unix Domain Sockets

All inter-server communication uses **librpc** (`TitanRpcNode`, `TitanRpcRemoteNode`).

- No middleware (no RabbitMQ, Kafka, ZeroMQ, gRPC, or protobuf).
- Each server is identified by an `RpcKey = (ServerType, instance_id)`.
- Server types (`TitanCommonEnum.h`):

```cpp
enum SeverType {
    Client_Type  = 0,
    Conn_Type    = 1,   // connsrv / connexsrv
    Login_Type   = 2,   // loginsrv
    Scene_Type   = 3,   // scenesrv
    World_Type   = 4,   // worldsrv
    Data_Type    = 5,   // datasrv
    Cross_Type   = 6,   // crosssrv
    Log_Type     = 7,   // logsrv
    Bridge_Type  = 8,   // bridgesrv
};
```

### Handshake

On every new inter-server TCP connection, a `HAND_SHAKE` message (`msgId=1`) is sent containing:
```
{ nSrvId, nSrvType, nGroupId }
```
This identifies the sender to the receiver before any game traffic flows.

### RPC Request/Response

`PacketRPC` (`libhttps/PacketRpc.h`) implements request/response semantics:
- RPC call packet: `msgId = -100`, carries `sint64 nCallID`
- RPC result packet: `msgId = -101`, carries matching `nCallID`
- Timeout tracked per outstanding call.

### Packet Format (inter-server)

Same binary header as `EventNetPacket` (the larger server packet format), sent over the Unix domain socket or TCP connection. No additional envelope.

---

## Server Ports & Roles

| Server | Role | Port | Notes |
|--------|------|------|-------|
| `connsrv` / `connexsrv` | Client gateway | **8200** (external) / 8210 (internal) | Only server clients connect to |
| `loginsrv` | Authentication, account creation | **7200** | Talks to datasrv for account lookup |
| `scenesrv` | Game zones, combat, monsters, skills | **6200** | 1 instance (see MEMORY.md) |
| `worldsrv` | World logic, rankings, groups, arena | **9200** | Must start after datasrv |
| `datasrv` | MySQL proxy / DB abstraction | **5200** | Only server with DB access |
| `crosssrv` | Cross-server features, territory | **10001** | Uses `srvconf_bincross.xml` |
| `logsrv` | Centralized logging | **10201** | Start first |
| `bridgesrv` | Gateway bridging | configured | Optional |
| MySQL | Database | **3306** | `ahlz` schema |

**HTTP service ports** (all on worldsrv / loginsrv side):

| Service | Port | Purpose |
|---------|------|---------|
| GM operations | **9221** | Admin GM commands |
| Platform management | **9231** | Platform admin |
| Recharge restore | **9203** | Payment recovery |
| Center server | **8810** | External center (non-fatal if unreachable) |
| Apple IAP verify | **8805** | iOS purchase validation |
| HTTP platform login | **8808** | Platform auth callback |
| HTTPS verify | **8807** | HTTPS validation |
| Plugin | **8803** | Plugin integration |
| Activity code | **8801** | Activation codes |
| Card | **8802** | Card system |

---

## Client Login Flow

```
Client
  │─── TCP connect ──────────────────────────► connsrv :8200
  │─── CL_CONN_SRV (1001) ──────────────────► connsrv
  │    {accountId, platform, MAC}
  │                     connsrv ──────────────► loginsrv :7200
  │                     loginsrv ─────────────► datasrv :5200
  │                     datasrv ──────────────► MySQL :3306 (sp_account_create)
  │                     loginsrv ◄─────────────── datasrv
  │◄── LC_CONN_RESP (6001) ──────────────────── connsrv
  │    resultCode: 0=OK, -1=create role, <0=error
  │
  │ [If new account, resultCode=-1:]
  │─── CL_CREATE_ROLE_REQ (1002) ───────────► connsrv ──► loginsrv
  │◄── LC_CREATE_ROLE_RESP (6002) ──────────── connsrv
  │
  │─── CW_ENTER_GAME ───────────────────────► connsrv ──► worldsrv
  │◄── [world state, scene assignment] ─────── connsrv
  │
  │─── CS_* messages ──────────────────────►  connsrv ──► scenesrv
  │◄── SC_* messages ──────────────────────── connsrv
```

**Session state machine** (maintained in connsrv per connection):
```
sit_none → sit_entering → sit_loginok → sit_logincreate → sit_loginfail
```

**Heartbeat:** `CW_HeartBeat` (client→server) / `WC_HeartBeat` (server→client). Missing heartbeats result in disconnect.

**Disconnect propagation:** When a client disconnects, connsrv sends `CONN_CLOSE_LINK` (`msgId=5`) to worldsrv.

---

## Message Routing

`connexsrv` (`TitanInternetServerThreadImpl::OnRecvMsg`) routes messages by packet ID range:

| ID Range | Prefix | Destination |
|----------|--------|-------------|
| 1001–1099 | `CL_*` | loginsrv (Login_Type, id=1) |
| 2001–2999 | `CW_*` | worldsrv (World_Type, id=1) |
| 3001–3999 | `CS_*` | scenesrv (Scene_Type, by `info.sceneid`) |
| 13001–13999 | `CS_EX_*` | scenesrv (extended) |
| 5001–5499 | `CR_*` | crosssrv (Cross_Type, id=1) |
| 5501–5999 | `CB_*` | bridgesrv (Bridge_Type, id=1) |

Server → client responses use the reverse prefix convention (`LC_*`, `WC_*`, `SC_*`, etc.).

---

## HTTP Services

Two separate HTTP subsystems exist:

### A. Outbound HTTP Client (platform/payment integration)

- Library: **libcurl** via libevent async (`TitanEventHttpRequest`)
- JSON bodies serialized with **jsoncpp**
- Used for: platform login verification, Apple IAP validation, payment callbacks, analytics
- Platform URLs are configured per-platform in `srvconf.xml` (~15 platforms: JQ/鲸旗, Apple, GamePot Korea, etc.)
- Methods available: GET, POST, POST JSON, PUT JSON

### B. Inbound HTTP Server (GM / admin operations)

- Custom HTTP server: `titan_https::HttpServer` (`libhttps/HttpServer.h`)
- Uses the same epoll/IOCP reactor stack as inter-server communication (not libevent)
- JSON responses via jsoncpp (`HandleResponse(Connection*, Json::Value&)`)
- Handles: GM commands, platform management, recharge restore

---

## Redis

Redis libraries present but **currently disabled/non-functional**:
- `thirdpartlibs/hiredis/` — hiredis sync C client
- `thirdpartlibs/x_redis/` — extended hiredis fork
- Compile flag: `-Dtitan_hiredis` (not set in any current CMakeLists.txt)

**Intended use (all commented out in worldsrv):**
- Season management data
- Cross-server ranking data
- `CRedisTitanClient` wraps `redisConnectWithTimeout`, `redisCommand`, `redisFree`
- Connection configured by: `titan_utils::GetRedisServerAddr()` / `GetRedisServerPort()` / `GetRedisConnectTimeoutMillSec()`

To enable Redis: uncomment `CRedisOperatorManager::Startup()` in `worldsrv/srv/redis_operator.cpp` and add `-Dtitan_hiredis` to worldsrv's `CMakeLists.txt`.

---

## Library Dependency Summary

| Library | Version | Location | Used By | Purpose |
|---------|---------|----------|---------|---------|
| **libevent** | 2.0.21 | `thirdpartlibs/libevent-2.0.21-stable/` | All servers | Async event loop, inter-server TCP |
| **LZ4** | 1.8 | `thirdpartlibs/lz4-1.8/` | connsrv, loginsrv, datasrv, worldsrv | Packet compression |
| **zlib** | 1.2.3 | `thirdpartlibs/zlib-1.2.3/` | common_static | Linked but inactive for game packets |
| **hiredis** | — | `thirdpartlibs/x_redis/` | worldsrv (disabled) | Redis client |
| **jsoncpp** | — | `thirdpartlibs/jsoncpp/` | All servers | JSON for HTTP bodies, configs |
| **snappy** | — | `libhttps/snappy/` | common_static | Compiled in, not used in game path |
| **Lua** | 5.3.0 | `thirdpartlibs/lua-5.3.0/` | common_static | Server-side scripting (scene/world) |
| **MySQL++** | 3.2.1 | `thirdpartlibs/mysql++-3.2.1/` | datasrv | MySQL ORM |
| **libcurl** | system | system | common_static | Outbound HTTP (platform auth) |
| **openssl** (ssl/crypto) | system | system | common_static | HTTPS client |
| **pthread** | system | system | All servers | Threading |
| **tcmalloc** | system | system | common_static | Memory allocator |
| **XLua** | — | `Client/` | Unity client | Lua scripting in Unity |

**No protobuf used.** The protocol is entirely custom binary packed structs (`#pragma pack(1)`).

---

## Setting Up a New Server

### Checklist for a fresh server instance

1. **Build** (Linux only — the C++ server is Linux-targeted):
   ```bash
   cd Server/
   ./build.sh debug    # or release
   ```

2. **Database**:
   ```bash
   mysql -uroot -hlocalhost ahlz < Server/ahlz.sql
   mysql -uroot ahlz -e "UPDATE tb_database_version SET version = 677;"
   # Also create missing stored procedures for cross-server territory:
   # sp_cross_territoty and sp_cross_territoty_insert_update
   ```

3. **Config files** (all in `Server/bin/`):
   - `srvconf.xml` — DB credentials, server ports, platform auth keys, group_id
   - `srvconf_bincross.xml` — crosssrv config (group_id=10001)
   - `hotconf.xml` — feature flags, compression threshold, version check switch
   - `game.json` — server list for startup
   - `data/config/KuafusaijiConfig.lua` — **Critical**: ensure season `endTime` is before 2038-01-19 (32-bit time_t limit), or worldsrv/crosssrv will crash at startup

4. **Start order** (wait a few seconds between each):
   ```
   1. logsrv.exe
   2. datasrv.exe
   3. crosssrv.exe srvconf_bincross.xml
   4. worldsrv.exe
   5. scenesrv.exe
   6. loginsrv.exe
   7. connsrv.exe  (or connexsrv.exe)
   ```

5. **Client config**:
   - `Client/Assets/Resources/Data/ServerList.xml` → set server IP to your host's IP, port 8200
   - `Client/Assets/Resources/Data/UpSetting.csv` → `useThirdPartyLogin = 0` for local auth
   - `hotconf.xml` → `<CheckMsgSwitch>0</CheckMsgSwitch>` to disable version check

6. **Firewall**: Open TCP port **8200** for clients. All other ports (7200, 9200, 6200, 5200, 10001, 10201) are internal — only expose 8200 externally.

7. **Known non-fatal startup errors** (safe to ignore):
   - `titaneventhttprequest.cpp:115:Error Request` — worldsrv can't reach external center server at 127.0.0.1:8200
   - Monster position errors in scenesrv logs
   - `EquipBossCreateMonster` errors — boss spawn issues

8. **Redis** (optional, currently disabled): Uncomment `CRedisOperatorManager::Startup()` in `worldsrv/srv/redis_operator.cpp`, add `-Dtitan_hiredis` to worldsrv's `CMakeLists.txt`, rebuild.
