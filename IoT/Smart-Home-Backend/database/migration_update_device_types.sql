-- Migration: Update Device Types
-- Changes device_type_enum from ('sensor', 'actuator', 'controller') to ('lamp', 'camera', 'electronics')
-- Run this migration to update the device type enum

-- Step 1: Add new enum values (PostgreSQL doesn't support removing enum values directly)
-- We'll create a new enum and migrate data

-- Create new enum type
DO $$ BEGIN
    CREATE TYPE device_type_enum_new AS ENUM ('lamp', 'camera', 'electronics');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Step 2: Add a temporary column with the new enum type
ALTER TABLE devices ADD COLUMN IF NOT EXISTS type_new device_type_enum_new;

-- Step 3: Map existing device types to new types
-- Mapping strategy:
-- - 'actuator' (typically lights, switches) -> 'lamp'
-- - 'controller' (typically cameras, security) -> 'camera'  
-- - 'sensor' (typically sensors, electronics) -> 'electronics'
UPDATE devices 
SET type_new = CASE 
    WHEN type::text = 'actuator' THEN 'lamp'::device_type_enum_new
    WHEN type::text = 'controller' THEN 'camera'::device_type_enum_new
    WHEN type::text = 'sensor' THEN 'electronics'::device_type_enum_new
    ELSE 'electronics'::device_type_enum_new  -- Default fallback
END;

-- Step 4: Set default for any NULL values
UPDATE devices SET type_new = 'electronics'::device_type_enum_new WHERE type_new IS NULL;

-- Step 5: Drop the old column and rename the new one
ALTER TABLE devices DROP COLUMN IF EXISTS type;
ALTER TABLE devices RENAME COLUMN type_new TO type;
ALTER TABLE devices ALTER COLUMN type SET NOT NULL;

-- Step 6: Drop the old enum type and rename the new one
DROP TYPE IF EXISTS device_type_enum;
ALTER TYPE device_type_enum_new RENAME TO device_type_enum;

-- Step 7: Recreate the index
DROP INDEX IF EXISTS idx_type;
CREATE INDEX IF NOT EXISTS idx_type ON devices(type);

-- Verify the migration
DO $$
DECLARE
    device_count INTEGER;
    lamp_count INTEGER;
    camera_count INTEGER;
    electronics_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO device_count FROM devices;
    SELECT COUNT(*) INTO lamp_count FROM devices WHERE type = 'lamp';
    SELECT COUNT(*) INTO camera_count FROM devices WHERE type = 'camera';
    SELECT COUNT(*) INTO electronics_count FROM devices WHERE type = 'electronics';
    
    RAISE NOTICE 'Migration completed successfully!';
    RAISE NOTICE 'Total devices: %', device_count;
    RAISE NOTICE 'Lamp devices: %', lamp_count;
    RAISE NOTICE 'Camera devices: %', camera_count;
    RAISE NOTICE 'Electronics devices: %', electronics_count;
END $$;
