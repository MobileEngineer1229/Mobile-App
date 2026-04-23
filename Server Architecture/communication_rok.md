# Server Communication Architecture

## Overview

The server is built on the **Skynet** framework (a Lua actor-model distributed server), with **nine specialized servers** communicating via TCP cluster channels. There is **no UDP** anywhere in the stack.

---

## Server Roster & Roles

| Server | Role | Client-Facing |
|--------|------|:-------------:|
| `login_server` | Authentication, session handshake | Yes |
| `game_server` | Core game logic, player state | Yes |
| `battle_server` | Combat resolution | No |
| `chat_server` | Messaging, channels | Yes |
| `db_server` | MySQL abstraction layer | No |
| `center_server` | Cross-server shared data (guilds, hell activities) | No |
| `log_server` | Analytics and audit logging | No |
| `push_server` | Mobile push notifications | No |
| `monitor_server` | Health monitoring, orchestration | No |

---

## Network Topology

```
Internet
    │
    ▼
[Nginx :44444]  ──TCP──►  login_server :58888
[Nginx :58111]  ──HTTP──►  monitor_server :58000

login_server ──cluster──► game_server
game_server  ──cluster──► battle_server
game_server  ──cluster──► chat_server
game_server  ──cluster──► center_server
All servers  ──cluster──► db_server
All servers  ──cluster──► log_server
All servers  ──cluster──► monitor_server
All servers  ──cluster──► push_server
```

---

## Port Allocation

### Skynet Cluster Ports (server-to-server TCP)

| Server | Cluster Port |
|--------|-------------|
| monitor | 57000 |
| login | 57001 |
| center | 57002 |
| db | 57004 |
| game | 57005 |
| battle | 57006 |
| push | 57007 |
| chat | 57009 |
| log | 57011 |

### Client-Facing Ports

| Endpoint | External (Nginx) | Internal |
|----------|:----------------:|:--------:|
| Login gateway | 44444 | 58888 |
| Game gateway | direct | configurable in `game.conf` |
| Chat gateway | direct | configurable in `chat.conf` |
| Monitor web | 58111 | 58000 |

Cluster port config files: `etc/cluster_*.lua`
```lua
-- etc/cluster_game1.lua (example)
game1   = "127.0.0.1:57005"
chat1   = "127.0.0.1:57009"
login1  = "127.0.0.1:57001"
center1 = "127.0.0.1:57002"
db1     = "127.0.0.1:57004"
battle1 = "127.0.0.1:57006"
push1   = "127.0.0.1:57007"
log1    = "127.0.0.1:57011"
monitor = "127.0.0.1:57000"
```

---

## Transport Protocol: TCP Only

**Harbor mode is disabled** (`harbor = 0` in `etc/common.conf`). All server-to-server communication goes through Skynet's **cluster** module, which uses plain TCP.

### Client ↔ Server (TCP)

