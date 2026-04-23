# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Bomb.io is a multiplayer browser-based battle game (Bomberman-style) with grid-based movement, bomb mechanics, terrain effects, and a character ability system. The backend is a TypeScript/Bun microservices architecture using Protocol Buffers over UDP (battle) and TCP (lobby/auth).

## Repository Structure

```
Bomb.io/
├── server/          # Backend microservices (Bun + TypeScript)
└── client/          # Frontend game client
```

### Server Architecture

Four microservices communicate via Redis:

| Service | Protocol | Port | Responsibility |
|---|---|---|---|
| `login-server` | TCP | 8000 | Auth, sessions, character management |
| `match-server` | TCP | 8001 | Room creation, matchmaking, lobby |
| `battle-server` | **UDP** | 5000–5100 | Game loop (20 Hz), bombs, terrain, abilities |
| `rank-server` | TCP | 8002 | ELO, leaderboard, stats |

The battle server is the core priority. It runs a fixed-timestep game loop at 20 Hz (50 ms/tick) and broadcasts delta-compressed `GameState` protobufs to all players in a room.

### Shared Code

- `shared/proto/` — `.proto` definitions for `common`, `battle`, `character`, `match`, `terrain`
- `shared/types/` — TypeScript interfaces: `Player`, `Character`, `Bomb`, `TerrainGrid`, `Abilities`
- `shared/utils/` — `grid-utils.ts` (A* pathfinding), `collision.ts` (spatial hash), `math-utils.ts`

## Tech Stack

- **Runtime**: Bun (not Node.js — use `bun` CLI everywhere)
- **Language**: TypeScript
- **Serialization**: Protocol Buffers (protobufjs)
- **Transport**: UDP (battle), TCP (others)
- **Cache/Pub-Sub**: Redis 7
- **Containers**: Docker + Docker Compose

## Key Game Systems

### Terrain
Seven terrain types with properties (walkable, speedMultiplier, destructible, blocksExplosion):
- `GRASS` (1.0x speed), `FOREST` (0.7x, blocks explosion), `SNOW` (0.85x), `WATER` (blocked), `MOUNTAIN` (blocked, blocks explosion), `DESTRUCTIBLE` (blocked, destructible → becomes GRASS on hit), `INDESTRUCTIBLE` (permanent wall)

### Bomb System
- Fuse: 60 ticks (3 s at 20 Hz)
- Explosion propagates in 4 cardinal directions up to `blastRadius` tiles
- Chain reactions: bombs caught in blast detonate immediately
- Terrain destruction: `DESTRUCTIBLE` blocks become `GRASS` and stop the blast in that direction

### Character System
- 5 character slots per player account
- Each character has base stats (hp, speed, bombCount, blastRadius) and up to 3 ability slots
- Abilities: `EXTRA_BOMB`, `SPEED_BOOST`, `BOMB_KICK`, `BLAST_SHIELD`, `REMOTE_DETONATE`, `GHOST_WALK`, `BLAST_PLUS`

### Input/Validation (Anti-cheat)
- Client sends `PlayerInput` (sequence, moveDir, placeBomb, abilitySlot, characterId) via UDP
- Server validates: rate-limit bombs (max 1 per 200 ms), movement doesn't cross walls, sequence ordering

## Development Commands

```bash
# Start all services
docker-compose up

# Start a single service in dev mode (from its directory)
bun run dev

# Run tests
bun test

# Generate TypeScript from .proto files
bash scripts/proto-gen.sh
```

## Performance Targets

- 20–50 players per battle room
- UDP packet size < 512 bytes
- Up to 10 simultaneous bomb explosions per tick
- Map size: 50×50 to 100×100 grid
