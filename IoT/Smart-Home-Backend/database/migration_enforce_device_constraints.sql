-- Migration: Enforce device constraints
-- Ensures user_id is NOT NULL and home_id has a default value

-- Step 1: Fix existing NULL user_id values (shouldn't exist, but just in case)
-- This will fail if there are devices with NULL user_id and you'll need to manually fix them
DO $$
DECLARE
    null_user_devices INTEGER;
BEGIN
    SELECT COUNT(*) INTO null_user_devices FROM devices WHERE user_id IS NULL;

    IF null_user_devices > 0 THEN
        RAISE NOTICE 'Found % devices with NULL user_id. Please fix manually before enforcing constraint.', null_user_devices;
        RAISE EXCEPTION 'Cannot enforce NOT NULL constraint: % devices have NULL user_id', null_user_devices;
    END IF;
END $$;

-- Step 2: Set home_id for devices that don't have one (use user's primary home)
UPDATE devices d
SET home_id = (
    SELECT h.id
    FROM homes h
    WHERE h.user_id = d.user_id
      AND h.is_primary = true
    LIMIT 1
)
WHERE d.home_id IS NULL
  AND d.user_id IS NOT NULL;

-- Step 3: For users without a primary home, create one
INSERT INTO homes (user_id, name, is_primary, created_at, updated_at)
SELECT DISTINCT
    d.user_id,
    'My Home' as name,
    true as is_primary,
    NOW() as created_at,
    NOW() as updated_at
FROM devices d
WHERE d.home_id IS NULL
  AND d.user_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM homes h WHERE h.user_id = d.user_id
  );

-- Step 4: Update devices again (for newly created homes)
UPDATE devices d
SET home_id = (
    SELECT h.id
    FROM homes h
    WHERE h.user_id = d.user_id
    LIMIT 1
)
WHERE d.home_id IS NULL
  AND d.user_id IS NOT NULL;

-- Step 5: Verify all devices now have home_id
DO $$
DECLARE
    null_home_devices INTEGER;
BEGIN
    SELECT COUNT(*) INTO null_home_devices FROM devices WHERE home_id IS NULL;

    IF null_home_devices > 0 THEN
        RAISE WARNING 'Still have % devices with NULL home_id after migration', null_home_devices;
    ELSE
        RAISE NOTICE 'All devices now have home_id set';
    END IF;
END $$;

-- Step 6: Optionally enforce NOT NULL constraint on home_id (uncomment if you want)
-- ALTER TABLE devices ALTER COLUMN home_id SET NOT NULL;

-- Step 7: Add check constraint to ensure device has either room_id or home_id
ALTER TABLE devices DROP CONSTRAINT IF EXISTS chk_device_has_location;
ALTER TABLE devices ADD CONSTRAINT chk_device_has_location
    CHECK (home_id IS NOT NULL);
