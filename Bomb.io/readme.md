Excellent! Bomb.io with multi-character and terrain systems—this is a substantial but well-scoped project. Let me structure a production-ready architecture that separates concerns cleanly while keeping the battle logic as the priority.

📋 Complete Feature List
Core Battle System (Phase 1 Priority)
Feature	Description
Player session	Login, session token, reconnection handling
Room system	Create/join battle rooms, max players configurable
Movement	Grid-based movement with terrain cost modifiers
Bomb placement	Plant bombs with fuse timers, explosion radius
Explosion propagation	Chain reactions, terrain destruction (if applicable)
Collision detection	Bomb blast zones, player hit detection
Character switching	Switch between owned characters mid-battle
Death/respawn	Eliminated characters, respawn timer/mechanics
Win condition	Last player standing, team modes optional
Character System
Feature	Description
5 character slots	Per player account, persistent storage
Character abilities	Each character has unique active/passive skills
Ability examples	Extra bomb, faster movement, bomb kick, blast immunity, remote detonation
Cooldown system	Ability usage limits and cooldown timers
Character stats	Speed, bomb count, blast radius per character
Map & Terrain System
Feature	Description
Grid-based map	Tile-based system (e.g., 50x50 to 100x100)
Terrain types	Forest (slow), Snow (slippery), Water (blocked/swim), Mountain (blocked)
Destructible blocks	Soft walls that bombs can destroy
Indestructible walls	Hard obstacles for strategic play
Terrain effects	Movement speed modifiers, bomb interactions
Map selection/voting	Pre-battle map choice
Meta Systems (Phase 2)
Feature	Description
Ranking/ELO	Competitive matchmaking
Character progression	Level up characters, unlock abilities
Cosmetics	Skins, bomb effects
Replay system	Save and replay matches
Spectator mode	Watch ongoing battles
🏗️ Server Architecture
Based on industry best practices for session-based multiplayer games, here's a battle-tested structure :

text
┌─────────────────────────────────────────────────────────────────┐
│                         API Gateway (TCP)                        │
│                    Authentication, Routing, Rate Limiting         │
└───────────────────────────────┬─────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────────┐
        │                       │                           │
        ▼                       ▼                           ▼
┌───────────────┐      ┌────────────────┐          ┌───────────────┐
│  Login Server │      │  Match Server  │          │  Rank Server  │
│   (TCP:8000)  │      │   (TCP:8001)   │          │  (TCP:8002)   │
├───────────────┤      ├────────────────┤          ├───────────────┤
│ • Auth        │      │ • Room mgmt    │          │ • Leaderboard │
│ • Session     │◄────►│ • Matchmaking  │◄────────►│ • ELO calc    │
│ • Character   │      │ • Lobby state  │          │ • Stats       │
│   Management  │      │ • Load balance │          │ • History     │
└───────────────┘      └───────┬────────┘          └───────────────┘
                               │
                               │ Assign Battle Server
                               ▼
                    ┌──────────────────────┐
                    │   Battle Server      │
                    │   (UDP:5000-5100)    │
                    ├──────────────────────┤
                    │ • Game loop (20Hz)   │
                    │ • Physics/collision  │
                    │ • Bomb mechanics     │
                    │ • Ability system     │
                    │ • Terrain queries    │
                    │ • Player input       │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │   Redis Cluster       │
                    │ • Session cache       │
                    │ • Room state          │
                    │ • Matchmaking queues  │
                    │ • Leaderboard data    │
                    └──────────────────────┘
