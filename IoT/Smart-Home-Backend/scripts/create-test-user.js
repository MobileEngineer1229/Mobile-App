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

async function createTestUser() {
  const client = await pool.connect();
  try {
    const email = 'test@example.com';
    const password = 'password123';
    const firstName = 'Test';
    const lastName = 'User';

    // Check if user already exists
    const existingUser = await client.query('SELECT id FROM users WHERE email = $1', [email]);
    
    if (existingUser.rows.length > 0) {
      console.log('✅ User already exists:', email);
      return;
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10);

    // Insert user
    const result = await client.query(
      'INSERT INTO users (email, password, first_name, last_name) VALUES ($1, $2, $3, $4) RETURNING id, email, first_name, last_name',
      [email, hashedPassword, firstName, lastName]
    );

    console.log('✅ Test user created successfully!');
    console.log('   Email:', email);
    console.log('   Password:', password);
    console.log('   Name:', firstName, lastName);
    console.log('   User ID:', result.rows[0].id);
  } catch (error) {
    console.error('❌ Error creating user:', error.message);
    throw error;
  } finally {
    client.release();
    await pool.end();
  }
}

createTestUser()
  .then(() => {
    console.log('✅ Script completed');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Script failed:', error);
    process.exit(1);
  });

