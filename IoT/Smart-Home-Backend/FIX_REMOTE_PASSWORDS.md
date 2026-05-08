# Fix Demo User Passwords on Remote Database

The demo users on the remote database (`172.86.88.76`) have invalid password hashes. Use one of these methods to fix them:

## Method 1: Using SQL Script (Recommended - Fastest)

1. **Connect to the remote PostgreSQL database:**
   ```bash
   psql -h 172.86.88.76 -U postgres -d smart_home_db
   ```

2. **Run the SQL script:**
   ```sql
   \i database/fix_demo_passwords.sql
   ```
   
   OR copy and paste the SQL directly:
   ```sql
   UPDATE users 
   SET password = '$2a$10$PtgKeXhEw4X4SY7TISn43uOmLl4zTYBETVZnCznZP4ZVqRdnWh0HG',
       updated_at = CURRENT_TIMESTAMP
   WHERE email LIKE '%@example.com';
   ```

3. **Verify:**
   ```sql
   SELECT email, LENGTH(password) as hash_length 
   FROM users 
   WHERE email = 'john.doe@example.com';
   ```
   Should show `hash_length = 60`

## Method 2: Using Node.js Script

1. **Set environment variables:**
   ```bash
   export DB_HOST=172.86.88.76
   export DB_PORT=5432
   export DB_USER=postgres
   export DB_PASSWORD=your_remote_db_password
   export DB_NAME=smart_home_db
   ```

2. **Run the script:**
   ```bash
   cd Smart-Home-Backend
   node scripts/fix-remote-demo-passwords.js
   ```

## Method 3: Using pgAdmin or DBeaver

1. Connect to the remote database (`172.86.88.76`)
2. Open SQL editor
3. Run this SQL:
   ```sql
   UPDATE users 
   SET password = '$2a$10$PtgKeXhEw4X4SY7TISn43uOmLl4zTYBETVZnCznZP4ZVqRdnWh0HG',
       updated_at = CURRENT_TIMESTAMP
   WHERE email LIKE '%@example.com';
   ```

## After Fixing

**Login Credentials:**
- **Email:** `john.doe@example.com` (or any `@example.com` user)
- **Password:** `password123` (without exclamation mark)

**Test with Postman:**
```json
POST http://172.86.88.76:3003/api/v1/users/login
Content-Type: application/json

{
    "email": "john.doe@example.com",
    "password": "password123"
}
```

## Troubleshooting

If you still get "Invalid email or password":

1. **Verify the user exists:**
   ```sql
   SELECT email, LENGTH(password) as hash_len 
   FROM users 
   WHERE email = 'john.doe@example.com';
   ```

2. **Check hash format:**
   ```sql
   SELECT email, 
          SUBSTRING(password, 1, 7) as hash_prefix,
          LENGTH(password) as hash_length
   FROM users 
   WHERE email = 'john.doe@example.com';
   ```
   Should show:
   - `hash_prefix = $2a$10$`
   - `hash_length = 60`

3. **Verify password is correct:**
   - Use exactly: `password123` (lowercase, no exclamation)
   - Not: `password123!` or `Password123`
