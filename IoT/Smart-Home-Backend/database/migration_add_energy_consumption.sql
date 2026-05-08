-- Migration: Add Energy Consumption Tracking
-- This migration adds support for tracking energy consumption per device

-- Energy consumption records table
CREATE TABLE IF NOT EXISTS energy_consumption (
    id SERIAL PRIMARY KEY,
    device_id INTEGER NOT NULL,
    date DATE NOT NULL,
    consumption_kwh DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    cost_usd DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE,
    UNIQUE (device_id, date)
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_energy_device_id ON energy_consumption(device_id);
CREATE INDEX IF NOT EXISTS idx_energy_date ON energy_consumption(date);
CREATE INDEX IF NOT EXISTS idx_energy_device_date ON energy_consumption(device_id, date);

-- Create trigger for updated_at
DROP TRIGGER IF EXISTS update_energy_consumption_updated_at ON energy_consumption;
CREATE TRIGGER update_energy_consumption_updated_at BEFORE UPDATE ON energy_consumption
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add power_rating_watts to devices metadata (optional, can be stored in metadata JSONB)
-- This allows devices to have a power rating for cost calculation
-- We'll use metadata JSONB field that already exists in devices table

COMMENT ON TABLE energy_consumption IS 'Stores daily energy consumption records for each device';
COMMENT ON COLUMN energy_consumption.consumption_kwh IS 'Energy consumption in kilowatt-hours';
COMMENT ON COLUMN energy_consumption.cost_usd IS 'Cost in USD (calculated based on consumption and electricity rate)';

