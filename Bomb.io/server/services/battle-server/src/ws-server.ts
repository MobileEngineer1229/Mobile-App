/**
 * Battle Server — WebSocket transport (replaces UDP).
 *
 * Why WebSocket instead of UDP:
 *   • Browsers have no raw UDP API — WebSocket is the only option for browser clients
 *   • WebSocket (TCP) works through NAT and firewalls on any network
 *   • One server process manages multiple simultaneous rooms (Map<roomId, BattleRoom>)
 *
 * Protocol:
 *   Client → Server (JSON):
 *     { type:'JOIN',  roomId, playerId, token, username }
 *     { type:'INPUT', seq, dir, bomb, ability, charId }
 *     { type:'PING' }
 *
 *   Server → Client (JSON):
 *     { type:'JOINED',    playerId, spawnX, spawnY, mapData:{width,height,tiles} }
 *     { type:'WAITING',   playerCount, needed }
 *     { type:'GAME_START',playerCount }
 *     { type:'STATE',     frame, players[], bombs[], explosions[], terrain[] }
 *     { type:'GAME_OVER', winnerId }
 *     { type:'PONG',      ts }
 *     { type:'ERROR',     message }
 */

import { GameLoop }        from './game-loop';
import { BombSystem }      from './bomb-system';
import { TerrainSystem }   from './terrain-system';
import { AbilitySystem }   from './ability-system';
import { PlayerController }from './player-controller';
import { CollisionGrid }   from './collision-grid';
import { MatchReporter }   from './match-reporter';
import { Logger }          from '../../../shared/logger';
import { GameMode, TerrainType } from '../../../shared/types';
import type { Player, PlayerId, Bomb, MapData, PlayerInput } from '../../../shared/types';
import { createHash } from 'node:crypto';

const log             = new Logger('Battle');
const BOMB_RATE_MS    = 200;   // min ms between bombs per player
const JOIN_TIMEOUT_MS = 30_000; // kick client if no JOIN within 30s
const MIN_PLAYERS     = 2;     // game starts when this many join

// ── Per-WebSocket session data ────────────────────────────────────────────────

interface WsData {
  playerId: PlayerId | null;
  roomId:   string | null;
}

// ── TrackedPlayer ─────────────────────────────────────────────────────────────

interface TrackedPlayer extends Player {
  ws:                any; // ServerWebSocket<WsData>
  kills:             number;
  deaths:            number;
  damageDealt:       number;
  eliminatedAtFrame: number;
}

// ── BattleRoom ────────────────────────────────────────────────────────────────

export class BattleRoom {
  private players         = new Map<PlayerId, TrackedPlayer>();
  private bombs           = new Map<number, Bomb>();
  private terrainSystem:  TerrainSystem;
  private collisionGrid   = new CollisionGrid();
  private bombSystem:     BombSystem;
  private abilitySystem   = new AbilitySystem();
  private playerController: PlayerController;
  private gameLoop:       GameLoop;
  private reporter:       MatchReporter;
  private startedAt       = Date.now();
  private isRunning       = false;

  constructor(
    readonly roomId:   string,
    readonly mapData:  MapData,
    private gameMode:  GameMode,
    private redis:     any,
    private onEmpty:   () => void,
  ) {
    this.terrainSystem    = new TerrainSystem(mapData.tiles);
    this.bombSystem       = new BombSystem(this.terrainSystem, this.collisionGrid);
    this.playerController = new PlayerController(this.terrainSystem, this.abilitySystem);
    this.gameLoop         = new GameLoop(this.tick.bind(this));
    this.reporter         = new MatchReporter(redis);
  }

  // ── Player management ───────────────────────────────────────────────────────

  addPlayer(player: Player, ws: any): void {
    const tracked: TrackedPlayer = {
      ...player, ws,
      kills: 0, deaths: 0, damageDealt: 0, eliminatedAtFrame: 0,
    };
    this.players.set(player.id, tracked);
    tracked.abilityStates = this.abilitySystem.buildAbilityStates(tracked);
    log.join(player.id, this.roomId, { username: player.username, total: this.players.size });

    // Notify everyone of new player count
    this.broadcast({ type: 'WAITING', playerCount: this.players.size, needed: MIN_PLAYERS });

    if (!this.isRunning && this.players.size >= MIN_PLAYERS) {
      this.startGame();
    }
  }

  removePlayer(playerId: PlayerId): void {
    this.players.delete(playerId);
    log.leave(playerId, this.roomId);
    if (this.players.size === 0) {
      this.stop();
      this.onEmpty();
    }
  }

