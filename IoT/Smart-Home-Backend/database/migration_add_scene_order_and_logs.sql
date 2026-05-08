-- Migration: Add order_index to smart_scenes and create scene_execution_logs table
-- Run this after schema.sql

-- Add order_index column to smart_scenes table if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'smart_scenes' AND column_name = 'order_index'
    ) THEN
        ALTER TABLE smart_scenes ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0;
        CREATE INDEX IF NOT EXISTS idx_scenes_order ON smart_scenes(user_id, type, order_index);
    END IF;
END $$;

-- Scene Execution Logs table
CREATE TABLE IF NOT EXISTS scene_execution_logs (
    id SERIAL PRIMARY KEY,
    scene_id INTEGER NOT NULL,
    scene_name VARCHAR(255) NOT NULL,
    user_id INTEGER NOT NULL,
    home_id INTEGER NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'succeeded', -- 'succeeded', 'failed', 'pending'
    error_message TEXT NULL,
    execution_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB NULL,
    FOREIGN KEY (scene_id) REFERENCES smart_scenes(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes on scene_execution_logs table
CREATE INDEX IF NOT EXISTS idx_logs_user_id ON scene_execution_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_logs_scene_id ON scene_execution_logs(scene_id);
CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON scene_execution_logs(execution_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_logs_status ON scene_execution_logs(status);
CREATE INDEX IF NOT EXISTS idx_logs_user_timestamp ON scene_execution_logs(user_id, execution_timestamp DESC);