📁 Project Structure
text
bomb-io-backend/
├── shared/
│   ├── proto/
│   │   ├── common.proto           # Shared message types
│   │   ├── battle.proto           # UDP battle protocol
│   │   ├── match.proto            # Matchmaking/lobby
│   │   ├── character.proto        # Character data
│   │   └── terrain.proto          # Map/terrain definitions
│   ├── types/
│   │   ├── Player.ts
│   │   ├── Character.ts
│   │   ├── Bomb.ts
│   │   ├── TerrainGrid.ts
│   │   └── Abilities.ts
│   └── utils/
│       ├── grid-utils.ts          # A* pathfinding, terrain checks
│       ├── collision.ts           # Spatial hash for explosions
│       └── math-utils.ts
│
├── services/
│   ├── login-server/
│   │   ├── src/
│   │   │   ├── server.ts
│   │   │   ├── auth-handler.ts
│   │   │   └── character-manager.ts
│   │   └── Dockerfile
│   │
│   ├── match-server/
│   │   ├── src/
│   │   │   ├── server.ts
│   │   │   ├── room-manager.ts
│   │   │   ├── matchmaker.ts
│   │   │   └── lobby-handler.ts
│   │   └── Dockerfile
│   │
│   ├── battle-server/               # UDP - Your Priority
│   │   ├── src/
│   │   │   ├── udp-server.ts        # Main UDP handler
│   │   │   ├── game-loop.ts         # Fixed timestep (20Hz)
│   │   │   ├── bomb-system.ts       # Bomb placement, explosion, chains
│   │   │   ├── terrain-system.ts    # Grid queries, movement costs
│   │   │   ├── ability-system.ts    # Character abilities
│   │   │   ├── player-controller.ts # Input processing
│   │   │   └── collision-grid.ts    # Spatial partitioning
│   │   └── Dockerfile
│   │
│   └── rank-server/
│       ├── src/
│       │   ├── server.ts
│       │   ├── leaderboard.ts
│       │   └── stats-tracker.ts
│       └── Dockerfile
│
├── scripts/
│   ├── proto-gen.sh                 # Generate TS from .proto
│   └── map-editor/                  # Simple terrain designer
│
├── docker-compose.yml
└── package.json                     # Workspace monorepo
📦 Protocol Buffer Definitions
shared/proto/common.proto

protobuf
syntax = "proto3";
package bombio;

// Core types used across services
message Vector2 {
    int32 x = 1;
    int32 y = 2;
}

enum TerrainType {
    GRASS = 0;
    FOREST = 1;
    SNOW = 2;
    WATER = 3;
    MOUNTAIN = 4;
    DESTRUCTIBLE = 5;
    INDESTRUCTIBLE = 6;
}

enum Direction {
    NONE = 0;
    UP = 1;
    DOWN = 2;
    LEFT = 3;
    RIGHT = 4;
}
shared/proto/battle.proto

protobuf
syntax = "proto3";
package bombio.battle;

import "common.proto";

// Client -> Server (UDP)
message PlayerInput {
    uint32 sequence = 1;           // For packet ordering
    Direction move_dir = 2;
    bool place_bomb = 3;
    uint32 ability_slot = 4;       // Which ability to use
    uint32 character_id = 5;       // Switch to this character
}

// Server -> Client (UDP, delta-compressed)
message GameState {
    uint32 frame = 1;
    uint32 timestamp = 2;
    repeated PlayerState players = 3;
    repeated BombState bombs = 4;
    repeated ExplosionEvent explosions = 5;  // Only current frame
    repeated TerrainUpdate terrain_updates = 6; // Destroyed blocks
}

message PlayerState {
    uint32 player_id = 1;
    uint32 character_id = 2;
    Vector2 position = 3;
    Direction facing = 4;
    uint32 hp = 5;
    uint32 max_hp = 6;
    uint32 bombs_available = 7;
    uint32 bombs_max = 8;
    uint32 blast_radius = 9;
    repeated AbilityState abilities = 10;
}

message BombState {
    uint32 bomb_id = 1;
    uint32 owner_player_id = 2;
    Vector2 position = 3;
    uint32 fuse_ticks = 4;         // Frames until explosion
    uint32 blast_radius = 5;
}

message ExplosionEvent {
    uint32 bomb_id = 1;
    repeated Vector2 affected_cells = 2;
    repeated uint32 hit_players = 3;
}

message TerrainUpdate {
    Vector2 position = 1;
    TerrainType new_type = 2;
}

message AbilityState {
    uint32 ability_id = 1;
    uint32 cooldown_remaining = 2;
    bool is_ready = 3;
}
shared/proto/character.proto

protobuf
syntax = "proto3";
package bombio.character;

enum AbilityType {
    NONE = 0;
    EXTRA_BOMB = 1;        // +1 bomb capacity
    SPEED_BOOST = 2;       // Faster movement
    BOMB_KICK = 3;         // Kick bombs away
    BLAST_SHIELD = 4;      // Immune to own bombs
    REMOTE_DETONATE = 5;   // Trigger bombs manually
    GHOST_WALK = 6;        // Pass through soft blocks briefly
    BLAST_PLUS = 7;        // +1 blast radius
}

message CharacterData {
    uint32 character_id = 1;
    string name = 2;
    uint32 level = 3;
    uint32 experience = 4;
    
    // Base stats
    uint32 base_hp = 5;
    float base_speed = 6;
    uint32 base_bomb_count = 7;
    uint32 base_blast_radius = 8;
    
    // Abilities (max 3 per character)
    repeated AbilitySlot abilities = 9;
    
    // Cosmetics
    string skin_id = 10;
    string bomb_effect_id = 11;
}

