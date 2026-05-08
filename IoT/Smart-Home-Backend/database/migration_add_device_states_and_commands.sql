-- Migration: Add device_states and device_commands tables
-- device_states: current state of each device property (e.g., brightness=80, power=on)
-- device_commands: history of commands sent to devices from the mobile app

-- 1. Device states - stores the latest value of each property per device
CREATE TABLE IF NOT EXISTS device_states (
    id SERIAL PRIMARY KEY,
    device_id INTEGER NOT NULL,
    property_key VARCHAR(100) NOT NULL,
    property_value JSONB NOT NULL,
    source VARCHAR(50) DEFAULT 'mqtt',  -- 'mqtt', 'manual', 'scene', 'api'
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE,
    UNIQUE (device_id, property_key)
);

CREATE INDEX IF NOT EXISTS idx_device_states_device_id ON device_states(device_id);

-- 2. Device commands - audit log of all commands sent to devices
CREATE TABLE IF NOT EXISTS device_commands (
    id SERIAL PRIMARY KEY,
    device_id INTEGER NOT NULL,
    user_id INTEGER NULL,
    command_type VARCHAR(100) NOT NULL,   -- e.g., 'set_property', 'power_on', 'power_off'
    payload JSONB NOT NULL,               -- the command payload sent via MQTT
    status VARCHAR(20) DEFAULT 'sent',    -- 'sent', 'delivered', 'acknowledged', 'failed'
    response JSONB NULL,                  -- device response if any
    source VARCHAR(50) DEFAULT 'app',     -- 'app', 'scene', 'voice', 'schedule'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP NULL,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_device_commands_device_id ON device_commands(device_id);
CREATE INDEX IF NOT EXISTS idx_device_commands_user_id ON device_commands(user_id);
CREATE INDEX IF NOT EXISTS idx_device_commands_created_at ON device_commands(created_at DESC);
