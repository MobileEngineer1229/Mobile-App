-- Diagnostic queries for device missing user_id and home_id

-- 1. Check the problematic device
SELECT
    id,
    name,
    type,
    user_id,
    home_id,
    mac_address,
    metadata
FROM devices
WHERE id = 111;

-- 2. Check if the user exists (based on metadata if user_id is NULL)
SELECT id, email, name
FROM users
LIMIT 5;

-- 3. Check homes for all users
SELECT
    h.id as home_id,
    h.user_id,
    u.email as user_email,
    h.name as home_name,
    h.is_primary
FROM homes h
JOIN users u ON h.user_id = u.user_id
ORDER BY h.user_id, h.is_primary DESC;

-- 4. Check database schema for devices table
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'devices'
    AND column_name IN ('user_id', 'home_id')
ORDER BY ordinal_position;

-- 5. Check if there are other devices with missing user_id or home_id
SELECT
    COUNT(*) as total_devices,
    COUNT(user_id) as devices_with_user_id,
    COUNT(home_id) as devices_with_home_id,
    COUNT(*) - COUNT(user_id) as missing_user_id,
    COUNT(*) - COUNT(home_id) as missing_home_id
FROM devices;
