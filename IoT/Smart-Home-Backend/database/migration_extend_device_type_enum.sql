-- Migration: Extend Device Type Enum
-- Adds more specific device types to device_type_enum
-- This allows for more granular device type classification

-- Add new enum values to device_type_enum
-- Note: ALTER TYPE ... ADD VALUE cannot be run inside a transaction block in PostgreSQL
-- Each ADD VALUE must be executed separately

-- Add CCTV type
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'cctv' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'device_type_enum')
    ) THEN
        ALTER TYPE device_type_enum ADD VALUE 'cctv';
    END IF;
END $$;

-- Add speaker type
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'speaker' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'device_type_enum')
    ) THEN
        ALTER TYPE device_type_enum ADD VALUE 'speaker';
    END IF;
END $$;

-- Add thermostat type
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'thermostat' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'device_type_enum')
    ) THEN
        ALTER TYPE device_type_enum ADD VALUE 'thermostat';
    END IF;
END $$;

-- Add lock type
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'lock' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'device_type_enum')
    ) THEN
        ALTER TYPE device_type_enum ADD VALUE 'lock';
    END IF;
END $$;

-- Add tv type
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'tv' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'device_type_enum')
    ) THEN
        ALTER TYPE device_type_enum ADD VALUE 'tv';
    END IF;
END $$;

-- Add appliance type
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'appliance' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'device_type_enum')
    ) THEN
        ALTER TYPE device_type_enum ADD VALUE 'appliance';
    END IF;
END $$;

-- Add sensor type
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'sensor' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'device_type_enum')
    ) THEN
        ALTER TYPE device_type_enum ADD VALUE 'sensor';
    END IF;
END $$;

-- Verify the migration
DO $$
DECLARE
    enum_values TEXT;
BEGIN
    SELECT string_agg(enumlabel::text, ', ' ORDER BY enumsortorder)
    INTO enum_values
    FROM pg_enum
    WHERE enumtypid = (SELECT oid FROM pg_type WHERE typname = 'device_type_enum');
    
    RAISE NOTICE 'Migration completed successfully!';
    RAISE NOTICE 'Current device_type_enum values: %', enum_values;
END $$;
