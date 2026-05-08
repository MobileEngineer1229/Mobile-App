const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
require('dotenv').config();

/**
 * Script to fix demo user passwords on remote database
 * 
 * Usage:
 * 1. Set environment variables for remote database:
 *    DB_HOST=172.86.88.76
 *    DB_PORT=5432
 *    DB_USER=postgres
 *    DB_PASSWORD=your_password
 *    DB_NAME=smart_home_db
 * 
 * 2. Run: node scripts/fix-remote-demo-passwords.js
 */

const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432', 10),
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || '213515',
  database: process.env.DB_NAME || 'smart_home_db',
});

async function fixRemoteDemoPasswords() {
  const client = await pool.connect();
  try {
    console.log('🔗 Connecting to database:');
    console.log('   Host:', process.env.DB_HOST || 'localhost');
    console.log('   Port:', process.env.DB_PORT || '5432');
    console.log('   Database:', process.env.DB_NAME || 'smart_home_db');
    console.log('');

    const password = 'password123';
    console.log('🔐 Generating bcrypt hash for password:', password);
    
    // Generate proper bcrypt hash
    const hashedPassword = await bcrypt.hash(password, 10);
    console.log('✅ Generated hash:', hashedPassword);
    console.log('');

    // Get all demo users
    const result = await client.query(
      `SELECT id, email, first_name, last_name, password
       FROM users 
       WHERE email LIKE '%@example.com' 
       ORDER BY email`
    );

    if (result.rows.length === 0) {
      console.log('⚠️  No demo users found with @example.com email');
      return;
    }

    console.log(`📋 Found ${result.rows.length} demo users:`);
    result.rows.forEach((user, index) => {
      const hashPreview = user.password.substring(0, 20) + '...';
      const isValidHash = user.password.length === 60 && 
                         (user.password.startsWith('$2a$') || 
                          user.password.startsWith('$2b$') || 
                          user.password.startsWith('$2y$'));
      console.log(`   ${index + 1}. ${user.email}`);
      console.log(`      Current hash: ${hashPreview}`);
      console.log(`      Hash valid: ${isValidHash ? '✅' : '❌'}`);
    });
    console.log('');

    // Update all demo users with the new password hash
    console.log('🔄 Updating passwords...');
    const updateResult = await client.query(
      `UPDATE users 
       SET password = $1, updated_at = CURRENT_TIMESTAMP 
       WHERE email LIKE '%@example.com'`,
      [hashedPassword]
    );

    console.log(`✅ Updated ${updateResult.rowCount} demo user(s) with new password hash`);
    console.log('');

    // Verify the update worked
    const verifyResult = await client.query(
      `SELECT email, password FROM users WHERE email = 'john.doe@example.com'`
    );
    
    if (verifyResult.rows.length > 0) {
      const testPassword = 'password123';
      const isValid = await bcrypt.compare(testPassword, verifyResult.rows[0].password);
      if (isValid) {
        console.log('✅ Verification: Password hash is valid and working!');
      } else {
        console.log('❌ Verification: Password hash verification failed!');
      }
    }

    console.log('');
    console.log('🔑 Login Credentials:');
    console.log('   Email: john.doe@example.com (or any @example.com user)');
    console.log('   Password: password123');
    console.log('');
    console.log('📝 Test with Postman:');
    console.log('   POST http://172.86.88.76:3003/api/v1/users/login');
    console.log('   Body: {');
    console.log('     "email": "john.doe@example.com",');
    console.log('     "password": "password123"');
    console.log('   }');

  } catch (error) {
    console.error('❌ Error:', error.message);
    if (error.code === 'ECONNREFUSED') {
      console.error('   Cannot connect to database. Check:');
      console.error('   - Database server is running');
      console.error('   - DB_HOST, DB_PORT are correct');
      console.error('   - Firewall allows connection');
    } else if (error.code === '28P01') {
      console.error('   Authentication failed. Check:');
      console.error('   - DB_USER and DB_PASSWORD are correct');
    }
    throw error;
  } finally {
    client.release();
    await pool.end();
  }
}

fixRemoteDemoPasswords()
  .then(() => {
    console.log('');
    console.log('✅ Script completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('');
    console.error('❌ Script failed:', error.message);
    process.exit(1);
  });
