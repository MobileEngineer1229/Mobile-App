import Redis from 'ioredis';
import { connectMongo } from '../../../db/mongo/connection';
import { seedCharacters } from '../../../db/mongo/seed/characters';
import { createLoginApp } from './server';

const redis = new Redis({
  host: process.env.REDIS_HOST ?? 'localhost',
  port: Number(process.env.REDIS_PORT ?? 6379),
  lazyConnect: true,
});

async function bootstrap(): Promise<void> {
  await redis.connect();
  await connectMongo();
  await seedCharacters(); // idempotent — skips existing templates

  createLoginApp(redis);
}

bootstrap().catch(err => {
  console.error('[Login] Bootstrap failed:', err);
  process.exit(1);
});

process.on('SIGTERM', () => process.exit(0));
process.on('SIGINT',  () => process.exit(0));