  handleInput(playerId: PlayerId, input: PlayerInput): void {
    const player = this.players.get(playerId);
    if (!player?.isAlive) return;

    // Rate-limit bomb placement
    if (input.placeBomb) {
      const now = Date.now();
      if (now - player.lastBombTime < BOMB_RATE_MS) {
        input.placeBomb = false;
      } else {
        player.lastBombTime = now;
      }
    }

    if (input.placeBomb && player.bombsPlaced < player.bombsMax) {
      const bomb = this.bombSystem.placeBomb(
        player.id, player.position.x, player.position.y, player.blastRadius,
      );
      this.bombs.set(bomb.id, bomb);
      player.bombsPlaced++;
      log.bomb('PLACE', { player: playerId, x: player.position.x, y: player.position.y });
    }

    player.pendingInput = input;
  }

  getPlayerCount(): number { return this.players.size; }
  isGameRunning():  boolean { return this.isRunning; }

  // ── Game lifecycle ──────────────────────────────────────────────────────────

  private startGame(): void {
    this.isRunning = true;
    this.startedAt = Date.now();
    log.gameStart(this.roomId, this.players.size, this.mapData.mapId);
    this.broadcast({ type: 'GAME_START', playerCount: this.players.size });
    this.gameLoop.start();
  }

  stop(): void {
    this.gameLoop.stop();
    this.isRunning = false;
  }

  // ── Tick (20 Hz) ────────────────────────────────────────────────────────────

  private tick(frame: number): void {
    // 1. Sync collision grid
    this.collisionGrid.sync(this.players);

    // 2. Process queued player inputs
    this.players.forEach(player => {
      if (player.pendingInput) {
        this.playerController.processInput(player, player.pendingInput, this.bombs, this.bombSystem);
        player.pendingInput = null;
      }
    });

    // 3. Update bombs / get explosions
    const explosions = this.bombSystem.update(this.bombs);

    // 4. Apply explosion damage
    explosions.forEach(exp => {
      // Find bomber
      const bomber = [...this.players.values()].find(p =>
        [...this.bombs.values()].some(b => b.id === exp.bombId && b.ownerPlayerId === p.id),
      );

      exp.hitPlayers.forEach(pid => {
        const p    = this.players.get(pid);
        if (!p || !p.isAlive) return;
        const char = p.characters.find(c => c.characterId === p.activeCharacterId);
        if (!char) return;

        // Team-based friendly fire protection
        if (bomber && bomber.id !== pid && bomber.teamId !== 0 && bomber.teamId === p.teamId) return;

        // Shield ability blocks damage
        const shielded = p.abilityStates.some((s, i) =>
          char.abilities[i]?.abilityType === 4 && s.activeTicksLeft > 0,
        );
        if (shielded) return;

        char.currentHp--;
        if (bomber && bomber.id !== pid) bomber.damageDealt++;

        if (char.currentHp <= 0) {
          p.isAlive = false;
          p.deaths++;
          p.eliminatedAtFrame = frame;
          if (bomber && bomber.id !== pid) {
            bomber.kills++;
            log.kill(bomber.id, pid, this.roomId);
          }
          // Notify the dead player
          try { p.ws.send(JSON.stringify({ type: 'YOU_DIED' })); } catch {}
        }
      });
    });

    // 5. Update ability cooldowns
    this.abilitySystem.updateCooldowns(this.players);

    // 6. Broadcast game state
    this.broadcastState(frame);

    // 7. Check win condition
    const alive      = [...this.players.values()].filter(p => p.isAlive);
    const aliveTeams = new Set(alive.filter(p => p.teamId !== 0).map(p => p.teamId));
    const gameOver   = this.players.size > 1 && (
      this.gameMode === GameMode.TEAM ? aliveTeams.size <= 1 : alive.length <= 1
    );

    if (gameOver) {
      const winnerId = this.gameMode === GameMode.TEAM
        ? (alive.find(p => aliveTeams.size === 1 && p.teamId === [...aliveTeams][0])?.id ?? null)
        : (alive[0]?.id ?? null);

      this.broadcast({ type: 'GAME_OVER', winnerId });
      log.ok('Game over', { room: this.roomId, winner: winnerId, frames: frame });

      this.reporter.publish(
        this.roomId, this.mapData.mapId, this.players as any, winnerId, this.startedAt, frame,
      ).catch(err => log.error('Failed to publish match result', err));

      this.stop();
    }
  }

  // ── Broadcast helpers ────────────────────────────────────────────────────────

