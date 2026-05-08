-- Migration: Add Rooms Support
-- Run this after the initial schema.sql

-- Rooms table
CREATE TABLE IF NOT EXISTS rooms (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (user_id, name)
);

-- Add room_id to devices table
ALTER TABLE devices ADD COLUMN IF NOT EXISTS room_id INTEGER NULL;
ALTER TABLE devices ADD CONSTRAINT fk_device_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_room_user_id ON rooms(user_id);
CREATE INDEX IF NOT EXISTS idx_device_room_id ON devices(room_id);

-- Create trigger for rooms updated_at
DROP TRIGGER IF EXISTS update_rooms_updated_at ON rooms;
CREATE TRIGGER update_rooms_updated_at BEFORE UPDATE ON rooms
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default rooms for existing users (optional - can be done via API)
-- This is just a template, actual rooms should be created via API

