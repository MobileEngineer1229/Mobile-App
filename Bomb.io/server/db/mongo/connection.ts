import mongoose from 'mongoose';

const MONGO_URI = process.env.MONGO_URI ?? 'mongodb://localhost:27017/bombio';

let connected = false;

export async function connectMongo(): Promise<void> {
  if (connected) return;

  mongoose.connection.on('connected',    () => console.log('[MongoDB] Connected'));
  mongoose.connection.on('disconnected', () => { console.warn('[MongoDB] Disconnected'); connected = false; });
  mongoose.connection.on('error',        (err) => console.error('[MongoDB] Error:', err));

  await mongoose.connect(MONGO_URI, {
    serverSelectionTimeoutMS: 5000,
    socketTimeoutMS: 45000,
  });

  connected = true;
}

export { mongoose };