  private broadcastState(frame: number): void {
    const payload = JSON.stringify({
      type: 'STATE',
      frame,
      players: [...this.players.values()].map(p => {
        const char = p.characters.find(c => c.characterId === p.activeCharacterId);
        return {
          id:             p.id,
          username:       p.username,
          x:              p.position.x,
          y:              p.position.y,
          hp:             char?.currentHp ?? 0,
          maxHp:          char?.baseHp ?? 3,
          isAlive:        p.isAlive,
          kills:          p.kills,
          bombsAvailable: p.bombsMax - p.bombsPlaced,
          blastRadius:    p.blastRadius,
          teamId:         p.teamId,
        };
      }),
      bombs: [...this.bombs.values()].map(b => ({
        id:     b.id,
        x:      b.x,
        y:      b.y,
        fuse:   b.fuseTicks,
        radius: b.blastRadius,
      })),
      explosions: this.bombSystem.getRecentExplosions().map(e => ({
        cells: e.affectedCells,
      })),
      terrain: this.terrainSystem.flushUpdates(),
    });

    this.players.forEach(p => { try { p.ws.send(payload); } catch {} });
  }

  private broadcast(obj: unknown): void {
    const msg = JSON.stringify(obj);
    this.players.forEach(p => { try { p.ws.send(msg); } catch {} });
  }
}

// ── BattleServer (manages all rooms on this process) ─────────────────────────

export class BattleServer {
  private rooms          = new Map<string, BattleRoom>();
  private pendingTimeouts = new Map<any, ReturnType<typeof setTimeout>>();

  constructor(
    private port:       number,
    private redis:      any,
    private buildMap:   (mapId: string) => MapData,
    private getMode:    (roomId: string) => Promise<GameMode>,
  ) {}

  start(): void {
    const self = this;

    (globalThis as any).Bun.serve({
      port: this.port,

      fetch(req: Request, server: any): Response | undefined {
        const { pathname } = new URL(req.url);

        if (pathname === '/battle') {
          const upgraded = server.upgrade(req, {
            data: { playerId: null, roomId: null } as WsData,
          });
          return upgraded ? undefined : new Response('Upgrade failed', { status: 400 });
        }

        if (pathname === '/health') {
          const totalPlayers = [...self.rooms.values()]
            .reduce((n, r) => n + r.getPlayerCount(), 0);
          return new Response(JSON.stringify({
            rooms: self.rooms.size, players: totalPlayers,
          }), { headers: { 'Content-Type': 'application/json' } });
        }

        return new Response('💣 Bomb.io Battle Server', { status: 200 });
      },

      websocket: {
        open(ws: any): void {
          log.connect(ws.remoteAddress, 'awaiting JOIN');
          // Kick unauthenticated connections after timeout
          self.pendingTimeouts.set(ws, setTimeout(() => {
            if (!ws.data.playerId) {
              log.warn('JOIN timeout — closing idle connection', { addr: ws.remoteAddress });
              ws.close(4000, 'JOIN_TIMEOUT');
            }
            self.pendingTimeouts.delete(ws);
          }, JOIN_TIMEOUT_MS));
        },

        async message(ws: any, data: string): Promise<void> {
          try {
            const msg = JSON.parse(data);
            await self.handleMessage(ws, msg);
          } catch (err) {
            log.warn('Malformed message', { addr: ws.remoteAddress });
            ws.send(JSON.stringify({ type: 'ERROR', message: 'Invalid JSON' }));
          }
        },

        close(ws: any, code: number): void {
          clearTimeout(self.pendingTimeouts.get(ws));
          self.pendingTimeouts.delete(ws);

          const { playerId, roomId } = ws.data as WsData;
          if (playerId && roomId) {
            self.rooms.get(roomId)?.removePlayer(playerId);
            log.disconnect(playerId, `room=${roomId} code=${code}`);
          } else {
            log.disconnect(ws.remoteAddress, `unauthenticated code=${code}`);
          }
        },
      },
    });

    log.start(this.port, { protocol: 'WebSocket', path: '/battle', rooms: 0 });
  }

  // ── Message dispatch ────────────────────────────────────────────────────────

  private async handleMessage(ws: any, msg: any): Promise<void> {
    switch (msg.type) {
      case 'JOIN':  await this.handleJoin(ws, msg);  break;
      case 'INPUT':       this.handleInput(ws, msg); break;
      case 'PING':
        ws.send(JSON.stringify({ type: 'PONG', ts: Date.now() }));
        break;
      default:
        ws.send(JSON.stringify({ type: 'ERROR', message: `Unknown message type: ${msg.type}` }));
    }
  }

