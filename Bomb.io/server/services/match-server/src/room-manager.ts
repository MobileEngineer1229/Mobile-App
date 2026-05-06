// Redis instance typed as any — ioredis uses `export =` which can't be `import type`'d with ESNext modules
import { RoomType, GameMode } from '../../../shared/types';
import type { PlayerId, RoomInfo, RoomStatus, Team } from '../../../shared/types';

export type { RoomInfo, RoomStatus };

const ROOM_TTL        = 60 * 60 * 24; // 24 h
const LARGE_MAX       = 500;
const LARGE_MAP_ID    = 'large-map';
const LARGE_ROOMS_KEY = 'rooms:large:waiting';

let roomSeq = 1;

export class RoomManager {
  constructor(private redis: any) {}

  // ── LARGE rooms (server-created) ─────────────────────────────────────────

  async ensureLargeRoom(): Promise<RoomInfo> {
    const ids = await this.redis.lrange(LARGE_ROOMS_KEY, 0, 0);
    if (ids.length) {
      const room = await this.getRoom(ids[0]);
      if (room && room.status === 'WAITING') return room;
      await this.redis.lrem(LARGE_ROOMS_KEY, 1, ids[0]);
    }
    return this.spawnLargeRoom();
  }

  async spawnLargeRoomWithMap(mapId: string): Promise<RoomInfo> {
    const room = this.buildRoom({ type: RoomType.LARGE, gameMode: GameMode.INDIVIDUAL, maxPlayers: LARGE_MAX, mapId, creatorPlayerId: null });
    await this.saveRoom(room);
    await this.redis.rpush(LARGE_ROOMS_KEY, room.roomId);
    return room;
  }

  private async spawnLargeRoom(): Promise<RoomInfo> {
    const room = this.buildRoom({
      type:            RoomType.LARGE,
      gameMode:        GameMode.INDIVIDUAL,
      maxPlayers:      LARGE_MAX,
      mapId:           LARGE_MAP_ID,
      creatorPlayerId: null,
    });
    await this.saveRoom(room);
    await this.redis.rpush(LARGE_ROOMS_KEY, room.roomId);
    return room;
  }

  async joinLargeRoom(playerId: PlayerId): Promise<RoomInfo | null> {
    const room = await this.ensureLargeRoom();
    if (room.playerIds.includes(playerId)) return room;
    room.playerIds.push(playerId);
    if (room.playerIds.length >= room.maxPlayers) {
      room.status = 'STARTING';
      await this.redis.lrem(LARGE_ROOMS_KEY, 1, room.roomId);
      await this.spawnLargeRoom(); // pre-create next one
    }
    await this.saveRoom(room);
    return room;
  }

  // ── CUSTOM rooms (player-created) ────────────────────────────────────────

  async createCustomRoom(
    creatorPlayerId: PlayerId,
    mapId:      string,
    maxPlayers: number,
    gameMode:   GameMode,
  ): Promise<RoomInfo> {
    const room = this.buildRoom({
      type:            RoomType.CUSTOM,
      gameMode,
      maxPlayers:      Math.max(2, Math.min(maxPlayers, 20)),
      mapId,
      creatorPlayerId,
    });
    room.playerIds.push(creatorPlayerId);
    await this.saveRoom(room);
    return room;
  }

  async joinRoom(playerId: PlayerId, roomId: string): Promise<RoomInfo | null> {
    const room = await this.getRoom(roomId);
    if (!room || room.status !== 'WAITING') return null;
    if (room.playerIds.length >= room.maxPlayers) return null;
    if (room.playerIds.includes(playerId)) return room;
    room.playerIds.push(playerId);
    if (room.playerIds.length >= room.maxPlayers) room.status = 'STARTING';
    await this.saveRoom(room);
    return room;
  }