message AbilitySlot {
    AbilityType ability_type = 1;
    uint32 cooldown_ticks = 2;
    uint32 duration_ticks = 3;  // For active abilities
    bool is_passive = 4;
}

message PlayerCharacters {
    uint32 player_id = 1;
    repeated CharacterData characters = 2;  // Max 5
    uint32 active_character_id = 3;
}
🎮 Battle Server Core Implementation (Priority)
typescript
// battle-server/src/udp-server.ts
import { createSocket } from 'bun';
import { GameLoop } from './game-loop';
import { BombSystem } from './bomb-system';
import { TerrainSystem } from './terrain-system';
import { AbilitySystem } from './ability-system';
import { CollisionGrid } from './collision-grid';
import * as proto from '../shared/proto-compiled';

export class BattleServer {
  private socket: ReturnType<typeof createSocket>;
  private gameLoop: GameLoop;
  private bombSystem: BombSystem;
  private terrainSystem: TerrainSystem;
  private abilitySystem: AbilitySystem;
  private collisionGrid: CollisionGrid;
  
  private players: Map<number, Player> = new Map();
  private bombs: Map<number, Bomb> = new Map();
  private readonly TICK_RATE = 20; // 20Hz = 50ms per tick
  
  // Terrain grid (static + dynamic)
  private terrainGrid: TerrainType[][];
  private destructibleMap: Map<string, boolean> = new Map();
  
  constructor(port: number, mapData: TerrainType[][]) {
    this.terrainGrid = mapData;
    this.collisionGrid = new CollisionGrid(mapData.length, mapData[0].length);
    this.terrainSystem = new TerrainSystem(this.terrainGrid);
    this.bombSystem = new BombSystem(this.terrainSystem, this.collisionGrid);
    this.abilitySystem = new AbilitySystem();
    
    this.socket = createSocket({
      type: 'udp4',
      port: port,
      hostname: '0.0.0.0'
    });
    
    this.gameLoop = new GameLoop(this.tick.bind(this), this.TICK_RATE);
    
    this.socket.on('data', (data, addr) => {
      this.handleInput(data, addr);
    });
  }
  
  private tick(deltaTime: number): void {
    // 1. Process queued player inputs
    this.processPendingInputs();
    
    // 2. Update bomb fuses and process explosions
    const explosions = this.bombSystem.update(this.bombs, deltaTime);
    
    // 3. Apply explosion damage to players
    this.applyExplosionDamage(explosions);
    
    // 4. Update player positions (with terrain speed modifiers)
    this.updatePlayerPositions(deltaTime);
    
    // 5. Update ability cooldowns
    this.abilitySystem.updateCooldowns(this.players, deltaTime);
    
    // 6. Check win condition
    this.checkWinCondition();
    
    // 7. Broadcast game state (delta compressed)
    this.broadcastGameState();
  }
  
  private handleInput(data: Buffer, addr: any): void {
    try {
      const input = proto.battle.PlayerInput.decode(data);
      const player = this.getPlayerByAddress(addr);
      
      if (!player) return;
      
      // Validate input (anti-cheat)
      if (!this.validateInput(player, input)) return;
      
      // Queue input for processing on next tick
      player.pendingInput = input;
      
      // Handle character switching
      if (input.characterId && input.characterId !== player.activeCharacterId) {
        this.switchCharacter(player, input.characterId);
      }
      
      // Handle bomb placement
      if (input.placeBomb && player.canPlaceBomb()) {
        const bomb = this.bombSystem.placeBomb(
          player.id,
          player.position,
          player.blastRadius
        );
        this.bombs.set(bomb.id, bomb);
        player.bombsPlaced++;
      }
      
      // Handle ability usage
      if (input.abilitySlot > 0) {
        this.abilitySystem.useAbility(player, input.abilitySlot);
      }
      
    } catch (err) {
      console.error('Failed to decode protobuf:', err);
    }
  }
  
  private validateInput(player: Player, input: proto.battle.PlayerInput): boolean {
    // Rate limiting: max 1 bomb per 200ms
    if (input.placeBomb) {
      const now = Date.now();
      if (now - player.lastBombTime < 200) return false;
      player.lastBombTime = now;
    }
    
    // Validate movement direction doesn't go through walls
    if (input.moveDir !== proto.common.Direction.NONE) {
      const newPos = this.terrainSystem.getNewPosition(
        player.position,
        input.moveDir,
        player.speed
      );
      if (!this.terrainSystem.isWalkable(newPos.x, newPos.y)) {
        return false;
      }
    }
    
    return true;
  }
  
