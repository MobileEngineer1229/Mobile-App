-- Fix script for device missing user_id and home_id

-- Step 1: Identify the device and figure out which user it should belong to
-- (You'll need to determine the correct user_id based on who registered it)

-- Step 2: Create a default home for the user if they don't have one
-- Replace <USER_ID> with the actual user ID
DO $$
DECLARE
    v_user_id INTEGER := <USER_ID>; -- CHANGE THIS to the correct user ID
    v_home_id INTEGER;
    v_device_id INTEGER := 111; -- The device ID from your data
BEGIN
    -- Check if user has a home
    SELECT id INTO v_home_id
    FROM homes
    WHERE user_id = v_user_id
    LIMIT 1;

    -- If no home exists, create one
    IF v_home_id IS NULL THEN
        INSERT INTO homes (user_id, name, is_primary, created_at, updated_at)
        VALUES (v_user_id, 'My Home', true, NOW(), NOW())
        RETURNING id INTO v_home_id;

        RAISE NOTICE 'Created default home (ID: %) for user %', v_home_id, v_user_id;
    ELSE
        RAISE NOTICE 'User % already has home (ID: %)', v_user_id, v_home_id;
    END IF;

    -- Update the device with user_id and home_id
    UPDATE devices
    SET
        user_id = v_user_id,
        home_id = v_home_id,
        updated_at = NOW()
    WHERE id = v_device_id;

    RAISE NOTICE 'Updated device % with user_id=% and home_id=%', v_device_id, v_user_id, v_home_id;
END $$;

-- Step 3: Verify the fix
SELECT
    id,
    name,
    type,
    user_id,
    home_id,
    mac_address,
    status
FROM devices
WHERE id = 111;

-- Step 4: (Optional) Fix all devices with missing user_id/home_id
-- WARNING: Only run this if you know all orphaned devices belong to a specific user
/*
UPDATE devices
SET
    user_id = <USER_ID>,  -- Replace with correct user ID
    home_id = (SELECT id FROM homes WHERE user_id = <USER_ID> LIMIT 1),
    updated_at = NOW()
WHERE user_id IS NULL OR home_id IS NULL;
*/
