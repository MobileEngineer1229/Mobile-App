const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432', 10),
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || '213515',
  database: process.env.DB_NAME || 'smart_home_db',
});

async function testLogin() {
  const client = await pool.connect();
  try {
    const email = 'john.doe@example.com';
    const testPasswords = ['password123', 'password123!', 'Password123', 'PASSWORD123'];
    
    console.log('🔍 Testing login for:', email);
    console.log('');

    // Get user from database
    const result = await client.query(
      `SELECT id, email, password, first_name, last_name 
       FROM users 
       WHERE email = $1`,
      [email]
    );

    if (result.rows.length === 0) {
      console.log('❌ User not found:', email);
      return;
    }

    const user = result.rows[0];
    console.log('✅ User found:');
    console.log('   ID:', user.id);
    console.log('   Email:', user.email);
    console.log('   Name:', user.first_name, user.last_name);
    console.log('   Password Hash:', user.password);
    console.log('   Hash Length:', user.password.length);
    console.log('   Hash Prefix:', user.password.substring(0, 7));
    console.log('');

    // Test different password variations
    console.log('🧪 Testing password variations:');
    console.log('');
    
    for (const testPassword of testPasswords) {
      try {
        const isValid = await bcrypt.compare(testPassword, user.password);
        const status = isValid ? '✅ VALID' : '❌ INVALID';
        console.log(`   ${status} - "${testPassword}"`);
      } catch (error) {
        console.log(`   ❌ ERROR - "${testPassword}": ${error.message}`);
      }
    }
    console.log('');

    // Check if hash looks valid
    const hashPrefix = user.password.substring(0, 7);
    const validPrefixes = ['$2a$10', '$2b$10', '$2y$10'];
    const isValidHashFormat = validPrefixes.some(prefix => user.password.startsWith(prefix));
    
    console.log('🔐 Hash Analysis:');
    console.log('   Format Valid:', isValidHashFormat ? '✅' : '❌');
    console.log('   Expected Prefix:', '$2a$10, $2b$10, or $2y$10');
    console.log('   Actual Prefix:', hashPrefix);
    console.log('');

    // Try to generate a new hash and compare
    console.log('🔄 Generating new hash for "password123" to compare:');
    const newHash = await bcrypt.hash('password123', 10);
    console.log('   New Hash:', newHash);
    console.log('   Old Hash:', user.password);
    console.log('   Hashes Match:', newHash === user.password ? 'Yes (same salt)' : 'No (different salt, but both should work)');
    console.log('');

    // Test if new hash works with password123
    const newHashTest = await bcrypt.compare('password123', newHash);
    console.log('   New hash test with "password123":', newHashTest ? '✅ WORKS' : '❌ FAILS');
    console.log('');

  } catch (error) {
    console.error('❌ Error:', error.message);
    console.error(error.stack);
    throw error;
  } finally {
    client.release();
    await pool.end();
  }
}

testLogin()
  .then(() => {
    console.log('✅ Test completed');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Test failed:', error);
    process.exit(1);
  });
