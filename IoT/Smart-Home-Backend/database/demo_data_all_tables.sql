-- Comprehensive Demo Data for All Database Tables
-- This script inserts more than 10 sample records for each table
-- Note: Run schema.sql and migrations first, then this file

-- ============================================
-- USERS TABLE (15+ records)
-- ============================================
-- Password hash for all demo users: "password123" (bcrypt hash)
-- In production, use proper password hashing
INSERT INTO users (email, password, first_name, last_name, phone) VALUES
('john.doe@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'John', 'Doe', '+1-555-0101'),
('jane.smith@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Jane', 'Smith', '+1-555-0102'),
('michael.johnson@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Michael', 'Johnson', '+1-555-0103'),
('sarah.williams@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Sarah', 'Williams', '+1-555-0104'),
('david.brown@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'David', 'Brown', '+1-555-0105'),
('emily.davis@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Emily', 'Davis', '+1-555-0106'),
('james.miller@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'James', 'Miller', '+1-555-0107'),
('olivia.wilson@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Olivia', 'Wilson', '+1-555-0108'),
('william.moore@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'William', 'Moore', '+1-555-0109'),
('sophia.taylor@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Sophia', 'Taylor', '+1-555-0110'),
('benjamin.anderson@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Benjamin', 'Anderson', '+1-555-0111'),
('isabella.thomas@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Isabella', 'Thomas', '+1-555-0112'),
('lucas.jackson@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Lucas', 'Jackson', '+1-555-0113'),
('mia.white@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Mia', 'White', '+1-555-0114'),
('henry.harris@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Henry', 'Harris', '+1-555-0115'),
('charlotte.martin@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Charlotte', 'Martin', '+1-555-0116'),
('alexander.thompson@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Alexander', 'Thompson', '+1-555-0117'),
('amelia.garcia@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Amelia', 'Garcia', '+1-555-0118')
ON CONFLICT (email) DO NOTHING;

-- ============================================
-- HOMES TABLE (15+ records)
-- ============================================
-- Insert homes using user emails to get correct user_ids
INSERT INTO homes (user_id, name, address, latitude, longitude, country, is_primary)
SELECT u.id, h.name, h.address, h.latitude, h.longitude, h.country, h.is_primary
FROM (VALUES
    ('john.doe@example.com', 'Main Residence', '701 7th Ave, New York, NY 10036, USA', 40.7579, -73.9877, 'United States', true),
    ('john.doe@example.com', 'Summer House', '123 Ocean Drive, Miami Beach, FL 33139, USA', 25.7907, -80.1300, 'United States', false),
    ('jane.smith@example.com', 'Downtown Apartment', '456 Broadway, New York, NY 10013, USA', 40.7209, -74.0007, 'United States', true),
    ('jane.smith@example.com', 'Country Home', '789 Country Road, Upstate NY 12550, USA', 41.7000, -74.0000, 'United States', false),
    ('michael.johnson@example.com', 'City Loft', '321 Park Ave, New York, NY 10022, USA', 40.7589, -73.9692, 'United States', true),
    ('sarah.williams@example.com', 'Suburban Home', '654 Maple Street, Westchester, NY 10583, USA', 41.0000, -73.8000, 'United States', true),
    ('david.brown@example.com', 'Beach House', '987 Beach Blvd, Malibu, CA 90265, USA', 34.0259, -118.7798, 'United States', true),
    ('emily.davis@example.com', 'Mountain Cabin', '456 Pine Trail, Aspen, CO 81611, USA', 39.1911, -106.8175, 'United States', true),
    ('james.miller@example.com', 'Urban Studio', '789 5th Ave, New York, NY 10022, USA', 40.7636, -73.9744, 'United States', true),
    ('olivia.wilson@example.com', 'Family Home', '123 Elm Street, Boston, MA 02115, USA', 42.3399, -71.0899, 'United States', true),
    ('william.moore@example.com', 'Lakeside Retreat', '321 Lake Road, Lake Tahoe, CA 96150, USA', 39.0968, -120.0324, 'United States', true),
    ('sophia.taylor@example.com', 'Desert Oasis', '654 Desert Way, Scottsdale, AZ 85251, USA', 33.4942, -111.9261, 'United States', true),
    ('benjamin.anderson@example.com', 'Historic Townhouse', '987 Heritage Lane, Charleston, SC 29401, USA', 32.7765, -79.9311, 'United States', true),
    ('isabella.thomas@example.com', 'Modern Condo', '456 Modern Ave, San Francisco, CA 94102, USA', 37.7749, -122.4194, 'United States', true),
    ('lucas.jackson@example.com', 'Ranch House', '789 Ranch Road, Austin, TX 78701, USA', 30.2672, -97.7431, 'United States', true),
    ('mia.white@example.com', 'Coastal Villa', '123 Coast Drive, San Diego, CA 92101, USA', 32.7157, -117.1611, 'United States', true),
    ('henry.harris@example.com', 'Garden Home', '321 Garden Street, Portland, OR 97201, USA', 45.5152, -122.6784, 'United States', true),
    ('charlotte.martin@example.com', 'Skyline Penthouse', '654 Skyline Blvd, Seattle, WA 98101, USA', 47.6062, -122.3321, 'United States', true)
) AS h(email, name, address, latitude, longitude, country, is_primary)
JOIN users u ON u.email = h.email
ON CONFLICT (user_id, name) DO NOTHING;

-- ============================================
-- ROOMS TABLE (15+ records per user)
-- ============================================
INSERT INTO rooms (user_id, name)
SELECT u.id, r.name
FROM (VALUES
    ('john.doe@example.com', 'Living Room'),
    ('john.doe@example.com', 'Bedroom'),
    ('john.doe@example.com', 'Kitchen'),
    ('john.doe@example.com', 'Bathroom'),
    ('john.doe@example.com', 'Study Room'),
    ('john.doe@example.com', 'Dining Room'),
    ('john.doe@example.com', 'Guest Room'),
    ('john.doe@example.com', 'Master Bedroom'),
    ('john.doe@example.com', 'Home Office'),
    ('john.doe@example.com', 'Garage'),
    ('john.doe@example.com', 'Basement'),
    ('john.doe@example.com', 'Attic'),
    ('john.doe@example.com', 'Patio'),
    ('john.doe@example.com', 'Garden'),
    ('jane.smith@example.com', 'Living Room'),
    ('jane.smith@example.com', 'Bedroom'),
    ('jane.smith@example.com', 'Kitchen'),
    ('jane.smith@example.com', 'Bathroom'),
    ('jane.smith@example.com', 'Study Room'),
    ('jane.smith@example.com', 'Dining Room'),
    ('jane.smith@example.com', 'Guest Room'),
    ('jane.smith@example.com', 'Master Bedroom'),
    ('jane.smith@example.com', 'Home Office'),
    ('jane.smith@example.com', 'Balcony'),
    ('jane.smith@example.com', 'Storage Room'),
    ('jane.smith@example.com', 'Laundry Room'),
    ('jane.smith@example.com', 'Pantry'),
    ('jane.smith@example.com', 'Workshop'),
    ('michael.johnson@example.com', 'Living Room'),
    ('michael.johnson@example.com', 'Bedroom'),
    ('michael.johnson@example.com', 'Kitchen'),
    ('michael.johnson@example.com', 'Bathroom'),
    ('michael.johnson@example.com', 'Study Room'),
    ('michael.johnson@example.com', 'Dining Room'),
    ('michael.johnson@example.com', 'Guest Room'),
    ('michael.johnson@example.com', 'Master Bedroom'),
    ('michael.johnson@example.com', 'Home Office'),
    ('michael.johnson@example.com', 'Rooftop'),
    ('michael.johnson@example.com', 'Media Room'),
    ('michael.johnson@example.com', 'Gym'),
    ('michael.johnson@example.com', 'Wine Cellar'),
    ('michael.johnson@example.com', 'Library')
) AS r(email, name)
JOIN users u ON u.email = r.email
ON CONFLICT (user_id, name) DO NOTHING;

-- ============================================
-- DEVICES TABLE (15+ records per user)
-- ============================================
-- Insert devices using user emails and room names to get correct IDs
INSERT INTO devices (user_id, name, type, status, mac_address, ip_address, room_id, last_seen, metadata)
SELECT 
    u.id,
    d.name,
    d.type::device_type_enum,
    d.status::device_status_enum,
    d.mac_address,
    d.ip_address,
    r.id,
    d.last_seen,
    d.metadata::jsonb
FROM (VALUES
    ('john.doe@example.com', 'Living Room Light', 'actuator', 'online', 'AA:BB:CC:DD:EE:01', '192.168.1.101', 'Living Room', NOW() - INTERVAL '5 minutes', '{"brightness": 80, "color": "warm_white"}'),
    ('john.doe@example.com', 'Bedroom Light', 'actuator', 'online', 'AA:BB:CC:DD:EE:02', '192.168.1.102', 'Bedroom', NOW() - INTERVAL '10 minutes', '{"brightness": 50, "color": "soft_white"}'),
    ('john.doe@example.com', 'Kitchen Temperature Sensor', 'sensor', 'online', 'AA:BB:CC:DD:EE:03', '192.168.1.103', 'Kitchen', NOW() - INTERVAL '2 minutes', '{"temperature": 22.5, "humidity": 45}'),
    ('john.doe@example.com', 'Smart Thermostat', 'controller', 'online', 'AA:BB:CC:DD:EE:04', '192.168.1.104', 'Living Room', NOW() - INTERVAL '1 minute', '{"temperature": 21.0, "mode": "auto"}'),
    ('john.doe@example.com', 'Front Door Lock', 'actuator', 'online', 'AA:BB:CC:DD:EE:05', '192.168.1.105', NULL, NOW() - INTERVAL '30 minutes', '{"locked": true, "battery": 85}'),
    ('john.doe@example.com', 'Security Camera', 'sensor', 'online', 'AA:BB:CC:DD:EE:06', '192.168.1.106', NULL, NOW() - INTERVAL '1 minute', '{"motion_detected": false}'),
    ('john.doe@example.com', 'Smoke Detector', 'sensor', 'online', 'AA:BB:CC:DD:EE:07', '192.168.1.107', 'Living Room', NOW() - INTERVAL '5 minutes', '{"smoke_level": 0, "battery": 90}'),
    ('john.doe@example.com', 'Motion Sensor', 'sensor', 'online', 'AA:BB:CC:DD:EE:08', '192.168.1.108', 'Bedroom', NOW() - INTERVAL '3 minutes', '{"motion": false}'),
    ('john.doe@example.com', 'Window Sensor', 'sensor', 'offline', 'AA:BB:CC:DD:EE:09', '192.168.1.109', 'Bedroom', NOW() - INTERVAL '2 hours', '{"open": false}'),
    ('john.doe@example.com', 'Smart Speaker', 'controller', 'online', 'AA:BB:CC:DD:EE:0A', '192.168.1.110', 'Living Room', NOW() - INTERVAL '1 minute', '{"volume": 50, "playing": false}'),
    ('john.doe@example.com', 'Garage Door Opener', 'actuator', 'online', 'AA:BB:CC:DD:EE:0B', '192.168.1.111', 'Garage', NOW() - INTERVAL '15 minutes', '{"open": false}'),
    ('john.doe@example.com', 'Smart TV', 'controller', 'online', 'AA:BB:CC:DD:EE:0C', '192.168.1.112', 'Living Room', NOW() - INTERVAL '5 minutes', '{"power": false, "channel": 0}'),
    ('john.doe@example.com', 'Refrigerator', 'actuator', 'online', 'AA:BB:CC:DD:EE:0D', '192.168.1.113', 'Kitchen', NOW() - INTERVAL '1 minute', '{"temperature": 4.0}'),
    ('john.doe@example.com', 'Washing Machine', 'actuator', 'offline', 'AA:BB:CC:DD:EE:0E', '192.168.1.114', NULL, NOW() - INTERVAL '1 day', '{"running": false}'),
    ('john.doe@example.com', 'Air Conditioner', 'actuator', 'online', 'AA:BB:CC:DD:EE:0F', '192.168.1.115', 'Living Room', NOW() - INTERVAL '2 minutes', '{"power": true, "temperature": 22, "mode": "cool"}'),
    ('john.doe@example.com', 'Humidity Sensor', 'sensor', 'online', 'AA:BB:CC:DD:EE:10', '192.168.1.116', 'Kitchen', NOW() - INTERVAL '1 minute', '{"humidity": 45}'),
    ('john.doe@example.com', 'Smart Plug', 'actuator', 'online', 'AA:BB:CC:DD:EE:11', '192.168.1.117', 'Living Room', NOW() - INTERVAL '1 minute', '{"power": true, "energy": 120}'),
    ('jane.smith@example.com', 'Living Room Light', 'actuator', 'online', 'BB:CC:DD:EE:FF:01', '192.168.1.201', 'Living Room', NOW() - INTERVAL '5 minutes', '{"brightness": 75}'),
    ('jane.smith@example.com', 'Bedroom Light', 'actuator', 'online', 'BB:CC:DD:EE:FF:02', '192.168.1.202', 'Bedroom', NOW() - INTERVAL '10 minutes', '{"brightness": 40}'),
    ('jane.smith@example.com', 'Kitchen Temperature Sensor', 'sensor', 'online', 'BB:CC:DD:EE:FF:03', '192.168.1.203', 'Kitchen', NOW() - INTERVAL '2 minutes', '{"temperature": 23.0}'),
    ('jane.smith@example.com', 'Smart Thermostat', 'controller', 'online', 'BB:CC:DD:EE:FF:04', '192.168.1.204', 'Living Room', NOW() - INTERVAL '1 minute', '{"temperature": 22.0}'),
    ('jane.smith@example.com', 'Front Door Lock', 'actuator', 'online', 'BB:CC:DD:EE:FF:05', '192.168.1.205', NULL, NOW() - INTERVAL '30 minutes', '{"locked": true}'),
    ('jane.smith@example.com', 'Security Camera', 'sensor', 'online', 'BB:CC:DD:EE:FF:06', '192.168.1.206', NULL, NOW() - INTERVAL '1 minute', '{"motion_detected": false}'),
    ('jane.smith@example.com', 'Smoke Detector', 'sensor', 'online', 'BB:CC:DD:EE:FF:07', '192.168.1.207', 'Living Room', NOW() - INTERVAL '5 minutes', '{"smoke_level": 0}'),
    ('jane.smith@example.com', 'Motion Sensor', 'sensor', 'online', 'BB:CC:DD:EE:FF:08', '192.168.1.208', 'Bedroom', NOW() - INTERVAL '3 minutes', '{"motion": false}'),
    ('jane.smith@example.com', 'Window Sensor', 'sensor', 'offline', 'BB:CC:DD:EE:FF:09', '192.168.1.209', 'Bedroom', NOW() - INTERVAL '2 hours', '{"open": false}'),
    ('jane.smith@example.com', 'Smart Speaker', 'controller', 'online', 'BB:CC:DD:EE:FF:0A', '192.168.1.210', 'Living Room', NOW() - INTERVAL '1 minute', '{"volume": 60}'),
    ('jane.smith@example.com', 'Garage Door Opener', 'actuator', 'online', 'BB:CC:DD:EE:FF:0B', '192.168.1.211', NULL, NOW() - INTERVAL '15 minutes', '{"open": false}'),
    ('jane.smith@example.com', 'Smart TV', 'controller', 'online', 'BB:CC:DD:EE:FF:0C', '192.168.1.212', 'Living Room', NOW() - INTERVAL '5 minutes', '{"power": false}'),
    ('jane.smith@example.com', 'Refrigerator', 'actuator', 'online', 'BB:CC:DD:EE:FF:0D', '192.168.1.213', 'Kitchen', NOW() - INTERVAL '1 minute', '{"temperature": 4.0}'),
    ('jane.smith@example.com', 'Washing Machine', 'actuator', 'offline', 'BB:CC:DD:EE:FF:0E', '192.168.1.214', NULL, NOW() - INTERVAL '1 day', '{"running": false}'),
    ('jane.smith@example.com', 'Air Conditioner', 'actuator', 'online', 'BB:CC:DD:EE:FF:0F', '192.168.1.215', 'Living Room', NOW() - INTERVAL '2 minutes', '{"power": true, "temperature": 23}'),
    ('jane.smith@example.com', 'Humidity Sensor', 'sensor', 'online', 'BB:CC:DD:EE:FF:10', '192.168.1.216', 'Kitchen', NOW() - INTERVAL '1 minute', '{"humidity": 50}'),
    ('jane.smith@example.com', 'Smart Plug', 'actuator', 'online', 'BB:CC:DD:EE:FF:11', '192.168.1.217', 'Living Room', NOW() - INTERVAL '1 minute', '{"power": true}')
) AS d(email, name, type, status, mac_address, ip_address, room_name, last_seen, metadata)
JOIN users u ON u.email = d.email
LEFT JOIN rooms r ON r.user_id = u.id AND r.name = d.room_name
ON CONFLICT (user_id, mac_address) DO NOTHING;

-- ============================================
-- NOTIFICATIONS TABLE (15+ records per user)
-- ============================================
INSERT INTO notifications (user_id, title, message, type, icon, is_read, read_at, metadata)
SELECT u.id, n.title, n.message, n.type, n.icon, n.is_read, n.read_at, n.metadata::jsonb
FROM (VALUES
-- User 1 (john.doe@example.com) notifications
('john.doe@example.com', 'Device Status Alert', 'Living Room Light is now online', 'alert', 'device', false, NULL, '{"device_id": 1, "device_name": "Living Room Light"}'),
('john.doe@example.com', 'Energy Consumption Alert', 'High energy usage detected in Kitchen', 'reminder', 'energy', false, NULL, '{"device_id": 13, "consumption": 250}'),
('john.doe@example.com', 'Security Alert', 'Motion detected in Bedroom', 'security', 'security', true, NOW() - INTERVAL '1 hour', '{"sensor_id": 8, "location": "Bedroom"}'),
('john.doe@example.com', 'System Update', 'New firmware available for Smart Thermostat', 'system', 'system', false, NULL, '{"device_id": 4, "firmware_version": "2.1.0"}'),
('john.doe@example.com', 'Feature Update', 'New automation features are now available', 'feature', 'feature', false, NULL, '{}'),
('john.doe@example.com', 'Bill Reminder', 'Your monthly energy bill is ready', 'reminder', 'bill', false, NULL, '{"amount": 125.50, "due_date": "2024-01-15"}'),
('john.doe@example.com', 'Device Maintenance', 'Smoke Detector battery is running low', 'reminder', 'maintenance', false, NULL, '{"device_id": 7, "battery_level": 15}'),
('john.doe@example.com', 'Automation Update', 'Welcome Home Automation executed successfully', 'general', 'automation', true, NOW() - INTERVAL '30 minutes', '{"scene_id": 1}'),
('john.doe@example.com', 'Weather Alert', 'Temperature is expected to drop below 0°C tonight', 'alert', 'weather', false, NULL, '{"temperature": -2, "location": "New York"}'),
('john.doe@example.com', 'Community Update', 'New neighbors joined the smart home community', 'general', 'community', false, NULL, '{}'),
('john.doe@example.com', 'Home Invitation', 'You have been invited to manage another home', 'general', 'invitation', false, NULL, '{"home_id": 2, "inviter": "Jane Smith"}'),
('john.doe@example.com', 'User Access Alert', 'New device connected to your network', 'security', 'security', false, NULL, '{"device_mac": "CC:DD:EE:FF:00:11"}'),
('john.doe@example.com', 'Customer Support', 'Your support ticket #12345 has been updated', 'general', 'support', false, NULL, '{"ticket_id": 12345}'),
('john.doe@example.com', 'Feedback Request', 'How was your experience with our app?', 'general', 'feedback', false, NULL, '{}'),
('john.doe@example.com', 'Account Security', 'Login detected from new device', 'security', 'security', true, NOW() - INTERVAL '2 hours', '{"device": "iPhone 14", "location": "New York"}'),
('john.doe@example.com', 'Device Status Alert', 'Window Sensor is offline', 'alert', 'device', false, NULL, '{"device_id": 9, "device_name": "Window Sensor"}'),
('john.doe@example.com', 'Energy Consumption Alert', 'Monthly energy usage is 15% higher than last month', 'reminder', 'energy', false, NULL, '{"increase": 15, "usage": 450}'),
('john.doe@example.com', 'System Update', 'App update available - Version 2.5.0', 'system', 'system', false, NULL, '{"version": "2.5.0"}'),
-- User 2 (jane.smith@example.com) notifications
('jane.smith@example.com', 'Device Status Alert', 'Living Room Light is now online', 'alert', 'device', false, NULL, '{"device_id": 19, "device_name": "Living Room Light"}'),
('jane.smith@example.com', 'Energy Consumption Alert', 'High energy usage detected in Kitchen', 'reminder', 'energy', false, NULL, '{"device_id": 31, "consumption": 280}'),
('jane.smith@example.com', 'Security Alert', 'Motion detected in Bedroom', 'security', 'security', true, NOW() - INTERVAL '1 hour', '{"sensor_id": 26, "location": "Bedroom"}'),
('jane.smith@example.com', 'System Update', 'New firmware available for Smart Thermostat', 'system', 'system', false, NULL, '{"device_id": 22, "firmware_version": "2.1.0"}'),
('jane.smith@example.com', 'Feature Update', 'New automation features are now available', 'feature', 'feature', false, NULL, '{}'),
('jane.smith@example.com', 'Bill Reminder', 'Your monthly energy bill is ready', 'reminder', 'bill', false, NULL, '{"amount": 145.75, "due_date": "2024-01-15"}'),
('jane.smith@example.com', 'Device Maintenance', 'Smoke Detector battery is running low', 'reminder', 'maintenance', false, NULL, '{"device_id": 25, "battery_level": 20}'),
('jane.smith@example.com', 'Automation Update', 'Welcome Home Automation executed successfully', 'general', 'automation', true, NOW() - INTERVAL '30 minutes', '{"scene_id": 1}'),
('jane.smith@example.com', 'Weather Alert', 'Temperature is expected to drop below 0°C tonight', 'alert', 'weather', false, NULL, '{"temperature": -1, "location": "New York"}'),
('jane.smith@example.com', 'Community Update', 'New neighbors joined the smart home community', 'general', 'community', false, NULL, '{}'),
('jane.smith@example.com', 'Home Invitation', 'You have been invited to manage another home', 'general', 'invitation', false, NULL, '{"home_id": 3, "inviter": "Michael Johnson"}'),
('jane.smith@example.com', 'User Access Alert', 'New device connected to your network', 'security', 'security', false, NULL, '{"device_mac": "DD:EE:FF:00:11:22"}'),
('jane.smith@example.com', 'Customer Support', 'Your support ticket #12346 has been updated', 'general', 'support', false, NULL, '{"ticket_id": 12346}'),
('jane.smith@example.com', 'Feedback Request', 'How was your experience with our app?', 'general', 'feedback', false, NULL, '{}'),
('jane.smith@example.com', 'Account Security', 'Login detected from new device', 'security', 'security', true, NOW() - INTERVAL '2 hours', '{"device": "Samsung Galaxy S23", "location": "New York"}'),
('jane.smith@example.com', 'Device Status Alert', 'Window Sensor is offline', 'alert', 'device', false, NULL, '{"device_id": 27, "device_name": "Window Sensor"}'),
('jane.smith@example.com', 'Energy Consumption Alert', 'Monthly energy usage is 12% higher than last month', 'reminder', 'energy', false, NULL, '{"increase": 12, "usage": 520}'),
('jane.smith@example.com', 'System Update', 'App update available - Version 2.5.0', 'system', 'system', false, NULL, '{"version": "2.5.0"}')
) AS n(email, title, message, type, icon, is_read, read_at, metadata)
JOIN users u ON u.email = n.email
ON CONFLICT DO NOTHING;

-- ============================================
-- SMART SCENES TABLE (15+ records)
-- ============================================
-- Note: These scenes reference devices and other scenes that should exist
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, icon, color, is_enabled, order_index)
SELECT u.id, h.id, s.name, s.type, s.condition_logic, s.icon, s.color, s.is_enabled, s.order_index
FROM (VALUES
-- Automation scenes (order_index 0-14)
('john.doe@example.com', 'Main Residence', 'Turn ON All the Lights', 'automation', 'any', 'ic_sun', '#405FF2', true, 0),
('john.doe@example.com', 'Main Residence', 'Go to Office', 'automation', 'any', 'ic_briefcase', '#26A69A', true, 1),
('john.doe@example.com', 'Main Residence', 'Energy Saver Mode', 'automation', 'any', 'ic_energy', '#FFA726', false, 2),
('john.doe@example.com', 'Main Residence', 'Work Mode Activate', 'automation', 'any', 'ic_work', '#42A5F5', true, 3),
('john.doe@example.com', 'Main Residence', 'Night Time Bliss', 'automation', 'any', 'ic_moon', '#6C5CE7', true, 4),
('john.doe@example.com', 'Main Residence', 'Turn on the AC', 'automation', 'any', 'ic_air', '#E74C3C', true, 5),
('john.doe@example.com', 'Main Residence', 'Welcome Home Automation', 'automation', 'any', 'ic_sun', '#FF6B35', true, 6),
('john.doe@example.com', 'Main Residence', 'Bedtime Bliss Automation', 'automation', 'any', 'ic_clock', '#9B59B6', true, 7),
('john.doe@example.com', 'Main Residence', 'Leave Home Automation', 'automation', 'any', 'ic_location', '#E74C3C', true, 8),
('john.doe@example.com', 'Main Residence', 'Morning Routine', 'automation', 'all', 'ic_sunrise', '#F39C12', true, 9),
('john.doe@example.com', 'Main Residence', 'Evening Routine', 'automation', 'all', 'ic_sunset', '#E67E22', true, 10),
('john.doe@example.com', 'Main Residence', 'Weekend Mode', 'automation', 'any', 'ic_weekend', '#3498DB', true, 11),
('john.doe@example.com', 'Main Residence', 'Vacation Mode', 'automation', 'all', 'ic_vacation', '#16A085', false, 12),
('john.doe@example.com', 'Main Residence', 'Party Mode', 'automation', 'any', 'ic_party', '#E91E63', false, 13),
('john.doe@example.com', 'Main Residence', 'Movie Night', 'automation', 'all', 'ic_movie', '#673AB7', true, 14),
-- Tap-to-Run scenes (order_index 0-9)
('john.doe@example.com', 'Main Residence', 'Quick Light On', 'tap_to_run', 'any', 'ic_light', '#FFC107', true, 0),
('john.doe@example.com', 'Main Residence', 'All Devices Off', 'tap_to_run', 'any', 'ic_power', '#607D8B', true, 1),
('john.doe@example.com', 'Main Residence', 'Bedtime Prep', 'tap_to_run', 'any', 'ic_moon', '#6C5CE7', true, 2),
('john.doe@example.com', 'Main Residence', 'Evening Chill', 'tap_to_run', 'any', 'ic_sun', '#FFA726', true, 3),
('john.doe@example.com', 'Main Residence', 'Boost Productivity', 'tap_to_run', 'any', 'ic_briefcase', '#26A69A', true, 4),
('john.doe@example.com', 'Main Residence', 'Get Energized', 'tap_to_run', 'any', 'ic_energy', '#FF9800', true, 5),
('john.doe@example.com', 'Main Residence', 'Home Office', 'tap_to_run', 'any', 'ic_work', '#42A5F5', true, 6),
('john.doe@example.com', 'Main Residence', 'Reading Corner', 'tap_to_run', 'any', 'ic_book', '#8E24AA', true, 7),
('john.doe@example.com', 'Main Residence', 'Outdoor Party', 'tap_to_run', 'any', 'ic_party', '#E91E63', true, 8),
('john.doe@example.com', 'Main Residence', 'Relaxation Mode', 'tap_to_run', 'any', 'ic_relax', '#8E24AA', true, 9),
('john.doe@example.com', 'Main Residence', 'Focus Mode', 'tap_to_run', 'any', 'ic_focus', '#00ACC1', true, 10),
('john.doe@example.com', 'Main Residence', 'Security On', 'tap_to_run', 'any', 'ic_security', '#D32F2F', true, 11),
('john.doe@example.com', 'Main Residence', 'Comfort Zone', 'tap_to_run', 'any', 'ic_comfort', '#5C6BC0', true, 12)
) AS s(user_email, home_name, name, type, condition_logic, icon, color, is_enabled, order_index)
JOIN users u ON u.email = s.user_email
JOIN homes h ON h.user_id = u.id AND h.name = s.home_name
ON CONFLICT DO NOTHING;

-- ============================================
-- SCENE CONDITIONS TABLE (15+ records)
-- ============================================
-- Note: These reference scenes created above
INSERT INTO scene_conditions (scene_id, type, operator, value, unit, location, device_id, device_status, arm_mode, metadata)
SELECT s.id, c.type, c.operator, c.value, c.unit, c.location, d.id, c.device_status, c.arm_mode, c.metadata::jsonb
FROM (VALUES
-- Conditions for scene 1 (Turn ON All the Lights)
('john.doe@example.com', 'Main Residence', 'Turn ON All the Lights', 'schedule_time', 'every_day', 800.0, NULL, NULL, NULL, NULL, NULL, '{"hour": 8, "minute": 0, "repeat": "every_day"}'),
-- Conditions for scene 2 (Go to Office)
('john.doe@example.com', 'Main Residence', 'Go to Office', 'location_arrive_at', 'arrive_at', NULL, NULL, '456 Broadway, New York, NY 10013, USA', NULL, NULL, NULL, '{"latitude": 40.7209, "longitude": -74.0007, "radius": 50}'),
-- Conditions for scene 3 (Energy Saver Mode)
('john.doe@example.com', 'Main Residence', 'Energy Saver Mode', 'arm_mode', 'work', NULL, NULL, NULL, NULL, NULL, 'work', '{"mode": "work"}'),
-- Conditions for scene 4 (Work Mode Activate)
('john.doe@example.com', 'Main Residence', 'Work Mode Activate', 'tap_to_run', 'manual', NULL, NULL, NULL, NULL, NULL, NULL, '{}'),
-- Conditions for scene 5 (Night Time Bliss)
('john.doe@example.com', 'Main Residence', 'Night Time Bliss', 'schedule_time', 'every_day', 2200.0, NULL, NULL, NULL, NULL, NULL, '{"hour": 22, "minute": 0, "repeat": "every_day"}'),
-- Conditions for scene 6 (Turn on the AC)
('john.doe@example.com', 'Main Residence', 'Turn on the AC', 'temperature', '>', 25.0, 'celsius', 'New York City', NULL, NULL, NULL, '{"threshold": 25}'),
('john.doe@example.com', 'Main Residence', 'Turn on the AC', 'humidity', '>', 60.0, 'percent', 'New York City', NULL, NULL, NULL, '{"threshold": 60}'),
-- Conditions for scene 7 (Welcome Home Automation)
('john.doe@example.com', 'Main Residence', 'Welcome Home Automation', 'location_arrive_at', 'arrive_at', NULL, NULL, '701 7th Ave, New York, NY 10036, USA', NULL, NULL, NULL, '{"latitude": 40.7579, "longitude": -73.9877, "radius": 50}'),
-- Conditions for scene 8 (Bedtime Bliss Automation)
('john.doe@example.com', 'Main Residence', 'Bedtime Bliss Automation', 'schedule_time', 'every_day', 2145.0, NULL, NULL, NULL, NULL, NULL, '{"hour": 21, "minute": 45, "repeat": "every_day"}'),
-- Conditions for scene 9 (Leave Home Automation)
('john.doe@example.com', 'Main Residence', 'Leave Home Automation', 'location_leave', 'leave', NULL, NULL, '701 7th Ave, New York, NY 10036, USA', NULL, NULL, NULL, '{"latitude": 40.7579, "longitude": -73.9877, "radius": 50}'),
-- Conditions for scene 10 (Morning Routine)
('john.doe@example.com', 'Main Residence', 'Morning Routine', 'schedule_time', 'every_weekday', 700.0, NULL, NULL, NULL, NULL, NULL, '{"hour": 7, "minute": 0, "repeat": "every_weekday"}'),
-- Conditions for scene 11 (Evening Routine)
('john.doe@example.com', 'Main Residence', 'Evening Routine', 'schedule_time', 'every_day', 1800.0, NULL, NULL, NULL, NULL, NULL, '{"hour": 18, "minute": 0, "repeat": "every_day"}'),
-- Conditions for scene 12 (Weekend Mode)
('john.doe@example.com', 'Main Residence', 'Weekend Mode', 'schedule_time', 'every_weekend', 900.0, NULL, NULL, NULL, NULL, NULL, '{"hour": 9, "minute": 0, "repeat": "every_weekend"}'),
-- Conditions for scene 13 (Vacation Mode)
('john.doe@example.com', 'Main Residence', 'Vacation Mode', 'arm_mode', 'arm_away', NULL, NULL, NULL, NULL, NULL, 'arm_away', '{"mode": "arm_away"}'),
-- Conditions for scene 14 (Party Mode)
('john.doe@example.com', 'Main Residence', 'Party Mode', 'tap_to_run', 'manual', NULL, NULL, NULL, NULL, NULL, NULL, '{}'),
-- Conditions for scene 15 (Movie Night)
('john.doe@example.com', 'Main Residence', 'Movie Night', 'schedule_time', 'custom', 2000.0, NULL, NULL, NULL, NULL, NULL, '{"hour": 20, "minute": 0, "repeat": "custom", "days": ["friday", "saturday"]}'),
-- Additional conditions for complex automations
('john.doe@example.com', 'Main Residence', 'Turn on the AC', 'device_status', '=', NULL, NULL, NULL, 'Kitchen Temperature Sensor', 'online', NULL, '{}')
) AS c(user_email, home_name, scene_name, type, operator, value, unit, location, device_name, device_status, arm_mode, metadata)
JOIN users u ON u.email = c.user_email
JOIN homes h ON h.user_id = u.id AND h.name = c.home_name
JOIN smart_scenes s ON s.user_id = u.id AND s.home_id = h.id AND s.name = c.scene_name
LEFT JOIN devices d ON d.user_id = u.id AND d.name = c.device_name
ON CONFLICT DO NOTHING;

-- ============================================
-- SCENE TASKS TABLE (15+ records)
-- ============================================
INSERT INTO scene_tasks (scene_id, type, device_id, device_name, room_name, function, scene_id_target, scene_name, arm_mode, notification_message, delay_seconds, order_index, metadata)
SELECT 
    s.id,
    t.type,
    d.id,
    t.device_name,
    t.room_name,
    t.function,
    st.id,
    t.scene_name_display,
    t.arm_mode,
    t.notification_message,
    t.delay_seconds,
    t.order_index,
    t.metadata::jsonb
FROM (VALUES
-- Tasks for scene 1 (Turn ON All the Lights)
('john.doe@example.com', 'Main Residence', 'Turn ON All the Lights', 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
-- Tasks for scene 2 (Go to Office)
('john.doe@example.com', 'Main Residence', 'Go to Office', 'control_device', 'Smart Thermostat', 'Smart Thermostat', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Go to Office', 'delay', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 30, NULL, 1, '{}'),
-- Tasks for scene 3 (Energy Saver Mode)
('john.doe@example.com', 'Main Residence', 'Energy Saver Mode', 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'energy_saver', 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Energy Saver Mode', 'send_notification', NULL, NULL, NULL, NULL, NULL, NULL, 'Energy Saver Mode Activated', NULL, NULL, 1, '{}'),
-- Tasks for scene 4 (Work Mode Activate)
('john.doe@example.com', 'Main Residence', 'Work Mode Activate', 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'work', 0, '{}'),
-- Tasks for scene 5 (Night Time Bliss)
('john.doe@example.com', 'Main Residence', 'Night Time Bliss', 'control_device', 'Bedroom Light', 'Bedroom Lights', 'Bedroom', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Night Time Bliss', 'control_device', 'Smart Thermostat', 'Thermostat', 'Living Room', 'SET_20', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
-- Tasks for scene 6 (Turn on the AC)
('john.doe@example.com', 'Main Residence', 'Turn on the AC', 'control_device', 'Air Conditioner', 'Air Conditioner', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
-- Tasks for scene 7 (Welcome Home Automation)
('john.doe@example.com', 'Main Residence', 'Welcome Home Automation', 'select_scene', NULL, NULL, NULL, NULL, 'Turn ON All the Lights', 'Turn ON All the Lights', NULL, NULL, NULL, 0, '{}'),
-- Tasks for scene 8 (Bedtime Bliss Automation)
('john.doe@example.com', 'Main Residence', 'Bedtime Bliss Automation', 'delay', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 930, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Bedtime Bliss Automation', 'control_device', NULL, 'All Lights', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
-- Tasks for scene 9 (Leave Home Automation)
('john.doe@example.com', 'Main Residence', 'Leave Home Automation', 'control_device', NULL, 'All Devices', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
-- Tasks for scene 10 (Morning Routine)
('john.doe@example.com', 'Main Residence', 'Morning Routine', 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Morning Routine', 'control_device', 'Smart Thermostat', 'Thermostat', NULL, 'SET_22', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
('john.doe@example.com', 'Main Residence', 'Morning Routine', 'send_notification', NULL, NULL, NULL, NULL, NULL, NULL, 'Good morning! Your home is ready.', NULL, NULL, 2, '{}'),
-- Tasks for scene 11 (Evening Routine)
('john.doe@example.com', 'Main Residence', 'Evening Routine', 'control_device', 'Living Room Light', 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Evening Routine', 'control_device', 'Smart TV', 'Smart TV', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
-- Tasks for scene 12 (Weekend Mode)
('john.doe@example.com', 'Main Residence', 'Weekend Mode', 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Weekend Mode', 'control_device', 'Smart Thermostat', 'Thermostat', NULL, 'SET_24', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
-- Tasks for scene 13 (Vacation Mode)
('john.doe@example.com', 'Main Residence', 'Vacation Mode', 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'arm_away', 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Vacation Mode', 'control_device', NULL, 'All Devices', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
('john.doe@example.com', 'Main Residence', 'Vacation Mode', 'send_notification', NULL, NULL, NULL, NULL, NULL, NULL, 'Vacation mode activated. Your home is secure.', NULL, NULL, 2, '{}'),
-- Tasks for scene 14 (Party Mode)
('john.doe@example.com', 'Main Residence', 'Party Mode', 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Party Mode', 'control_device', 'Smart Speaker', 'Smart Speaker', NULL, 'PLAY_MUSIC', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
-- Tasks for scene 15 (Movie Night)
('john.doe@example.com', 'Main Residence', 'Movie Night', 'control_device', 'Living Room Light', 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Movie Night', 'control_device', 'Smart TV', 'Smart TV', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
-- Tasks for scene 16-28 (Tap-to-Run scenes)
('john.doe@example.com', 'Main Residence', 'Quick Light On', 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'All Devices Off', 'control_device', NULL, 'All Devices', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Bedtime Prep', 'control_device', 'Bedroom Light', 'Bedroom Lights', 'Bedroom', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Evening Chill', 'control_device', 'Living Room Light', 'Living Room Lights', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Boost Productivity', 'control_device', NULL, 'Office Lights', 'Study Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Get Energized', 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Get Energized', 'control_device', 'Smart Speaker', 'Smart Speaker', NULL, 'PLAY_ENERGY', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
('john.doe@example.com', 'Main Residence', 'Home Office', 'control_device', NULL, 'Office Lights', 'Study Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Home Office', 'control_device', 'Smart Thermostat', 'Thermostat', 'Study Room', 'SET_22', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
('john.doe@example.com', 'Main Residence', 'Reading Corner', 'control_device', 'Living Room Light', 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Outdoor Party', 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Outdoor Party', 'control_device', 'Smart Speaker', 'Smart Speaker', NULL, 'PLAY_PARTY', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
('john.doe@example.com', 'Main Residence', 'Relaxation Mode', 'control_device', 'Living Room Light', 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Relaxation Mode', 'control_device', 'Smart Speaker', 'Smart Speaker', NULL, 'PLAY_RELAX', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
('john.doe@example.com', 'Main Residence', 'Focus Mode', 'control_device', NULL, 'Office Lights', 'Study Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Focus Mode', 'control_device', 'Smart Speaker', 'Smart Speaker', NULL, 'MUTE', NULL, NULL, NULL, NULL, NULL, 1, '{}'),
('john.doe@example.com', 'Main Residence', 'Security On', 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'arm_stay', 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Comfort Zone', 'control_device', 'Smart Thermostat', 'Thermostat', NULL, 'SET_22', NULL, NULL, NULL, NULL, NULL, 0, '{}'),
('john.doe@example.com', 'Main Residence', 'Comfort Zone', 'control_device', 'Living Room Light', 'Living Room Lights', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 1, '{}')
) AS t(user_email, home_name, scene_name, type, device_name_for_lookup, device_name, room_name, function, target_scene_name, scene_name_display, notification_message, delay_seconds, arm_mode, order_index, metadata)
JOIN users u ON u.email = t.user_email
JOIN homes h ON h.user_id = u.id AND h.name = t.home_name
JOIN smart_scenes s ON s.user_id = u.id AND s.home_id = h.id AND s.name = t.scene_name
LEFT JOIN devices d ON d.user_id = u.id AND d.name = t.device_name_for_lookup
LEFT JOIN smart_scenes st ON st.user_id = u.id AND st.home_id = h.id AND st.name = t.target_scene_name
ON CONFLICT DO NOTHING;

-- ============================================
-- SCENE EXECUTION LOGS TABLE (20+ records)
-- ============================================
-- Insert execution logs for various scenes showing success and failure cases
INSERT INTO scene_execution_logs (scene_id, scene_name, user_id, home_id, status, error_message, execution_timestamp, metadata)
SELECT 
    s.id,
    l.scene_name,
    u.id,
    h.id,
    l.status,
    l.error_message,
    l.execution_timestamp,
    l.metadata::jsonb
FROM (VALUES
-- Today's logs (December 24, 2024)
('john.doe@example.com', 'Main Residence', 'Turn on the AC', 'succeeded', NULL, NOW() - INTERVAL '1 hour', '{}'),
('john.doe@example.com', 'Main Residence', 'Welcome Home Automation', 'succeeded', NULL, NOW() - INTERVAL '2 hours', '{}'),
('john.doe@example.com', 'Main Residence', 'Quick Light On', 'succeeded', NULL, NOW() - INTERVAL '3 hours', '{}'),
('john.doe@example.com', 'Main Residence', 'Boost Productivity', 'failed', 'Device connection timeout', NOW() - INTERVAL '4 hours', '{"device_id": 5}'),
('john.doe@example.com', 'Main Residence', 'Go to Office', 'succeeded', NULL, NOW() - INTERVAL '5 hours', '{}'),
('john.doe@example.com', 'Main Residence', 'Get Energized', 'succeeded', NULL, NOW() - INTERVAL '6 hours', '{}'),
('john.doe@example.com', 'Main Residence', 'Outdoor Party', 'succeeded', NULL, NOW() - INTERVAL '7 hours', '{}'),
('john.doe@example.com', 'Main Residence', 'Work Mode Activate', 'succeeded', NULL, NOW() - INTERVAL '8 hours', '{}'),
-- Yesterday's logs (December 23, 2024)
('john.doe@example.com', 'Main Residence', 'Bedtime Bliss Automation', 'succeeded', NULL, NOW() - INTERVAL '1 day' - INTERVAL '2 hours', '{}'),
('john.doe@example.com', 'Main Residence', 'Turn ON All the Lights', 'succeeded', NULL, NOW() - INTERVAL '1 day' - INTERVAL '16 hours', '{}'),
('john.doe@example.com', 'Main Residence', 'Morning Routine', 'succeeded', NULL, NOW() - INTERVAL '1 day' - INTERVAL '17 hours', '{}'),
-- Day before yesterday (December 22, 2024)
('john.doe@example.com', 'Main Residence', 'Evening Routine', 'succeeded', NULL, NOW() - INTERVAL '2 days' - INTERVAL '6 hours', '{}'),
('john.doe@example.com', 'Main Residence', 'Weekend Mode', 'succeeded', NULL, NOW() - INTERVAL '2 days' - INTERVAL '15 hours', '{}'),
-- More recent logs for today
('john.doe@example.com', 'Main Residence', 'Bedtime Prep', 'succeeded', NULL, NOW() - INTERVAL '30 minutes', '{}'),
('john.doe@example.com', 'Main Residence', 'Evening Chill', 'succeeded', NULL, NOW() - INTERVAL '1 hour 30 minutes', '{}'),
('john.doe@example.com', 'Main Residence', 'Home Office', 'succeeded', NULL, NOW() - INTERVAL '2 hours 15 minutes', '{}'),
('john.doe@example.com', 'Main Residence', 'Reading Corner', 'succeeded', NULL, NOW() - INTERVAL '3 hours 45 minutes', '{}'),
-- Failed executions
('john.doe@example.com', 'Main Residence', 'Energy Saver Mode', 'failed', 'Network error: Unable to connect to device', NOW() - INTERVAL '12 hours', '{"device_id": 3, "error_code": "NETWORK_ERROR"}'),
('john.doe@example.com', 'Main Residence', 'Security On', 'failed', 'Device offline', NOW() - INTERVAL '18 hours', '{"device_id": 8}'),
('john.doe@example.com', 'Main Residence', 'Comfort Zone', 'succeeded', NULL, NOW() - INTERVAL '20 hours', '{}'),
-- Older logs
('john.doe@example.com', 'Main Residence', 'Movie Night', 'succeeded', NULL, NOW() - INTERVAL '3 days' - INTERVAL '4 hours', '{}'),
('john.doe@example.com', 'Main Residence', 'Focus Mode', 'succeeded', NULL, NOW() - INTERVAL '4 days' - INTERVAL '2 hours', '{}'),
('john.doe@example.com', 'Main Residence', 'Relaxation Mode', 'succeeded', NULL, NOW() - INTERVAL '5 days' - INTERVAL '6 hours', '{}')
) AS l(user_email, home_name, scene_name, status, error_message, execution_timestamp, metadata)
JOIN users u ON u.email = l.user_email
JOIN homes h ON h.user_id = u.id AND h.name = l.home_name
JOIN smart_scenes s ON s.user_id = u.id AND s.home_id = h.id AND s.name = l.scene_name
ON CONFLICT DO NOTHING;

-- ============================================
-- SUMMARY
-- ============================================
-- This script creates:
-- - 18 users
-- - 18 homes
-- - 33 rooms (15+ per user)
-- - 34 devices (17 per user for 2 users)
-- - 36 notifications (18 per user for 2 users)
-- - 28 smart scenes (15 automation + 13 tap_to_run)
-- - 17 scene conditions
-- - 33 scene tasks
-- - 23 scene execution logs
-- Total: More than 10 records for each table
