-- Smart Home Backend Database Schema
-- PostgreSQL 12+

-- Create database (run this separately if database doesn't exist)
-- Note: You cannot create a database from within a transaction block.
-- If the database already exists, you'll get an error - that's fine, just continue.
-- To create the database, run this command separately:
-- CREATE DATABASE smart_home_db;

-- Create ENUM types (only if they don't exist)
DO $$ BEGIN
    CREATE TYPE device_type_enum AS ENUM (
        'lamp', 
        'camera', 
        'electronics',
        'cctv',
        'speaker',
        'thermostat',
        'lock',
        'tv',
        'appliance',
        'sensor'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE device_status_enum AS ENUM ('online', 'offline', 'unknown');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on email
CREATE INDEX IF NOT EXISTS idx_email ON users(email);

-- Devices table
CREATE TABLE IF NOT EXISTS devices (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    type device_type_enum NOT NULL,
    status device_status_enum NOT NULL DEFAULT 'unknown',
    mac_address VARCHAR(17) NOT NULL,
    ip_address VARCHAR(45) NULL,
    last_seen TIMESTAMP NULL,
    metadata JSONB NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (user_id, mac_address)
);

-- Create indexes on devices table
CREATE INDEX IF NOT EXISTS idx_user_id ON devices(user_id);
CREATE INDEX IF NOT EXISTS idx_mac_address ON devices(mac_address);
CREATE INDEX IF NOT EXISTS idx_type ON devices(type);
CREATE INDEX IF NOT EXISTS idx_status ON devices(status);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Drop triggers if they exist (to allow re-running the script)
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
DROP TRIGGER IF EXISTS update_devices_updated_at ON devices;

-- Create triggers to automatically update updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_devices_updated_at BEFORE UPDATE ON devices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Smart Scenes table
CREATE TABLE IF NOT EXISTS smart_scenes (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    home_id INTEGER NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'automation', -- 'automation' or 'tap_to_run'
    condition_logic VARCHAR(10) NOT NULL DEFAULT 'any', -- 'any' or 'all'
    icon VARCHAR(100) NULL, -- Icon name/resource identifier
    color VARCHAR(20) NULL, -- Color hex code (e.g., '#405FF2')
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes on smart_scenes table
CREATE INDEX IF NOT EXISTS idx_scenes_user_id ON smart_scenes(user_id);
CREATE INDEX IF NOT EXISTS idx_scenes_home_id ON smart_scenes(home_id);
CREATE INDEX IF NOT EXISTS idx_scenes_type ON smart_scenes(type);
CREATE INDEX IF NOT EXISTS idx_scenes_enabled ON smart_scenes(is_enabled);

-- Scene Conditions table
CREATE TABLE IF NOT EXISTS scene_conditions (
    id SERIAL PRIMARY KEY,
    scene_id INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'temperature', 'humidity', 'weather', 'sunrise_sunset', 'wind_speed', 'location', 'location_arrive_at', 'location_leave', 'schedule_time', 'time', 'device_status', 'arm_mode', 'alarm', 'tap_to_run'
    operator VARCHAR(50) NULL, -- '<', '=', '>', 'dry', 'comfortable', 'moist', 'sunny', 'cloudy', 'rainy', 'snowy', 'hazy', 'sunrise', 'sunset'
    value DOUBLE PRECISION NULL,
    unit VARCHAR(20) NULL, -- 'celsius', 'fahrenheit', 'm/s', 'km/h'
    location VARCHAR(255) NULL,
    device_id INTEGER NULL,
    device_status VARCHAR(50) NULL,
    arm_mode VARCHAR(50) NULL, -- 'disarmed', 'arm_stay', 'arm_away'
    metadata JSONB NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (scene_id) REFERENCES smart_scenes(id) ON DELETE CASCADE,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
);

-- Create indexes on scene_conditions table
CREATE INDEX IF NOT EXISTS idx_conditions_scene_id ON scene_conditions(scene_id);
CREATE INDEX IF NOT EXISTS idx_conditions_type ON scene_conditions(type);

-- Scene Tasks table
CREATE TABLE IF NOT EXISTS scene_tasks (
    id SERIAL PRIMARY KEY,
    scene_id INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'control_device', 'select_scene', 'change_arm_mode', 'send_notification', 'delay'
    device_id INTEGER NULL,
    device_name VARCHAR(255) NULL,
    room_name VARCHAR(255) NULL,
    function VARCHAR(50) NULL, -- 'ON', 'OFF', or device-specific function
    scene_id_target INTEGER NULL, -- For 'select_scene' type
    scene_name VARCHAR(255) NULL,
    arm_mode VARCHAR(50) NULL, -- For 'change_arm_mode' type
    notification_message TEXT NULL, -- For 'send_notification' type
    delay_seconds INTEGER NULL, -- For 'delay' type
    order_index INTEGER NOT NULL DEFAULT 0, -- Order of task execution
    metadata JSONB NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (scene_id) REFERENCES smart_scenes(id) ON DELETE CASCADE,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL,
    FOREIGN KEY (scene_id_target) REFERENCES smart_scenes(id) ON DELETE SET NULL
);

-- Create indexes on scene_tasks table
CREATE INDEX IF NOT EXISTS idx_tasks_scene_id ON scene_tasks(scene_id);
CREATE INDEX IF NOT EXISTS idx_tasks_type ON scene_tasks(type);
CREATE INDEX IF NOT EXISTS idx_tasks_order ON scene_tasks(scene_id, order_index);

-- Create trigger to update smart_scenes updated_at
DROP TRIGGER IF EXISTS update_scenes_updated_at ON smart_scenes;
CREATE TRIGGER update_scenes_updated_at BEFORE UPDATE ON smart_scenes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

