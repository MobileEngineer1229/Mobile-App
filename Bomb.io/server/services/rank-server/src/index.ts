import Redis from 'ioredis';
import { connectMongo } from '../../../db/mongo/connection';
import { Logger, withLogging } from '../../../shared/logger';
import { Leaderboard } from './leaderboard';
import { StatsTracker } from './stats-tracker';

const PORT = Number(process.env.PORT ?? 8002);
const log  = new Logger('Rank');

const CORS = {
  'Content-Type': 'application/json',
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type,Authorization',
};

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: CORS });
}

const redis = new Redis({
  host: process.env.REDIS_HOST ?? 'localhost',
  port: Number(process.env.REDIS_PORT ?? 6379),
  lazyConnect: true,
});

redis.on('connect', () => log.info('Redis connected', { host: process.env.REDIS_HOST ?? 'localhost' }));
redis.on('error', (err: Error) => log.error('Redis error', err));

async function bootstrap(): Promise<void> {
  await redis.connect();
  await connectMongo();
  log.info('MongoDB connected');

  const leaderboard = new Leaderboard(redis);
  const tracker     = new StatsTracker(redis, leaderboard);

  async function handler(req: Request): Promise<Response> {
    if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: CORS });

    const { pathname } = new URL(req.url);

    try {
      if (req.method === 'GET' && pathname === '/leaderboard') {
        const data = await leaderboard.getTopPlayers(50);
        log.info('Leaderboard fetched', { entries: data.length });
        return json(200, { leaderboard: data });
      }

      const statsM = pathname.match(/^\/players\/([^/]+)\/stats$/);
      if (req.method === 'GET' && statsM) {
        const playerId = statsM[1];
        const [stats, rank, history] = await Promise.all([
          leaderboard.getStats(playerId),
          leaderboard.getRank(playerId),
          tracker.getHistory(playerId),
        ]);
        log.info('Player stats fetched', { playerId, rank: rank ?? 'unranked' });
        return json(200, { stats, rank, history });
      }

      // Internal endpoint called by battle-server to record match results
      if (req.method === 'POST' && pathname === '/match-result') {
        const body = await req.json() as any;
        const { roomId, results } = body; // results: [{playerId, kills, deaths, won}]
        log.gameStart(roomId, results?.length ?? 0);
        const winnerId = body.winnerId ?? results?.find((r: any) => r.won)?.playerId;
        if (winnerId && Array.isArray(results)) {
          const before = await leaderboard.getStats(String(winnerId));
          await leaderboard.recordResult(String(winnerId), results);
          const after = await leaderboard.getStats(String(winnerId));
          log.rank(String(winnerId), after.elo, after.elo - before.elo);
        }
        return json(200, { ok: true });
      }

      return json(404, { error: 'Not found' });

    } catch (err) {
      log.error(`Handler crashed: ${pathname}`, err);
      return json(500, { error: 'Server error' });
    }
  }

  (globalThis as any).Bun.serve({
    port: PORT,
    fetch: withLogging(log, async (req) => handler(req)),
  });

  log.start(PORT, { mongo: 'connected', redis: 'connected' });
}

bootstrap().catch(err => {
  log.error('Bootstrap failed', err);
  process.exit(1);
});

process.on('SIGTERM', () => { log.warn('SIGTERM received — shutting down'); process.exit(0); });
process.on('SIGINT',  () => { log.warn('SIGINT received — shutting down');  process.exit(0); });
