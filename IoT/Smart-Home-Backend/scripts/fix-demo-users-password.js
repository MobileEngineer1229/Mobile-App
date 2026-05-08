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

async function fixDemoUsersPassword() {
  const client = await pool.connect();
  try {
    const password = 'password123'; // The password for all demo users
    console.log('🔐 Generating bcrypt hash for password:', password);
    
    // Generate proper bcrypt hash
    const hashedPassword = await bcrypt.hash(password, 10);
    console.log('✅ Generated hash:', hashedPassword);
    console.log('');

    // Get all demo users (users with @example.com email)
    const result = await client.query(
      `SELECT id, email, first_name, last_name 
       FROM users 
       WHERE email LIKE '%@example.com' 
       ORDER BY email`
    );

    if (result.rows.length === 0) {
      console.log('⚠️  No demo users found with @example.com email');
      return;
    }

    console.log(`📋 Found ${result.rows.length} demo users to update:`);
    result.rows.forEach((user, index) => {
      console.log(`   ${index + 1}. ${user.email} (${user.first_name} ${user.last_name})`);
    });
    console.log('');

    // Update all demo users with the new password hash
    const updateResult = await client.query(
      `UPDATE users 
       SET password = $1, updated_at = CURRENT_TIMESTAMP 
       WHERE email LIKE '%@example.com'`,
      [hashedPassword]
    );

    console.log(`✅ Updated ${updateResult.rowCount} demo user(s) with new password hash`);
    console.log('');
    console.log('🔑 Login Credentials:');
    console.log('   Password for all demo users: password123');
    console.log('');
    console.log('📝 Example login:');
    console.log('   Email: john.doe@example.com');
    console.log('   Password: password123');
    console.log('');

    // Verify the update worked by checking one user
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

  } catch (error) {
    console.error('❌ Error fixing demo users password:', error.message);
    throw error;
  } finally {
    client.release();
    await pool.end();
  }
}

fixDemoUsersPassword()
  .then(() => {
    console.log('✅ Script completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Script failed:', error);
    process.exit(1);
  });
