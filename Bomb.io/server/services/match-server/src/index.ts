import Redis from 'ioredis';
import { RoomManager } from './room-manager';
import { Matchmaker } from './matchmaker';
import { createMatchApp } from './lobby-handler';
import { Logger } from '../../../shared/logger';

const log = new Logger('Match');

const LARGE_MAPS = ['volcano-isle', 'frozen-tundra', 'desert-ruins', 'jungle-maze', 'city-ruins'];

const redis = new Redis({
  host: process.env.REDIS_HOST ?? 'localhost',
  port: Number(process.env.REDIS_PORT ?? 6379),
});

redis.on('connect', () => log.info('Redis connected', { host: process.env.REDIS_HOST ?? 'localhost' }));
redis.on('error', (err: Error) => log.error('Redis connection error', err));

const rooms    = new RoomManager(redis);
const matchmaker = new Matchmaker(redis, rooms);
createMatchApp(rooms, matchmaker);

// Pre-create 5 named LARGE rooms — clears stale list first
async function seedLargeRooms(): Promise<void> {
  log.info('Seeding large rooms…');

  // Remove stale room IDs whose Redis keys have expired
  const ids: string[] = await redis.lrange('rooms:large:waiting', 0, -1);
  let cleared = 0;
  for (const id of ids) {
    const exists = await redis.exists(`room:${id}`);
    if (!exists) { await redis.lrem('rooms:large:waiting', 0, id); cleared++; }
  }
  if (cleared) log.warn(`Cleared ${cleared} stale room IDs from list`);

  for (const mapId of LARGE_MAPS) {
    const existing = await rooms.listLargeRooms();
    const hasMap   = existing.some(r => r.mapId === mapId);
    if (!hasMap) {
      const room = await (rooms as any).spawnLargeRoomWithMap(mapId);
      log.ok(`Large room created`, { map: mapId, roomId: room.roomId });
    } else {
      log.info(`Large room exists`, { map: mapId });
    }
  }

  log.ok('All 5 large rooms ready 🗺️');
}

seedLargeRooms().catch(err => log.error('Seed failed', err));

process.on('SIGTERM', () => { log.warn('SIGTERM — shutting down'); process.exit(0); });
process.on('SIGINT',  () => { log.warn('SIGINT — shutting down');  process.exit(0); });
