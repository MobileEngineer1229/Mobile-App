const { Pool } = require('pg');
const bcrypt = require('bcryptjs');

const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432', 10),
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || '213515',
  database: process.env.DB_NAME || 'smart_home_db',
});

async function resetPassword() {
  const client = await pool.connect();
  try {
    const email = 'tymoshenkovitalii84@gmail.com';
    const newPassword = 'Password123';

    // Check if user exists
    const userResult = await client.query('SELECT id FROM users WHERE email = $1', [email]);
    
    if (userResult.rows.length === 0) {
      console.log('❌ User not found:', email);
      return;
    }

    // Hash new password
    const hashedPassword = await bcrypt.hash(newPassword, 10);

    // Update password
    await client.query(
      'UPDATE users SET password = $1, updated_at = CURRENT_TIMESTAMP WHERE email = $2',
      [hashedPassword, email]
    );

    console.log('✅ Password reset successfully!');
    console.log('   Email:', email);
    console.log('   New Password:', newPassword);
    console.log('   User ID:', userResult.rows[0].id);
  } catch (error) {
    console.error('❌ Error resetting password:', error.message);
    throw error;
  } finally {
    client.release();
    await pool.end();
  }
}

resetPassword()
  .then(() => {
    console.log('✅ Script completed');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Script failed:', error);
    process.exit(1);
  });

