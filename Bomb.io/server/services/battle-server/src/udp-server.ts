import { GameLoop } from './game-loop';
import { BombSystem } from './bomb-system';
import { TerrainSystem } from './terrain-system';
import { AbilitySystem } from './ability-system';
import { PlayerController } from './player-controller';
import { CollisionGrid } from './collision-grid';
import { MatchReporter } from './match-reporter';
import { GameMode } from '../../../shared/types';
import type { Player, Bomb, MapData, PlayerInput } from '../../../shared/types';

const BOMB_RATE_LIMIT_MS = 200;

interface TrackedPlayer extends Player {
  kills:             number;
  deaths:            number;
  damageDealt:       number;
  eliminatedAtFrame: number;
}

export class BattleServer {
  private socket: any = null; // Bun.UDPSocket — typed as any until bun-types installed
  private gameLoop:        GameLoop;
  private bombSystem:      BombSystem;
  private terrainSystem:   TerrainSystem;
  private abilitySystem:   AbilitySystem;
  private playerController: PlayerController;
  private collisionGrid:   CollisionGrid;
  private reporter:        MatchReporter;

  private players:         Map<number, TrackedPlayer> = new Map();
  private bombs:           Map<number, Bomb>          = new Map();
  private addrToPlayerId:  Map<string, number>        = new Map();

  private readonly port:     number;
  private readonly roomId:   string;
  private readonly mapId:    string;
  private readonly gameMode: GameMode;
  private startedAt = Date.now();
  private isRunning = false;

  constructor(port: number, roomId: string, mapData: MapData, private redis: any, gameMode = GameMode.INDIVIDUAL) {
    this.port     = port;
    this.roomId   = roomId;
    this.mapId    = mapData.mapId;
    this.gameMode = gameMode;

    this.terrainSystem    = new TerrainSystem(mapData.tiles);
    this.collisionGrid    = new CollisionGrid();
    this.bombSystem       = new BombSystem(this.terrainSystem, this.collisionGrid);
    this.abilitySystem    = new AbilitySystem();
    this.playerController = new PlayerController(this.terrainSystem, this.abilitySystem);
    this.gameLoop         = new GameLoop(this.tick.bind(this));
    this.reporter         = new MatchReporter(redis);
  }

  async start(): Promise<void> {
    this.socket = await (globalThis as any).Bun.udpSocket({
      port:     this.port,
      hostname: '0.0.0.0',
      socket: {
        data: (_sock: any, data: ArrayBuffer, port: number, address: string) => {
          this.handlePacket(Buffer.from(data), address, port);
        },
      },
    });
    this.startedAt = Date.now();
    this.isRunning = true;
    this.gameLoop.start();
    console.log(`[Battle] Room ${this.roomId} listening on UDP :${this.port}`);
  }

  stop(): void {
    this.gameLoop.stop();
    this.socket?.close();
    this.isRunning = false;
  }

  addPlayer(player: Player): void {
    const tracked: TrackedPlayer = {
      ...player,
      kills: 0, deaths: 0, damageDealt: 0, eliminatedAtFrame: 0,
    };
    this.players.set(player.id, tracked);
    this.addrToPlayerId.set(`${player.address.host}:${player.address.port}`, player.id);
    tracked.abilityStates = this.abilitySystem.buildAbilityStates(tracked);
  }

  removePlayer(playerId: number): void {
    const p = this.players.get(playerId);
    if (p) this.addrToPlayerId.delete(`${p.address.host}:${p.address.port}`);
    this.players.delete(playerId);
  }

