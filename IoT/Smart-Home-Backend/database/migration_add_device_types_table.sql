-- Migration: Add Device Types Table
-- Creates a table to store device type templates instead of hardcoding them

-- Create device_types table
CREATE TABLE IF NOT EXISTS device_types (
    id SERIAL PRIMARY KEY,
    template_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    icon VARCHAR(255),
    default_type device_type_enum NOT NULL,
    metadata JSONB,
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_device_types_category ON device_types(category);
CREATE INDEX IF NOT EXISTS idx_device_types_active ON device_types(is_active);
CREATE INDEX IF NOT EXISTS idx_device_types_display_order ON device_types(display_order);

-- Create trigger for updated_at
DROP TRIGGER IF EXISTS update_device_types_updated_at ON device_types;
CREATE TRIGGER update_device_types_updated_at BEFORE UPDATE ON device_types
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert device type templates
INSERT INTO device_types (template_id, name, category, description, default_type, display_order) VALUES
-- Popular
('smart_v1_cctv', 'Smart V1 CCTV', 'Popular', 'Security camera with motion detection', 'cctv', 1),
('smart_lamp', 'Smart Lamp', 'Popular', 'Wi-Fi enabled smart lamp', 'lamp', 2),
('stereo_speaker', 'Stereo Speaker', 'Popular', 'Wireless stereo speaker', 'speaker', 3),
('smart_webcam', 'Smart Webcam', 'Popular', 'HD webcam with smart features', 'camera', 4),
('smart_thermostat', 'Smart Thermostat', 'Popular', 'Programmable smart thermostat', 'thermostat', 5),
('smart_lock', 'Smart Lock', 'Popular', 'Keyless smart door lock', 'lock', 6),

-- Lightning
('smart_lamp_lightning', 'Smart Lamp', 'Lightning', 'Wi-Fi enabled smart lamp', 'lamp', 1),
('smart_bulb', 'Smart Bulb', 'Lightning', 'Color-changing smart bulb', 'lamp', 2),
('led_strip', 'LED Strip', 'Lightning', 'RGB LED light strip', 'lamp', 3),
('smart_switch', 'Smart Switch', 'Lightning', 'Wi-Fi smart wall switch', 'lamp', 4),
('smart_dimmer', 'Smart Dimmer', 'Lightning', 'Dimmable smart switch', 'lamp', 5),
('smart_plug', 'Smart Plug', 'Lightning', 'Smart power outlet', 'lamp', 6),

-- Camera
('smart_v1_cctv_camera', 'Smart V1 CCTV', 'Camera', 'Security camera with motion detection', 'cctv', 1),
('smart_webcam_camera', 'Smart Webcam', 'Camera', 'HD webcam with smart features', 'camera', 2),
('smart_v2_cctv', 'Smart V2 CCTV', 'Camera', 'Advanced security camera', 'cctv', 3),
('smart_doorbell', 'Smart Doorbell', 'Camera', 'Video doorbell with motion detection', 'camera', 4),
('security_camera', 'Security Camera', 'Camera', 'Indoor/outdoor security camera', 'camera', 5),
('baby_monitor', 'Baby Monitor', 'Camera', 'Smart baby monitoring camera', 'camera', 6),

-- Electronics
('stereo_speaker_electronics', 'Stereo Speaker', 'Electronics', 'Wireless stereo speaker', 'speaker', 1),
('smart_tv', 'Smart TV', 'Electronics', 'Internet-connected smart TV', 'tv', 2),
('smart_refrigerator', 'Smart Refrigerator', 'Electronics', 'Wi-Fi enabled refrigerator', 'appliance', 3),
('smart_washer', 'Smart Washer', 'Electronics', 'Smart washing machine', 'appliance', 4),
('smart_ac', 'Smart AC', 'Electronics', 'Smart air conditioner', 'appliance', 5),
('smart_fan', 'Smart Fan', 'Electronics', 'Wi-Fi enabled smart fan', 'appliance', 6),

-- Speakers (additional types)
('bluetooth_speaker', 'Bluetooth Speaker', 'Electronics', 'Portable Bluetooth wireless speaker', 'speaker', 7),
('smart_speaker', 'Smart Speaker', 'Electronics', 'Voice-activated smart speaker with AI assistant', 'speaker', 8),
('soundbar', 'Soundbar', 'Electronics', 'Wireless soundbar for TV audio enhancement', 'speaker', 9),
('wireless_speaker', 'Wireless Speaker', 'Electronics', 'Multi-room wireless speaker system', 'speaker', 10),
('portable_speaker', 'Portable Speaker', 'Electronics', 'Battery-powered portable speaker', 'speaker', 11),
('home_theater_speaker', 'Home Theater Speaker', 'Electronics', 'Surround sound home theater speaker system', 'speaker', 12),

-- CCTV (additional types)
('indoor_cctv', 'Indoor CCTV', 'Camera', 'Indoor security surveillance camera', 'cctv', 7),
('outdoor_cctv', 'Outdoor CCTV', 'Camera', 'Weatherproof outdoor security camera', 'cctv', 8),
('ptz_camera', 'PTZ Camera', 'Camera', 'Pan-tilt-zoom security camera', 'cctv', 9),
('ip_camera', 'IP Camera', 'Camera', 'Network IP security camera', 'cctv', 10),
('wireless_cctv', 'Wireless CCTV', 'Camera', 'Wireless security camera system', 'cctv', 11),
('night_vision_cctv', 'Night Vision CCTV', 'Camera', 'Infrared night vision security camera', 'cctv', 12),
('dome_camera', 'Dome Camera', 'Camera', 'Dome-shaped security camera', 'cctv', 13),
('bullet_camera', 'Bullet Camera', 'Camera', 'Bullet-style outdoor security camera', 'cctv', 14)
ON CONFLICT (template_id) DO NOTHING;

-- Verify the migration
DO $$
DECLARE
    device_type_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO device_type_count FROM device_types;
    RAISE NOTICE 'Migration completed successfully!';
    RAISE NOTICE 'Total device types inserted: %', device_type_count;
END $$;
