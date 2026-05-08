/**
 * Script to update devices table with home_id
 * Updates all devices to have home_id set based on their room or user's primary home
 * 
 * Usage:
 *   ts-node scripts/update-devices-home-id.ts
 */

import { getPool, testConnection } from '../src/config/database';
import logger from '../src/utils/logger';

async function updateDevicesHomeId() {
  const pool = getPool();
  const client = await pool.connect();

  try {
    logger.infoWithEmoji('🚀', 'Starting device home_id update...', 'UPDATE');

    // Test database connection
    await testConnection();
    logger.infoWithEmoji('✅', 'Database connection successful', 'UPDATE');

    // Step 0: First ensure rooms have home_id set (from user's primary home)
    logger.infoWithEmoji('🔄', 'Ensuring rooms have home_id...', 'UPDATE');
    const updateRooms = await client.query(`
      UPDATE rooms r
      SET home_id = (
        SELECT h.id 
        FROM homes h 
        WHERE h.user_id = r.user_id 
        AND h.is_primary = true 
        LIMIT 1
      )
      WHERE r.home_id IS NULL
        AND EXISTS (
          SELECT 1 FROM homes h 
          WHERE h.user_id = r.user_id 
          AND h.is_primary = true
        )
    `);
    logger.infoWithEmoji('✅', `Updated ${updateRooms.rowCount} room(s) with home_id`, 'UPDATE');

    // Step 1: Update devices with rooms - set home_id from room's home_id
    logger.infoWithEmoji('🔄', 'Updating devices with rooms...', 'UPDATE');
    const updateWithRooms = await client.query(`
      UPDATE devices d
      SET home_id = r.home_id
      FROM rooms r
      WHERE d.room_id = r.id
        AND (d.home_id IS NULL OR d.home_id != r.home_id)
        AND r.home_id IS NOT NULL
    `);
    logger.infoWithEmoji('✅', `Updated ${updateWithRooms.rowCount} device(s) from rooms`, 'UPDATE');

    // Step 2: Update devices without rooms - set to user's primary home
    logger.infoWithEmoji('🔄', 'Updating devices without rooms...', 'UPDATE');
    const updateWithoutRooms = await client.query(`
      UPDATE devices d
      SET home_id = (
        SELECT h.id 
        FROM homes h 
        WHERE h.user_id = d.user_id 
        AND h.is_primary = true 
        LIMIT 1
      )
      WHERE d.home_id IS NULL
        AND d.room_id IS NULL
        AND EXISTS (
          SELECT 1 FROM homes h 
          WHERE h.user_id = d.user_id 
          AND h.is_primary = true
        )
    `);
    logger.infoWithEmoji('✅', `Updated ${updateWithoutRooms.rowCount} device(s) without rooms`, 'UPDATE');

    // Step 3: Get statistics
    const stats = await client.query(`
      SELECT 
        COUNT(*) as total_devices,
        COUNT(home_id) as devices_with_home_id,
        COUNT(*) - COUNT(home_id) as devices_without_home_id
      FROM devices
    `);

    const statsRow = stats.rows[0];
    logger.infoWithEmoji('📊', 'Device Statistics:', 'UPDATE', {
      total: statsRow.total_devices,
      withHomeId: statsRow.devices_with_home_id,
      withoutHomeId: statsRow.devices_without_home_id,
    });

    // Step 4: Show devices still without home_id (if any)
    if (parseInt(statsRow.devices_without_home_id, 10) > 0) {
      const devicesWithoutHome = await client.query(`
        SELECT d.id, d.name, d.user_id, u.email, d.room_id
        FROM devices d
        JOIN users u ON d.user_id = u.id
        WHERE d.home_id IS NULL
        ORDER BY d.user_id, d.id
      `);

      logger.infoWithEmoji('⚠️', `Found ${devicesWithoutHome.rows.length} device(s) without home_id:`, 'UPDATE');
      devicesWithoutHome.rows.forEach((device) => {
        logger.info('', `  - Device ID: ${device.id}, Name: ${device.name}, User: ${device.email}, Room ID: ${device.room_id}`, 'UPDATE');
      });
    }

    logger.infoWithEmoji('✨', 'Device home_id update completed successfully!', 'UPDATE');
  } catch (error) {
    logger.errorWithEmoji('❌', 'Device home_id update failed', 'UPDATE', {
      error: error instanceof Error ? error.message : String(error),
    });
    throw error;
  } finally {
    client.release();
    await pool.end();
  }
}

// Run the update
updateDevicesHomeId()
  .then(() => {
    process.exit(0);
  })
  .catch((error) => {
    console.error('Error:', error);
    process.exit(1);
  });