  // Creator explicitly starts the game (for CUSTOM rooms)
  async startRoom(roomId: string, requesterId: PlayerId): Promise<RoomInfo | { error: string }> {
    const room = await this.getRoom(roomId);
    if (!room)                                   return { error: 'Room not found' };
    if (room.type !== RoomType.CUSTOM)            return { error: 'Only custom rooms can be manually started' };
    if (room.creatorPlayerId !== requesterId)     return { error: 'Only the room creator can start the game' };
    if (room.status !== 'WAITING')                return { error: 'Room is not in waiting state' };
    if (room.playerIds.length < 2)                return { error: 'Need at least 2 players' };

    room.status = 'STARTING';

    if (room.gameMode === GameMode.TEAM) {
      room.teams = assignTeams(room.playerIds);
    }

    await this.saveRoom(room);
    return room;
  }

  async leaveRoom(playerId: PlayerId, roomId: string): Promise<void> {
    const room = await this.getRoom(roomId);
    if (!room) return;
    room.playerIds = room.playerIds.filter(id => id !== playerId);
    if (room.playerIds.length === 0) {
      await this.redis.del(`room:${roomId}`);
      await this.redis.lrem('rooms:list', 0, roomId);
    } else {
      // Transfer ownership if creator left
      if (room.creatorPlayerId === playerId && room.type === RoomType.CUSTOM) {
        room.creatorPlayerId = room.playerIds[0];
      }
      await this.saveRoom(room);
    }
  }

  async setStatus(roomId: string, status: RoomStatus): Promise<void> {
    const room = await this.getRoom(roomId);
    if (!room) return;
    room.status = status;
    await this.saveRoom(room);
  }

  async getRoom(roomId: string): Promise<RoomInfo | null> {
    const raw = await this.redis.get(`room:${roomId}`);
    if (!raw) return null;
    return JSON.parse(raw) as RoomInfo;
  }

  async listCustomRooms(): Promise<RoomInfo[]> {
    const ids = await this.redis.lrange('rooms:list', 0, -1);
    const rooms = await Promise.all(ids.map((id: string) => this.getRoom(id)));
    return rooms.filter(
      (r: RoomInfo | null): r is RoomInfo => r !== null && r.type === RoomType.CUSTOM && r.status === 'WAITING',
    );
  }

  async listLargeRooms(): Promise<RoomInfo[]> {
    const ids = await this.redis.lrange(LARGE_ROOMS_KEY, 0, -1);
    const rooms = await Promise.all(ids.map((id: string) => this.getRoom(id)));
    return rooms.filter((r: RoomInfo | null): r is RoomInfo => r !== null && r.status === 'WAITING');
  }

  // ── Internal ──────────────────────────────────────────────────────────────

  private buildRoom(opts: {
    type:            RoomType;
    gameMode:        GameMode;
    maxPlayers:      number;
    mapId:           string;
    creatorPlayerId: PlayerId | null;
  }): RoomInfo {
    return {
      roomId:           `room-${roomSeq++}-${Date.now()}`,
      type:             opts.type,
      status:           'WAITING',
      gameMode:         opts.gameMode,
      playerIds:        [],
      maxPlayers:       opts.maxPlayers,
      mapId:            opts.mapId,
      teams:            [],
      creatorPlayerId:  opts.creatorPlayerId,
      battleServerHost: process.env.BATTLE_SERVER_HOST ?? 'localhost',
      battleServerPort: Number(process.env.UDP_PORT_START ?? 5000),
      createdAt:        Date.now(),
    };
  }

  private async saveRoom(room: RoomInfo): Promise<void> {
    const key    = `room:${room.roomId}`;
    const exists = await this.redis.exists(key);
    await this.redis.setex(key, ROOM_TTL, JSON.stringify(room));
    // Only track CUSTOM rooms in the general list
    if (!exists && room.type === RoomType.CUSTOM) {
      await this.redis.rpush('rooms:list', room.roomId);
    }
  }
}

function assignTeams(playerIds: PlayerId[]): Team[] {
  const shuffled = [...playerIds].sort(() => Math.random() - 0.5);
  const mid      = Math.ceil(shuffled.length / 2);
  return [
    { teamId: 1, playerIds: shuffled.slice(0, mid) },
    { teamId: 2, playerIds: shuffled.slice(mid) },
  ];
}