  private broadcastGameState(): void {
    const state: proto.battle.GameState = {
      frame: this.gameLoop.currentFrame,
      timestamp: Date.now(),
      players: Array.from(this.players.values()).map(p => p.toProto()),
      bombs: Array.from(this.bombs.values()).map(b => b.toProto()),
      explosions: this.bombSystem.getRecentExplosions(),
      terrainUpdates: this.terrainSystem.getPendingUpdates()
    };
    
    const encoded = proto.battle.GameState.encode(state).finish();
    
    // Send to all players in this battle
    this.players.forEach((player, playerId) => {
      this.socket.send(encoded, player.address.port, player.address.host);
    });
  }
}
🗺️ Terrain System with Grid Effects
typescript
// battle-server/src/terrain-system.ts
import { TerrainType } from '../shared/proto-compiled/common';

interface TerrainProperties {
  walkable: boolean;
  speedMultiplier: number;
  destructible: boolean;
  blocksExplosion: boolean;
}

export class TerrainSystem {
  private grid: TerrainType[][];
  private properties: Map<TerrainType, TerrainProperties>;
  
  constructor(grid: TerrainType[][]) {
    this.grid = grid;
    this.properties = new Map([
      [TerrainType.GRASS, { walkable: true, speedMultiplier: 1.0, destructible: false, blocksExplosion: false }],
      [TerrainType.FOREST, { walkable: true, speedMultiplier: 0.7, destructible: false, blocksExplosion: true }],
      [TerrainType.SNOW, { walkable: true, speedMultiplier: 0.85, destructible: false, blocksExplosion: false }],
      [TerrainType.WATER, { walkable: false, speedMultiplier: 0, destructible: false, blocksExplosion: false }],
      [TerrainType.MOUNTAIN, { walkable: false, speedMultiplier: 0, destructible: false, blocksExplosion: true }],
      [TerrainType.DESTRUCTIBLE, { walkable: false, speedMultiplier: 0, destructible: true, blocksExplosion: true }],
      [TerrainType.INDESTRUCTIBLE, { walkable: false, speedMultiplier: 0, destructible: false, blocksExplosion: true }],
    ]);
  }
  
  isWalkable(x: number, y: number): boolean {
    if (x < 0 || x >= this.grid[0].length || y < 0 || y >= this.grid.length) {
      return false;
    }
    const terrain = this.grid[y][x];
    return this.properties.get(terrain)?.walkable ?? false;
  }
  
  getSpeedMultiplier(x: number, y: number): number {
    if (!this.isValidPosition(x, y)) return 1.0;
    const terrain = this.grid[y][x];
    return this.properties.get(terrain)?.speedMultiplier ?? 1.0;
  }
  
  destroyTile(x: number, y: number): boolean {
    if (!this.isValidPosition(x, y)) return false;
    const terrain = this.grid[y][x];
    const props = this.properties.get(terrain);
    
    if (props?.destructible) {
      this.grid[y][x] = TerrainType.GRASS; // Become walkable grass
      return true;
    }
    return false;
  }
  
  blocksExplosion(x: number, y: number): boolean {
    if (!this.isValidPosition(x, y)) return true;
    const terrain = this.grid[y][x];
    return this.properties.get(terrain)?.blocksExplosion ?? true;
  }
  
  private isValidPosition(x: number, y: number): boolean {
    return x >= 0 && x < this.grid[0].length && y >= 0 && y < this.grid.length;
  }
}
💣 Bomb System with Chain Reactions
typescript
// battle-server/src/bomb-system.ts
import { TerrainSystem } from './terrain-system';
import { CollisionGrid } from './collision-grid';

interface Bomb {
  id: number;
  ownerPlayerId: number;
  x: number;
  y: number;
  fuseTicks: number;
  blastRadius: number;
}

interface ExplosionResult {
  bombId: number;
  affectedCells: { x: number; y: number }[];
  destroyedBlocks: { x: number; y: number }[];
  hitPlayers: number[];
  triggeredBombs: Bomb[]; // Chain reactions
}

export class BombSystem {
  private terrainSystem: TerrainSystem;
  private collisionGrid: CollisionGrid;
  private recentExplosions: ExplosionResult[] = [];
  
  constructor(terrainSystem: TerrainSystem, collisionGrid: CollisionGrid) {
    this.terrainSystem = terrainSystem;
    this.collisionGrid = collisionGrid;
  }
  
