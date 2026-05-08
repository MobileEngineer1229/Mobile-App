import { Pool } from 'pg';
import dotenv from 'dotenv';
import logger from '../utils/logger';

dotenv.config();

interface DatabaseConfig {
  host: string;
  port: number;
  user: string;
  password: string;
  database: string;
  max: number;
  idleTimeoutMillis: number;
  connectionTimeoutMillis: number;
}

const dbConfig: DatabaseConfig = {
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432', 5432),
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || '213515',
  database: process.env.DB_NAME || 'smart_home_db',
  max: 10,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
};

const pool = new Pool(dbConfig);

/**
 * Get database connection pool
 * @returns PostgreSQL connection pool
 */
export const getPool = (): Pool => {
  return pool;
};

/**
 * Test database connection
 */
export const testConnection = async (): Promise<void> => {
  try {
    const client = await pool.connect();
    logger.db.connect('Database connected successfully', {
      host: dbConfig.host,
      port: dbConfig.port,
      database: dbConfig.database,
    });
    client.release();
  } catch (error) {
    logger.db.error('Database connection failed', {
      error: error instanceof Error ? error.message : String(error),
      host: dbConfig.host,
      port: dbConfig.port,
      database: dbConfig.database,
    });
    throw error;
  }
};

export default pool;