  private async handleJoin(ws: any, msg: any): Promise<void> {
    const { roomId, playerId, token, username } = msg;
    const playerKey = String(playerId ?? '');

    if (!roomId || !playerKey) {
      ws.send(JSON.stringify({ type: 'ERROR', message: 'roomId and playerId required' }));
      return;
    }

    // Validate session token against Redis
    if (token) {
      const valid = await this.validateToken(token, playerKey);
      if (!valid) {
        log.warn('Invalid token on JOIN', { playerId: playerKey, roomId });
        ws.send(JSON.stringify({ type: 'ERROR', message: 'Invalid or expired token' }));
        ws.close(4001, 'UNAUTHORIZED');
        return;
      }
    }

    // Cancel join timeout — this connection is legitimate
    clearTimeout(this.pendingTimeouts.get(ws));
    this.pendingTimeouts.delete(ws);

    // Get or create room
    let room = this.rooms.get(roomId);
    if (!room) {
      const mapId   = await this.getMapIdForRoom(roomId);
      const mapData = this.buildMap(mapId);
      const mode    = await this.getMode(roomId).catch(() => GameMode.INDIVIDUAL);
      room = new BattleRoom(roomId, mapData, mode, this.redis, () => {
        this.rooms.delete(roomId);
        log.info('Room closed (empty)', { roomId });
      });
      this.rooms.set(roomId, room);
      log.info('Room opened', { roomId, mapId, mode });
    }

    // Pick spawn point (cycle through available spawns)
    const spawnIdx = room.getPlayerCount() % room.mapData.spawnPoints.length;
    const spawn    = room.mapData.spawnPoints[spawnIdx];

    // Build Player object
    const player: Player = {
      id:               playerKey,
      username:         String(username ?? `Player${playerKey}`),
      sessionToken:     token ?? '',
      address:          { host: ws.remoteAddress ?? '0.0.0.0', port: 0 },
      position:         { x: spawn.x, y: spawn.y },
      facing:           0 as any,
      speed:            1,
      blastRadius:      2,
      bombsMax:         1,
      bombsPlaced:      0,
      teamId:           0,
      isAlive:          true,
      lastBombTime:     0,
      lastSequence:     0,
      pendingInput:     null,
      abilityStates:    [],
      activeCharacterId: 1,
      characters:       [{
        characterId:    1,
        name:           'Blaster',
        level:          1,
        experience:     0,
        baseHp:         3,
        currentHp:      3,
        baseSpeed:      1,
        baseBombCount:  1,
        baseBlastRadius:2,
        abilities:      [],
        abilityStates:  [],
        skinId:         'blaster_default',
        bombEffectId:   'bomb_default',
      }],
    };

    // Attach to WebSocket data
    ws.data.playerId = player.id;
    ws.data.roomId   = roomId;

    // Confirm join — send map data so client can render the map
    ws.send(JSON.stringify({
      type:        'JOINED',
      playerId:    player.id,
      spawnX:      spawn.x,
      spawnY:      spawn.y,
      mapData: {
        width:  room.mapData.width,
        height: room.mapData.height,
        tiles:  room.mapData.tiles,   // 2D array of TerrainType numbers
      },
    }));

    room.addPlayer(player, ws);
    log.info('Player joined room', { playerId: playerKey, roomId, spawnX: spawn.x, spawnY: spawn.y });
  }

  private handleInput(ws: any, msg: any): void {
    const { playerId, roomId } = ws.data as WsData;
    if (!playerId || !roomId) return;
    const room = this.rooms.get(roomId);
    if (!room) return;

    room.handleInput(playerId, {
      sequence:    Number(msg.seq    ?? 0),
      moveDir:     Number(msg.dir    ?? 0),
      placeBomb:   Boolean(msg.bomb),
      abilitySlot: Number(msg.ability ?? 0),
      characterId: Number(msg.charId  ?? 0),
    });
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private async getMapIdForRoom(roomId: string): Promise<string> {
    try {
      const raw = await this.redis.get(`room:${roomId}`);
      if (raw) return (JSON.parse(raw) as any).mapId ?? 'volcano-isle';
    } catch {}
    return 'volcano-isle';
  }

  private async validateToken(token: string, playerId: PlayerId): Promise<boolean> {
    try {
      const hash   = await sha256(token);
      const sessionPlayerId = await this.redis.get(`session:${hash}`);
      return sessionPlayerId === playerId;
    } catch { return false; }
  }
}

// ── Shared utility ─────────────────────────────────────────────────────────────

async function sha256(input: string): Promise<string> {
  return createHash('sha256').update(input).digest('hex').slice(0, 32);
}