  update(bombs: Map<number, Bomb>, deltaTime: number): ExplosionResult[] {
    const explosions: ExplosionResult[] = [];
    const bombsToRemove: number[] = [];
    
    bombs.forEach((bomb, bombId) => {
      bomb.fuseTicks -= deltaTime;
      
      if (bomb.fuseTicks <= 0) {
        const explosion = this.detonate(bomb, bombs);
        explosions.push(explosion);
        bombsToRemove.push(bombId);
        
        // Chain reaction: detonate bombs caught in blast
        explosion.triggeredBombs.forEach(triggeredBomb => {
          if (bombs.has(triggeredBomb.id)) {
            const chainExplosion = this.detonate(triggeredBomb, bombs);
            explosions.push(chainExplosion);
            bombsToRemove.push(triggeredBomb.id);
          }
        });
      }
    });
    
    bombsToRemove.forEach(id => bombs.delete(id));
    this.recentExplosions = explosions;
    
    return explosions;
  }
  
  private detonate(bomb: Bomb, allBombs: Map<number, Bomb>): ExplosionResult {
    const affectedCells: { x: number; y: number }[] = [];
    const destroyedBlocks: { x: number; y: number }[] = [];
    const triggeredBombs: Bomb[] = [];
    
    // Four directions: up, down, left, right
    const directions = [[0, -1], [0, 1], [-1, 0], [1, 0]];
    
    directions.forEach(([dx, dy]) => {
      for (let i = 1; i <= bomb.blastRadius; i++) {
        const nx = bomb.x + dx * i;
        const ny = bomb.y + dy * i;
        
        affectedCells.push({ x: nx, y: ny });
        
        // Check if blast hits a bomb
        allBombs.forEach((otherBomb) => {
          if (otherBomb.id !== bomb.id && 
              otherBomb.x === nx && otherBomb.y === ny) {
            triggeredBombs.push(otherBomb);
          }
        });
        
        // Check terrain blocking
        if (this.terrainSystem.blocksExplosion(nx, ny)) {
          // Try to destroy the block
          if (this.terrainSystem.destroyTile(nx, ny)) {
            destroyedBlocks.push({ x: nx, y: ny });
          }
          break; // Explosion stopped by wall
        }
      }
    });
    
    return {
      bombId: bomb.id,
      affectedCells,
      destroyedBlocks,
      hitPlayers: [], // Populated by game loop
      triggeredBombs
    };
  }
  
  placeBomb(playerId: number, x: number, y: number, blastRadius: number): Bomb {
    return {
      id: this.generateBombId(),
      ownerPlayerId: playerId,
      x, y,
      fuseTicks: 60, // 3 seconds at 20Hz
      blastRadius
    };
  }
  
  getRecentExplosions(): ExplosionResult[] {
    return this.recentExplosions;
  }
  
  private generateBombId(): number {
    return Math.floor(Math.random() * 1000000);
  }
}
🚀 Deployment Configuration
yaml
# docker-compose.yml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

  login-server:
    build: ./services/login-server
    ports:
      - "8000:8000"
    environment:
      - REDIS_HOST=redis
    depends_on:
      - redis

  match-server:
    build: ./services/match-server
    ports:
      - "8001:8001"
    environment:
      - REDIS_HOST=redis
      - LOGIN_SERVER_URL=http://login-server:8000
    depends_on:
      - redis
      - login-server

  battle-server:
    build: ./services/battle-server
    ports:
      - "5000-5010:5000-5010/udp"
    environment:
      - REDIS_HOST=redis
      - MATCH_SERVER_URL=http://match-server:8001
      - MAX_PLAYERS=50
    depends_on:
      - redis
      - match-server
    deploy:
      replicas: 3
    ulimits:
      nofile:
        soft: 65536
        hard: 65536

  rank-server:
    build: ./services/rank-server
    ports:
      - "8002:8002"
    environment:
      - REDIS_HOST=redis
    depends_on:
      - redis

volumes:
  redis-data:
📊 Performance Targets for Bomb.io
Metric	Target
Players per battle	20-50
Tick rate	20 Hz
UDP packet size	< 512 bytes
Bomb explosions per tick	Up to 10 simultaneous
Map size	50x50 to 100x100
Character abilities	3 per character
🎯 Next Steps
Week 1-2: Implement battle server core with UDP, protobuf encoding, and basic movement

Week 3: Add bomb placement and explosion propagation

Week 4: Implement terrain system with speed modifiers and destructible blocks

Week 5: Add character switching and ability system

Week 6: Build match server and room management

Week 7: Add login/character persistence

Week 8: Polish, load testing, bug fixes

This architecture separates concerns cleanly, uses protobuf for efficient binary serialization , and follows session-based game design patterns used in production multiplayer games . The UDP battle server with fixed timestep ensures fair, responsive gameplay even with many concurrent players.