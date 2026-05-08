-- Migration: Add home_id to devices table
-- This migration adds home_id foreign key to devices table
-- This allows devices to be directly associated with homes, even if they don't have a room

-- Add home_id column to devices table
ALTER TABLE devices ADD COLUMN IF NOT EXISTS home_id INTEGER NULL;

-- Add foreign key constraint
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_home'
    ) THEN
        ALTER TABLE devices ADD CONSTRAINT fk_device_home 
        FOREIGN KEY (home_id) REFERENCES homes(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_device_home_id ON devices(home_id);

-- Update existing devices to set home_id based on their room
-- If device has a room, get home_id from that room
UPDATE devices d
SET home_id = (
    SELECT r.home_id 
    FROM rooms r 
    WHERE r.id = d.room_id
)
WHERE d.room_id IS NOT NULL 
  AND d.home_id IS NULL;

-- For devices without rooms, set home_id to user's primary home
-- This is a best-effort update - some devices might remain NULL if user has no primary home
UPDATE devices d
SET home_id = (
    SELECT h.id 
    FROM homes h 
    WHERE h.user_id = d.user_id 
      AND h.is_primary = true 
    LIMIT 1
)
WHERE d.room_id IS NULL 
  AND d.home_id IS NULL;