- Handled by `skynet.socket` / `skynet.socketdriver`
- Nginx fronts port 44444 with `tcp_nodelay on` (disables Nagle's algorithm)
- Client gate is `server/login_server/LoginGate.lua` and `server/game_server/Gamed.lua`

### Server ↔ Server (TCP cluster)

```lua
local cluster = require "skynet.cluster"
cluster.open(clusterport)               -- listen for inbound cluster calls
cluster.call(node, service, method, …)  -- synchronous RPC
cluster.send(node, service, method, …)  -- fire-and-forget
cluster.query(node, svrname)            -- service discovery
cluster.snax(node, svrname, address)    -- get a SNAX proxy to remote service
cluster.register(SERVICE_NAME)         -- publish this service to the cluster
```

---

## Client–Server Handshake (Login)

All client connections go through a DH + DES encrypted handshake before any game data is exchanged.

```
Server → Client:  base64( 8-byte challenge )
Client → Server:  base64( handshake key )
Server → Client:  base64( DH exchange value )
Client → Server:  base64( HMAC-MD5(challenge) )
Client → Server:  base64( DES(token) )
Server → Client:  "200 " + base64( subid )
```

**Crypto packages used** (from `skynet.crypt`):
- `DES` — session key encryption
- `HMAC-MD5` — challenge authentication
- `DH-exchange` — key agreement
- `Base64` — transport encoding

---

## Binary Protocol: Sproto

All game messages between client and server use **Sproto**, a compact binary protocol (similar in spirit to protobuf but Lua-native).

### Protocol Definition Files

| File | Purpose |
|------|---------|
| `common/protocol/Protocol.sproto` | All client↔server commands (C2S/S2C) |
| `common/protocol/Common.sproto` | Shared structures (PosInfo, SoldierInfo, etc.) |
| `common/protocol/Db.sproto` | Database entity schema (d_role, d_user, etc.) |

### Message Structure

```
GateMessage
  └─ header: uid, deviceType, clientVersion
  └─ content[]: one or more sub-messages
       └─ type: command id (C2S 1–30000 / S2C push)
       └─ body: sproto-encoded payload
```

### Large Message Compression

Messages exceeding **8 KB** are compressed with **zlib** before being sent.

```lua
local zlib = require "zlib"   -- 3rd/zlib
```

### Code Generation

Re-generate client Lua/C# stubs from server `.sproto` files:
```bash
lua tool/sprotodump.lua
```

---

## Redis

### Library

```lua
local redis = require "skynet.db.redis"   -- bundled with Skynet
```

Service file: `common/service/RedisAgent.lua`

### Connection

```lua
redisip   = "127.0.0.1"     -- env var: redisip
redisport = 56379            -- base port; each agent gets base + index offset
redisauth = ""               -- optional password
```

### Database Assignments

| Server | Redis DB |
|--------|:--------:|
| db_server | 0 |
| game_server | 2 |
| center_server | 3 |
| login_server | 4 |
| chat_server | 5 |
| battle_server | 6 |
| push_server | 7 |
| log_server | 10 |
| monitor_server | 11 |

### Connection Pool (RedisAgent)

Each server spawns `redisnum` (5–10) `RedisAgent` service instances, accessed via the MSM (Multi Service Manager):

```lua
-- Usage from any Lua service
Common.redisExecute(cmd, routeIndex, pipeline)
-- routes to MSM.RedisAgent[routeIndex % poolSize + 1]
```

Redis is **flushed** (`FLUSHDB`) on server startup — it is used only for ephemeral/session data, not persistent storage.

---

## MySQL

### Library

Accessed through the `db_server` cluster node; other servers never connect to MySQL directly.

```lua
Common.mysqlExecute(sql, routeIndex)
-- → cluster.call(dbNode, "MysqlAgent", ...)
```

Schema files:
- `data/sql/ig.sql` — game data
- `data/sql/log.sql` — audit/log data

---

## Service Manager Patterns

### SM — Singleton Manager

One instance per cluster node, created on first access via `snax.uniqueservice`.

```lua
SM.SysLog.req.Init()
SM.EnumInit.req.initAllEnum()
SM.MonitorSubscribe.req.connectMonitorAndPush(selfNodeName)
```

### MSM — Multi-instance Service Manager

Multiple workers, request routed by hash.

```lua
MSM.RedisAgent[routeIndex]     -- pool of Redis connections
MSM.MysqlAgent[routeIndex]     -- pool of MySQL connections
```

---

## JSON

Used for HTTP API responses (PHP web layer) and internal config loading.

```lua
local cjson      = require "cjson"
local cjson_safe = require "cjson.safe"
-- source: 3rd/lua-cjson/
```

---

## Third-Party Packages Summary

| Package | Path | Purpose |
|---------|------|---------|
| `skynet` | `3rd/skynet/` | Core framework: actor model, clustering, sockets |
| `snax` | (Skynet built-in) | Lua RPC layer over Skynet services |
| `sprotoloader` | (Skynet built-in) | Load/parse `.sproto` protocol files |
| `cjson` | `3rd/lua-cjson/` | Fast JSON encode/decode |
| `zlib` | `3rd/zlib/` | Message compression for payloads >8 KB |
| `lpeg` | `3rd/lpeg/` | Pattern matching (used by Skynet internals) |
| `lfs` | `3rd/luafilesystem/` | File I/O |
| `zset` | `3rd/lua-zset/` | Sorted set data structure |
| `pbc` | `3rd/pbc/binding/lua53/` | Protocol Buffer bindings (game & chat server) |
| `skynet.crypt` | (Skynet built-in) | DES, HMAC-MD5, DH-exchange, Base64 |
| `skynet.db.redis` | (Skynet built-in) | Redis client |
| `skynet.socket` | (Skynet built-in) | TCP socket management |
| `skynet.cluster` | (Skynet built-in) | Cross-node TCP cluster RPC |
| `skynet.sharedata` | (Skynet built-in) | Shared read-only data between co-routines |

---

## Nginx Configuration Summary

File: `etc/nginx/nginx.conf`

```nginx
# TCP stream proxy (login gateway)
stream {
    upstream loginServer { server 127.0.0.1:58888; }
    server {
        listen      44444;
        proxy_pass  loginServer;
        proxy_connect_timeout  3s;
        proxy_timeout          5s;
        tcp_nodelay on;
    }
}

# HTTP proxy (monitor dashboard)
http {
    server {
        listen 58111;
        location / { proxy_pass http://127.0.0.1:58000; }
    }
}
```

---

## Server Startup Sequence (all servers follow this pattern)

```lua
-- server/*/Main.lua
SM.SysLog.req.Init()                                 -- 1. logging
SM.EnumInit.req.initAllEnum()                        -- 2. config tables & enums
SM.MonitorSubscribe.req.connectMonitorAndPush(node)  -- 3. register with monitor
-- server-specific service initialization            -- 4. business services
skynet.uniqueservice("Gamed")                        -- 5. open client gate (if applicable)
cluster.open(clusterport)                            -- 6. listen for cluster calls
```

---

## Setting Up a New Game Server Node

### Minimum Steps

1. **Copy a cluster config**: `etc/cluster_game1.lua` → `etc/cluster_game2.lua`, update ports.
2. **Create a server config**: `etc/game2.conf` based on `etc/game1.conf`.
   - Set unique `clusterport`, `clientport`, `redisdb`, `nodename`.
3. **Configure Redis**: ensure the assigned `redisdb` index is free.
4. **Register the node** in every other server's cluster config file so they can `cluster.call` it.
5. **Inform center_server** about the new game node (it maintains the global node registry).
6. **Start with Skynet**:
   ```bash
   ./skynet etc/game2.conf
   ```

### Key Environment Variables (read via `skynet.getenv`)

| Variable | Description |
|----------|-------------|
| `nodename` | Unique name for this node (e.g., `game2`) |
| `clusterport` | Skynet cluster listen port |
| `clientport` | Port for game clients |
| `redisip` / `redisport` | Redis connection |
| `redisdb` | Redis database index |
| `redisnum` | RedisAgent pool size |
| `debugport` | Skynet debug console port (0 = disabled) |

---

## Quick Reference: Communication Matrix

| From → To | Mechanism | Sync? |
|-----------|-----------|:-----:|
| Client → login_server | TCP (via Nginx :44444) | Yes |
| Client → game_server | TCP direct | Yes |
| Client → chat_server | TCP direct | Yes |
| game_server → db_server | `cluster.call` (TCP) | Yes |
| game_server → battle_server | `cluster.call` (TCP) | Yes |
| game_server → chat_server | `cluster.call` (TCP) | Yes |
| game_server → center_server | `cluster.call` (TCP) | Yes |
| Any server → log_server | `cluster.send` (TCP) | No |
| Any server → push_server | `cluster.call` (TCP) | Yes |
| Any server → monitor_server | `cluster.call` (TCP) | Yes |
| Any server → Redis | `skynet.db.redis` (TCP) | Yes |
| Any server → MySQL | via db_server cluster call | Yes |
