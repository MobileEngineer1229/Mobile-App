import Redis from 'ioredis';
import { connectMongo } from '../../../db/mongo/connection';
import { BattleServer } from './ws-server';
import { Logger } from '../../../shared/logger';
import { TerrainType, GameMode } from '../../../shared/types';
import type { MapData } from '../../../shared/types';

const WS_PORT = Number(process.env.PORT ?? 5000);
const log = new Logger('Battle');

const redis = new Redis({
  host: process.env.REDIS_HOST ?? 'localhost',
  port: Number(process.env.REDIS_PORT ?? 6379),
  lazyConnect: true,
});

redis.on('connect', () => log.info('Redis connected', { host: process.env.REDIS_HOST ?? 'localhost' }));
redis.on('error',   (err: Error) => log.error('Redis error', err));

// ── Map builders ───────────────────────────────────────────────────────────────

const G = TerrainType.GRASS;
const D = TerrainType.DESTRUCTIBLE;
const I = TerrainType.INDESTRUCTIBLE;
const F = TerrainType.FOREST;
const S = TerrainType.SNOW;
const W = TerrainType.WATER;
const M = TerrainType.MOUNTAIN;

function fillBorder(tiles: TerrainType[][], rows: number, cols: number, t: TerrainType): void {
  for (let x = 0; x < cols; x++) { tiles[0][x] = t; tiles[rows - 1][x] = t; }
  for (let y = 0; y < rows; y++) { tiles[y][0] = t; tiles[y][cols - 1] = t; }
}

function indestructPillars(tiles: TerrainType[][], rows: number, cols: number): void {
  for (let y = 2; y < rows - 2; y += 2)
    for (let x = 2; x < cols - 2; x += 2)
      tiles[y][x] = I;
}

function clearSpawns(tiles: TerrainType[][], spawns: { x: number; y: number }[]): void {
  for (const sp of spawns) {
    for (let dy = -1; dy <= 1; dy++)
      for (let dx = -1; dx <= 1; dx++)
        if (tiles[sp.y + dy]?.[sp.x + dx] === D || tiles[sp.y + dy]?.[sp.x + dx] === F)
          tiles[sp.y + dy][sp.x + dx] = G;
  }
}

/** 37×29 classic Bomberman-style map — used for most themes */
function buildClassic(mapId: string, name: string, base: TerrainType, density = 0.4): MapData {
  const COLS = 37, ROWS = 29;
  const tiles: TerrainType[][] = Array.from({ length: ROWS }, () => Array(COLS).fill(base));

  fillBorder(tiles, ROWS, COLS, I);
  indestructPillars(tiles, ROWS, COLS);

  // Random destructible blocks
  for (let y = 1; y < ROWS - 1; y++)
    for (let x = 1; x < COLS - 1; x++)
      if (tiles[y][x] === base && Math.random() < density)
        tiles[y][x] = D;

  const spawnPoints = [
    { x: 1,        y: 1       },
    { x: COLS - 2, y: 1       },
    { x: 1,        y: ROWS - 2},
    { x: COLS - 2, y: ROWS - 2},
    { x: Math.floor(COLS / 2), y: 1       },
    { x: Math.floor(COLS / 2), y: ROWS - 2},
    { x: 1,        y: Math.floor(ROWS / 2)},
    { x: COLS - 2, y: Math.floor(ROWS / 2)},
  ];
  clearSpawns(tiles, spawnPoints);
  return { mapId, name, width: COLS, height: ROWS, tiles, spawnPoints };
}

/** Large 60×45 map for large rooms (100+ players) */
function buildLarge(mapId: string, name: string, primary: TerrainType, accent: TerrainType): MapData {
  const COLS = 60, ROWS = 45;
  const tiles: TerrainType[][] = Array.from({ length: ROWS }, () => Array(COLS).fill(G));

  fillBorder(tiles, ROWS, COLS, I);
  indestructPillars(tiles, ROWS, COLS);

  for (let y = 1; y < ROWS - 1; y++)
    for (let x = 1; x < COLS - 1; x++) {
      if (tiles[y][x] === I) continue;
      const r = Math.random();
      if (r < 0.30) tiles[y][x] = D;
      else if (r < 0.40) tiles[y][x] = primary;
      else if (r < 0.45) tiles[y][x] = accent;
    }

  // 12 spawn points spread around edges
  const spawnPoints = [
    { x: 1,        y: 1       }, { x: COLS - 2, y: 1       },
    { x: 1,        y: ROWS - 2}, { x: COLS - 2, y: ROWS - 2},
    { x: 15,       y: 1       }, { x: 45,       y: 1       },
    { x: 15,       y: ROWS - 2}, { x: 45,       y: ROWS - 2},
    { x: 1,        y: 15      }, { x: COLS - 2, y: 15      },
    { x: 1,        y: 30      }, { x: COLS - 2, y: 30      },
  ];
  clearSpawns(tiles, spawnPoints);
  return { mapId, name, width: COLS, height: ROWS, tiles, spawnPoints };
}

function buildMap(mapId: string): MapData {
  switch (mapId) {
    case 'volcano-isle':  return buildLarge('volcano-isle',  'Volcano Isle',  M, G);
    case 'frozen-tundra': return buildLarge('frozen-tundra', 'Frozen Tundra', S, W);
    case 'desert-ruins':  return buildLarge('desert-ruins',  'Desert Ruins',  G, D);
    case 'jungle-maze':   return buildLarge('jungle-maze',   'Jungle Maze',   F, G);
    case 'city-ruins':    return buildLarge('city-ruins',    'City Ruins',    G, D);
    default:              return buildClassic(mapId, 'Dev Arena', G, 0.35);
  }
}

async function getMode(roomId: string): Promise<GameMode> {
  try {
    const raw = await redis.get(`room:${roomId}`);
    if (raw) return (JSON.parse(raw) as any).gameMode ?? GameMode.INDIVIDUAL;
  } catch {}
  return GameMode.INDIVIDUAL;
}

// ── Bootstrap ──────────────────────────────────────────────────────────────────

async function bootstrap(): Promise<void> {
  await redis.connect();
  await connectMongo();

  const server = new BattleServer(WS_PORT, redis, buildMap, getMode);
  server.start();

  process.on('SIGTERM', () => { log.warn('SIGTERM — shutting down'); process.exit(0); });
  process.on('SIGINT',  () => { log.warn('SIGINT — shutting down');  process.exit(0); });
}

bootstrap().catch(err => {
  log.error('Bootstrap failed', err);
  process.exit(1);
});
