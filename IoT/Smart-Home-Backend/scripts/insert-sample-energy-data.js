/**
 * Script to insert sample energy consumption data for testing
 * Usage: node scripts/insert-sample-energy-data.js [deviceId] [days]
 */

const { Pool } = require('pg');
require('dotenv').config();

const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432', 10),
  database: process.env.DB_NAME || 'smart_home_db',
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || '213515',
});

async function insertSampleData(deviceId, days = 30) {
  console.log(`Inserting ${days} days of sample energy data for device ${deviceId}...`);
  
  const today = new Date();
  let inserted = 0;
  let skipped = 0;

  for (let i = 0; i < days; i++) {
    const date = new Date(today);
    date.setDate(date.getDate() - i);
    const dateStr = date.toISOString().split('T')[0];
    
    // Random consumption between 0.5-5 kWh (realistic range)
    const consumptionKwh = parseFloat((Math.random() * 4.5 + 0.5).toFixed(2));
    const costUsd = parseFloat((consumptionKwh * 0.15).toFixed(2)); // $0.15 per kWh
    
    try {
      const result = await pool.query(
        `INSERT INTO energy_consumption (device_id, date, consumption_kwh, cost_usd)
         VALUES ($1, $2, $3, $4)
         ON CONFLICT (device_id, date) DO UPDATE SET
           consumption_kwh = EXCLUDED.consumption_kwh,
           cost_usd = EXCLUDED.cost_usd,
           updated_at = CURRENT_TIMESTAMP
         RETURNING id`,
        [deviceId, dateStr, consumptionKwh, costUsd]
      );
      
      if (result.rows.length > 0) {
        inserted++;
        console.log(`✓ ${dateStr}: ${consumptionKwh} kWh, $${costUsd}`);
      } else {
        skipped++;
      }
    } catch (error) {
      console.error(`✗ Error inserting data for ${dateStr}:`, error.message);
      skipped++;
    }
  }

  console.log(`\nCompleted: ${inserted} records inserted, ${skipped} skipped`);
}

async function main() {
  const deviceId = parseInt(process.argv[2] || '1', 10);
  const days = parseInt(process.argv[3] || '30', 10);

  if (isNaN(deviceId)) {
    console.error('Error: Invalid device ID');
    console.log('Usage: node scripts/insert-sample-energy-data.js [deviceId] [days]');
    process.exit(1);
  }

  try {
    // Check if device exists
    const deviceCheck = await pool.query('SELECT id FROM devices WHERE id = $1', [deviceId]);
    if (deviceCheck.rows.length === 0) {
      console.error(`Error: Device ${deviceId} not found`);
      console.log('Available devices:');
      const devices = await pool.query('SELECT id, name FROM devices LIMIT 10');
      devices.rows.forEach(d => console.log(`  - Device ${d.id}: ${d.name}`));
      process.exit(1);
    }

    await insertSampleData(deviceId, days);
  } catch (error) {
    console.error('Error:', error.message);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

main();