  private tick(frame: number): void {
    this.collisionGrid.sync(this.players);

    this.players.forEach(player => {
      if (player.pendingInput) {
        this.playerController.processInput(player, player.pendingInput, this.bombs, this.bombSystem);
        player.pendingInput = null;
      }
    });

    const explosions = this.bombSystem.update(this.bombs);

    explosions.forEach(exp => {
      const bomber = [...this.players.values()].find(p =>
        [...this.bombs.values()].some(b => b.id === exp.bombId && b.ownerPlayerId === p.id)
      );

      exp.hitPlayers.forEach(pid => {
        const p = this.players.get(pid);
        if (!p || !p.isAlive) return;
        const char = p.characters.find(c => c.characterId === p.activeCharacterId);
        if (!char) return;

        // Friendly fire: teammates never take damage (teamId 0 = individual, always damages)
        if (bomber && bomber.id !== pid && bomber.teamId !== 0 && bomber.teamId === p.teamId) return;

        const isShielded = p.abilityStates.some((s, i) =>
          char.abilities[i]?.abilityType === 4 && s.activeTicksLeft > 0
        );
        if (isShielded) return;

        char.currentHp--;
        if (bomber && bomber.id !== pid) bomber.damageDealt++;

        if (char.currentHp <= 0) {
          p.isAlive = false;
          p.deaths++;
          p.eliminatedAtFrame = frame;
          if (bomber && bomber.id !== pid) bomber.kills++;
        }
      });
    });

    this.abilitySystem.updateCooldowns(this.players);

    const alive = [...this.players.values()].filter(p => p.isAlive);
    const aliveTeams = new Set(alive.filter(p => p.teamId !== 0).map(p => p.teamId));
    const gameOver = this.players.size > 1 && (
      this.gameMode === GameMode.TEAM ? aliveTeams.size <= 1 : alive.length <= 1
    );
    if (gameOver) {
      const winnerId = this.gameMode === GameMode.TEAM
        ? (alive.find(p => aliveTeams.size === 1 && p.teamId === [...aliveTeams][0])?.id ?? null)
        : (alive[0]?.id ?? null);
      this.broadcastWinner(winnerId);
      this.reporter.publish(
        this.roomId, this.mapId, this.players as any,
        winnerId, this.startedAt, frame,
      ).catch(err => console.error('[Battle] Failed to publish result:', err));
      this.stop();
      return;
    }

    this.broadcastGameState(frame);
  }

  private handlePacket(data: Buffer, address: string, port: number): void {
    const playerId = this.addrToPlayerId.get(`${address}:${port}`);
    if (!playerId) return;
    const player = this.players.get(playerId);
    if (!player?.isAlive) return;

    try {
      const input = this.decodeInput(data);
      if (!input) return;

      if (input.placeBomb) {
        const now = Date.now();
        if (now - player.lastBombTime < BOMB_RATE_LIMIT_MS) {
          input.placeBomb = false;
        } else {
          player.lastBombTime = now;
        }
      }

      if (input.placeBomb && player.bombsPlaced < player.bombsMax) {
        const bomb = this.bombSystem.placeBomb(player.id, player.position.x, player.position.y, player.blastRadius);
        this.bombs.set(bomb.id, bomb);
        player.bombsPlaced++;
      }

      player.pendingInput = input;
    } catch {
      // malformed packet
    }
  }

  private decodeInput(data: Buffer): PlayerInput | null {
    if (data.length < 5) return null;
    const view        = new DataView(data.buffer, data.byteOffset, data.byteLength);
    const sequence    = view.getUint16(0);
    const flags       = view.getUint8(2);
    const moveDir     = view.getUint8(3);
    const abilitySlot = view.getUint8(4);
    const characterId = data.length >= 7 ? view.getUint16(5) : 0;
    return { sequence, moveDir, placeBomb: (flags & 0x01) !== 0, abilitySlot, characterId };
  }

  private broadcastGameState(frame: number): void {
    if (!this.socket) return;

    const payload = Buffer.from(JSON.stringify({
      frame,
      players: [...this.players.values()].map(p => {
        const char = p.characters.find(c => c.characterId === p.activeCharacterId);
        return {
          playerId: p.id, characterId: p.activeCharacterId,
          x: p.position.x, y: p.position.y, facing: p.facing,
          hp: char?.currentHp ?? 0, maxHp: char?.baseHp ?? 0,
          bombsAvailable: p.bombsMax - p.bombsPlaced, bombsMax: p.bombsMax,
          blastRadius: p.blastRadius, isAlive: p.isAlive,
        };
      }),
      bombs: [...this.bombs.values()].map(b => ({
        bombId: b.id, ownerId: b.ownerPlayerId,
        x: b.x, y: b.y, fuseTicks: b.fuseTicks, blastRadius: b.blastRadius,
      })),
      explosions: this.bombSystem.getRecentExplosions().map(e => ({
        bombId: e.bombId, cells: e.affectedCells, hitPlayers: e.hitPlayers,
      })),
      terrainUpdates: this.terrainSystem.flushUpdates(),
    }));

    this.players.forEach(p => this.socket.send(payload, p.address.port, p.address.host));
  }

  private broadcastWinner(winnerId: number | null): void {
    if (!this.socket) return;
    const payload = Buffer.from(JSON.stringify({ type: 'GAME_OVER', winnerId }));
    this.players.forEach(p => this.socket.send(payload, p.address.port, p.address.host));
  }
}
