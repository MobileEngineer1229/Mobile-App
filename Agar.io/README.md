# Agar.io — C++ Server + React Client

## Architecture

```
TCP WebSocket (port 9001) ── reliable events ──► Join, Welcome, Death, Leaderboard, Chat
                                                  SDP/ICE signaling for WebRTC
UDP (WebRTC DataChannel) ─── battle only ──────► MoveInput (C→S), DeltaUpdate (S→C)
MongoDB ───────────────────── persistence ──────► Player records, scores, quests
Redis ─────────────────────── hot data ─────────► Leaderboard ZADD, Pub/Sub chat, Presence
```

## Server Dependencies (VS 2017)

Install via **vcpkg**:
```
vcpkg install boost:x64-windows
vcpkg install protobuf:x64-windows
vcpkg install mongo-cxx-driver:x64-windows
vcpkg install hiredis:x64-windows
vcpkg install libdatachannel:x64-windows
vcpkg install openssl:x64-windows
```

Install **EnTT** (header-only):
```
git clone https://github.com/skypjack/entt vendor/entt
```

## Protobuf code generation

```bash
# Run once before building
protoc --cpp_out=server/src proto/agario.proto
```
This generates `agario.pb.h` and `agario.pb.cc` inside `server/src/`.

## Build (VS 2017 + CMake)

```
# In VS 2017: File → Open → CMake → select Agar.io/server/CMakeLists.txt
# OR from command line:
cd server
mkdir build && cd build
cmake .. -G "Visual Studio 15 2017 Win64" -DCMAKE_TOOLCHAIN_FILE=C:/vcpkg/scripts/buildsystems/vcpkg.cmake
cmake --build . --config Release
```

## Run Server

```
# Start MongoDB
mongod --dbpath C:\data\db

# Start Redis
redis-server

# Start game server
./build/Release/agario_server.exe
```

## Client Setup

```bash
# Copy proto file to public folder (for protobufjs to load at runtime)
copy proto\agario.proto client\public\proto\agario.proto

cd client
npm install
npm start      # dev server on http://localhost:3000
npm run build  # production build
```

## Network Protocol

### TCP (WebSocket) — Reliable

| Direction | Message | Description |
|-----------|---------|-------------|
| C → S | `JOIN_REQUEST` | Player name, mode, skin |
| C → S | `SDP_OFFER` | WebRTC offer for UDP DataChannel |
| C → S | `ICE_CANDIDATE` | ICE candidate for WebRTC |
| C → S | `CHAT_SEND` | Chat message |
| S → C | `WELCOME` | Player ID, world size, SDP answer |
| S → C | `DEATH` | Killer name, score, XP gained |
| S → C | `LEADERBOARD` | Top 10 players (from Redis ZADD) |
| S → C | `CHAT_MSG` | Broadcast chat |
| S → C | `QUEST_UPDATE` | Daily quest progress |
| S → C | `LEVEL_UP` | XP milestone reached |

### UDP (WebRTC DataChannel, unreliable+unordered)

| Direction | Message | Rate | Description |
|-----------|---------|------|-------------|
| C → S | `MOVE_INPUT` | ~60 Hz | Mouse world-coords + split/eject flags |
| S → C | `FULL_SNAPSHOT` | once | All entities on first join |
| S → C | `DELTA_UPDATE` | 20 Hz | Only changed/eaten entities |

## ECS Architecture (IO-Server.md)

```
Components (contiguous arrays via EnTT):
  Pos, Vel, Circle, Mass          ← spatial/physics
  CellComp                        ← player cell ownership + recombine timer
  PlayerInfo                      ← player meta + latest input
  FoodTag, VirusTag, EjectedTag   ← entity type tags

Systems (per tick):
  InputSystem    → apply UDP inputs → PlayerInfo.mouse_target
  PhysicsSystem  → move cells toward mouse, decay ejected mass
  CollisionSystem→ eat food/players/viruses, split on virus (SpatialGrid broad-phase)
  DecaySystem    → passive mass loss above threshold
  RecombineSystem→ merge split cells when timer expires
  SpawnSystem    → maintain food + virus counts
  BRSystem       → shrink safe zone, apply out-of-zone damage
```

## Spatial Grid (IO-Server.md)

- Cell size = 1000 units (matches AoI radius)
- AoI check = current cell + 8 neighbors only
- O(1) insert/remove, O(k) query where k = entities in 9 cells
- Faster than quadtree for IO games (entities move every tick)

## MongoDB Schema

```json
// players collection
{
  "_id": "uuid",
  "name": "PlayerName",
  "level": 5,
  "xp": 4200,
  "high_score": 150000,
  "skin_id": "earth",
  "stats": { "kills": 42, "deaths": 18 },
  "last_login": "2026-04-11T00:00:00Z"
}

// scores collection
{
  "name": "PlayerName",
  "score": 82000,
  "mode": "FFA",
  "timestamp": 1744329600
}
```

## Redis Keys

| Key | Type | TTL | Purpose |
|-----|------|-----|---------|
| `player:{id}` | STRING | 10s | Presence heartbeat |
| `leaderboard:ffa` | ZSET | — | Global score ranking (ZADD every 1s) |
| `chat:global` | PUBSUB | — | Cross-server chat broadcast |
