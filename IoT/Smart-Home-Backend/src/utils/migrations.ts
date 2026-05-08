import { Pool } from 'pg';
import { readFileSync } from 'fs';
import { join } from 'path';
import logger from './logger';

/**
 * Migration record interface
 */
interface MigrationRecord {
  id: number;
  name: string;
  executed_at: Date;
}

/**
 * Run database migrations automatically
 */
export async function runMigrations(pool: Pool): Promise<void> {
  try {
    // Create migrations table if it doesn't exist
    await pool.query(`
      CREATE TABLE IF NOT EXISTS migrations (
        id SERIAL PRIMARY KEY,
        name VARCHAR(255) NOT NULL UNIQUE,
        executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // Get list of executed migrations
    const executedResult = await pool.query<MigrationRecord>(
      'SELECT name FROM migrations ORDER BY executed_at ASC'
    );
    const executedMigrations = new Set(executedResult.rows.map((r) => r.name));

    // Define migrations in order
    const migrations = [
      {
        name: 'migration_add_rooms',
        file: 'migration_add_rooms.sql',
      },
      {
        name: 'migration_add_password_reset',
        file: 'migration_add_password_reset.sql',
      },
      {
        name: 'migration_add_energy_consumption',
        file: 'migration_add_energy_consumption.sql',
      },
      {
        name: 'migration_add_notifications',
        file: 'migration_add_notifications.sql',
      },
      {
        name: 'migration_add_chatbot',
        file: 'migration_add_chatbot.sql',
      },
      {
        name: 'migration_add_voice_assistants',
        file: 'migration_add_voice_assistants.sql',
      },
      {
        name: 'migration_add_user_settings',
        file: 'migration_add_user_settings.sql',
      },
      {
        name: 'migration_add_linked_accounts',
        file: 'migration_add_linked_accounts.sql',
      },
      {
        name: 'migration_add_additional_settings',
        file: 'migration_add_additional_settings.sql',
      },
      {
        name: 'migration_consolidate_user_settings',
        file: 'migration_consolidate_user_settings.sql',
      },
      {
        name: 'migration_add_homes',
        file: 'migration_add_homes.sql',
      },
      {
        name: 'migration_add_home_id_to_rooms',
        file: 'migration_add_home_id_to_rooms.sql',
      },
      {
        name: 'migration_add_notification_category',
        file: 'migration_add_notification_category.sql',
      },
      {
        name: 'migration_separate_user_settings',
        file: 'migration_separate_user_settings.sql',
      },
      {
        name: 'migration_add_user_actions',
        file: 'migration_add_user_actions.sql',
      },
      {
        name: 'migration_add_home_members',
        file: 'migration_add_home_members.sql',
      },
      {
        name: 'migration_add_home_invitations',
        file: 'migration_add_home_invitations.sql',
      },
      {
        name: 'migration_add_app_versions',
        file: 'migration_add_app_versions.sql',
      },
      {
        name: 'migration_update_device_types',
        file: 'migration_update_device_types.sql',
      },
      {
        name: 'migration_extend_device_type_enum',
        file: 'migration_extend_device_type_enum.sql',
      },
      {
        name: 'migration_add_device_types_table',
        file: 'migration_add_device_types_table.sql',
      },
      {
        name: 'migration_update_device_types_default_type',
        file: 'migration_update_device_types_default_type.sql',
      },
      {
        name: 'migration_add_home_id_to_devices',
        file: 'migration_add_home_id_to_devices.sql',
      },
      {
        name: 'migration_update_devices_type',
        file: 'migration_update_devices_type.sql',
      },
      {
        name: 'migration_add_demo_notifications',
        file: 'migration_add_demo_notifications.sql',
      },
      {
        name: 'migration_add_demo_home_members',
        file: 'migration_add_demo_home_members.sql',
      },
      {
        name: 'migration_mqtt_device_support',
        file: 'migration_mqtt_device_support.sql',
      },
      {
        name: 'migration_add_device_states_and_commands',
        file: 'migration_add_device_states_and_commands.sql',
      },
      {
        name: 'migration_add_scene_order_and_logs',
        file: 'migration_add_scene_order_and_logs.sql',
      },
      // Add more migrations here as needed
    ];

    // Run pending migrations
    for (const migration of migrations) {
      if (executedMigrations.has(migration.name)) {
        logger.infoWithEmoji('✓', `Migration already applied: ${migration.name}`, 'MIGRATION');
        continue;
      }

      try {
        logger.infoWithEmoji('🔄', `Running migration: ${migration.name}`, 'MIGRATION');

        // Read migration file (works in both dev and production)
        // In dev: __dirname = src/utils, go up to root, then database/
        // In prod: __dirname = dist/utils, go up to root, then database/
        const migrationPath = join(process.cwd(), 'database', migration.file);
        const migrationSQL = readFileSync(migrationPath, 'utf-8');

        // Run migration in a transaction
        const client = await pool.connect();
        try {
          await client.query('BEGIN');

          // Execute migration SQL
          await client.query(migrationSQL);

          // Record migration
          await client.query('INSERT INTO migrations (name) VALUES ($1)', [migration.name]);

          await client.query('COMMIT');
          logger.infoWithEmoji('✅', `Migration completed: ${migration.name}`, 'MIGRATION');
        } catch (error) {
          await client.query('ROLLBACK');
          throw error;
        } finally {
          client.release();
        }
      } catch (error) {
        logger.errorWithEmoji('❌', `Migration failed: ${migration.name}`, 'MIGRATION', {
          error: error instanceof Error ? error.message : String(error),
        });
        throw error;
      }
    }

    logger.infoWithEmoji('✨', 'All migrations completed', 'MIGRATION');
  } catch (error) {
    logger.errorWithEmoji('❌', 'Migration process failed', 'MIGRATION', {
      error: error instanceof Error ? error.message : String(error),
    });
    throw error;
  }
}

