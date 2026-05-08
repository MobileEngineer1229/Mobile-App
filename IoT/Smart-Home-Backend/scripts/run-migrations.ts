/**
 * Standalone Migration Script
 * Run this script to execute database migrations manually
 * 
 * Usage:
 *   ts-node scripts/run-migrations.ts
 *   or
 *   npm run migrate
 */

import { getPool, testConnection } from '../src/config/database';
import { runMigrations } from '../src/utils/migrations';
import logger from '../src/utils/logger';

async function main() {
  try {
    logger.infoWithEmoji('🚀', 'Starting database migration...', 'MIGRATION');

    // Test database connection
    await testConnection();
    logger.infoWithEmoji('✅', 'Database connection successful', 'MIGRATION');

    // Get database pool
    const pool = getPool();

    // Run migrations
    await runMigrations(pool);

    logger.infoWithEmoji('✨', 'Migration process completed successfully', 'MIGRATION');
    process.exit(0);
  } catch (error) {
    logger.errorWithEmoji('❌', 'Migration process failed', 'MIGRATION', {
      error: error instanceof Error ? error.message : String(error),
    });
    process.exit(1);
  }
}

// Run the migration
main();
