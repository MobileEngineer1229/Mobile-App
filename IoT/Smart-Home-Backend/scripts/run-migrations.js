/**
 * Standalone Migration Script (JavaScript version for production)
 * Run this script to execute database migrations manually
 * 
 * Usage:
 *   node scripts/run-migrations.js
 *   or
 *   npm run migrate:prod
 */

const { getPool, testConnection } = require('../dist/config/database');
const { runMigrations } = require('../dist/utils/migrations');
const logger = require('../dist/utils/logger').default;

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
