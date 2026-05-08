-- Migration: Add home_id to rooms table
-- This migration adds home_id foreign key to rooms table

-- Add home_id column to rooms table
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS home_id INTEGER NULL;

-- Add foreign key constraint
ALTER TABLE rooms ADD CONSTRAINT fk_room_home 
    FOREIGN KEY (home_id) REFERENCES homes(id) ON DELETE CASCADE;

-- Create index
CREATE INDEX IF NOT EXISTS idx_room_home_id ON rooms(home_id);

-- Update existing rooms to have a default home (if homes exist)
-- This sets all existing rooms to the primary home of each user
DO $$
BEGIN
    UPDATE rooms r
    SET home_id = (
        SELECT h.id 
        FROM homes h 
        WHERE h.user_id = r.user_id 
        AND h.is_primary = true 
        LIMIT 1
    )
    WHERE r.home_id IS NULL;
END $$;

-- Make home_id NOT NULL after setting defaults (if we want to enforce it)
-- ALTER TABLE rooms ALTER COLUMN home_id SET NOT NULL;
