-- Fix Demo User Passwords
-- This script updates all demo users (@example.com) with a valid bcrypt hash for password "password123"
-- 
-- Usage:
--   psql -h 172.86.88.76 -U postgres -d smart_home_db -f database/fix_demo_passwords.sql
--   OR connect via pgAdmin/any PostgreSQL client and run this script

-- Valid bcrypt hash for password "password123" (generated with salt rounds 10)
-- This hash will work for all demo users
UPDATE users 
SET password = '$2a$10$PtgKeXhEw4X4SY7TISn43uOmLl4zTYBETVZnCznZP4ZVqRdnWh0HG',
    updated_at = CURRENT_TIMESTAMP
WHERE email LIKE '%@example.com';

-- Verify the update
SELECT 
    email,
    first_name,
    last_name,
    CASE 
        WHEN password LIKE '$2a$10$%' OR password LIKE '$2b$10$%' OR password LIKE '$2y$10$%' THEN 'Valid'
        ELSE 'Invalid'
    END as hash_status,
    LENGTH(password) as hash_length,
    updated_at
FROM users
WHERE email LIKE '%@example.com'
ORDER BY email;

-- Show count of updated users
SELECT COUNT(*) as updated_users_count
FROM users
WHERE email LIKE '%@example.com' 
  AND password = '$2a$10$PtgKeXhEw4X4SY7TISn43uOmLl4zTYBETVZnCznZP4ZVqRdnWh0HG';
