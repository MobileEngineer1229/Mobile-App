import type { Redis } from 'ioredis';
import { GameMode } from '../../../shared/types';
import type { RoomManager, RoomInfo } from './room-manager';

const QUEUE_KEY  = 'matchmaking:queue';
const MIN_PLAYERS = 2;
const MAX_PLAYERS = 4;

export class Matchmaker {
  constructor(private redis: Redis, private rooms: RoomManager) {}

  // ── CUSTOM matchmaking queue (auto-creates a CUSTOM room when enough players) ──

  async enqueue(playerId: number, mapId = 'random', gameMode = GameMode.INDIVIDUAL): Promise<void> {
    await this.redis.rpush(QUEUE_KEY, JSON.stringify({ playerId, mapId, gameMode, joinedAt: Date.now() }));
    await this.tryMatch();
  }

  async dequeue(playerId: number): Promise<void> {
    const entries = await this.redis.lrange(QUEUE_KEY, 0, -1);
    for (const entry of entries) {
      if (JSON.parse(entry).playerId === playerId) {
        await this.redis.lrem(QUEUE_KEY, 1, entry);
        break;
      }
    }
  }

  async tryMatch(): Promise<string | null> {
    const len = await this.redis.llen(QUEUE_KEY);
    if (len < MIN_PLAYERS) return null;

    const entries = await this.redis.lrange(QUEUE_KEY, 0, MAX_PLAYERS - 1);
    const players = entries.map(e => JSON.parse(e) as { playerId: number; mapId: string; gameMode: GameMode });

    const room = await this.rooms.createCustomRoom(
      players[0].playerId,
      players[0].mapId,
      MAX_PLAYERS,
      players[0].gameMode,
    );

    for (const p of players.slice(1)) {
      await this.rooms.joinRoom(p.playerId, room.roomId);
    }

    for (const entry of entries) {
      await this.redis.lrem(QUEUE_KEY, 1, entry);
    }

    const started = await this.rooms.startRoom(room.roomId, players[0].playerId);
    if ('error' in started) return null;

    await this.redis.publish('match:ready', JSON.stringify({
      roomId:    room.roomId,
      playerIds: players.map((p: { playerId: number }) => p.playerId),
      gameMode:  room.gameMode,
      teams:     (started as RoomInfo).teams,
    }));

    return room.roomId;
  }

  async getQueueLength(): Promise<number> {
    return this.redis.llen(QUEUE_KEY);
  }

  // ── LARGE room join (no matchmaking — just drop into the next available room) ──

  async joinLarge(playerId: number): Promise<RoomInfo> {
    const room = await this.rooms.joinLargeRoom(playerId);
    if (!room) throw new Error('No large room available');

    if (room.status === 'STARTING') {
      await this.redis.publish('match:ready', JSON.stringify({
        roomId:    room.roomId,
        playerIds: room.playerIds,
        gameMode:  room.gameMode,
        teams:     [],
      }));
    }

    return room;
  }
}
