-- Demo data for Smart Scenes feature
-- This script inserts sample scenes, conditions, and tasks for testing
-- Note: Adjust user_id and home_id based on your demo user and home

-- Clear existing demo scenes (optional - comment out if you want to keep existing data)
-- DELETE FROM scene_tasks WHERE scene_id IN (SELECT id FROM smart_scenes WHERE user_id = 1);
-- DELETE FROM scene_conditions WHERE scene_id IN (SELECT id FROM smart_scenes WHERE user_id = 1);
-- DELETE FROM smart_scenes WHERE user_id = 1;

-- Scene 1: Turn ON All the Lights (Automation)
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, is_enabled)
VALUES (1, 1, 'Turn ON All the Lights', 'automation', 'any', true);

INSERT INTO scene_conditions (scene_id, type, operator, metadata)
SELECT id, 'time', 'schedule', '{"time": "08:00", "days": ["monday", "tuesday", "wednesday", "thursday", "friday"]}'::jsonb
FROM smart_scenes WHERE user_id = 1 AND name = 'Turn ON All the Lights';

INSERT INTO scene_tasks (scene_id, type, device_name, function, order_index)
SELECT id, 'control_device', 'All Lights', 'ON', 0
FROM smart_scenes WHERE user_id = 1 AND name = 'Turn ON All the Lights';

-- Scene 2: Go to Office (Automation)
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, is_enabled)
VALUES (1, 1, 'Go to Office', 'automation', 'any', true);

INSERT INTO scene_conditions (scene_id, type, operator, location)
SELECT id, type, operator, location
FROM smart_scenes,
LATERAL (VALUES 
    ('location', 'arrive', 'Office'::VARCHAR),
    ('time', 'schedule', NULL::VARCHAR)
) AS cond(type, operator, location)
WHERE user_id = 1 AND name = 'Go to Office';

INSERT INTO scene_tasks (scene_id, type, device_name, function, order_index, delay_seconds)
SELECT id, type, device_name, function, order_index, delay_seconds
FROM smart_scenes,
LATERAL (VALUES 
    ('control_device', 'Thermostat', 'ON', 0, NULL::INTEGER),
    ('delay', NULL, NULL, 1, 30)
) AS task(type, device_name, function, order_index, delay_seconds)
WHERE user_id = 1 AND name = 'Go to Office';

-- Scene 3: Energy Saver Mode (Automation)
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, is_enabled)
VALUES (1, 1, 'Energy Saver Mode', 'automation', 'any', false);

INSERT INTO scene_conditions (scene_id, type, operator, metadata)
SELECT id, 'arm_mode', 'work', '{"mode": "work"}'::jsonb
FROM smart_scenes WHERE user_id = 1 AND name = 'Energy Saver Mode';

INSERT INTO scene_tasks (scene_id, type, arm_mode, order_index)
SELECT id, 'change_arm_mode', 'energy_saver', 0
FROM smart_scenes WHERE user_id = 1 AND name = 'Energy Saver Mode';

INSERT INTO scene_tasks (scene_id, type, notification_message, order_index)
SELECT id, 'send_notification', 'Energy Saver Mode Activated', 1
FROM smart_scenes WHERE user_id = 1 AND name = 'Energy Saver Mode';

-- Scene 4: Work Mode Activate (Automation)
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, is_enabled)
VALUES (1, 1, 'Work Mode Activate', 'automation', 'any', true);

INSERT INTO scene_conditions (scene_id, type, operator)
SELECT id, 'tap_to_run', 'manual'
FROM smart_scenes WHERE user_id = 1 AND name = 'Work Mode Activate';

INSERT INTO scene_tasks (scene_id, type, arm_mode, order_index)
SELECT id, 'change_arm_mode', 'work', 0
FROM smart_scenes WHERE user_id = 1 AND name = 'Work Mode Activate';

-- Scene 5: Night Time Bliss (Automation)
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, is_enabled)
VALUES (1, 1, 'Night Time Bliss', 'automation', 'any', true);

INSERT INTO scene_conditions (scene_id, type, operator, metadata)
SELECT id, 'time', 'schedule', '{"time": "22:00", "days": ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"]}'::jsonb
FROM smart_scenes WHERE user_id = 1 AND name = 'Night Time Bliss';

INSERT INTO scene_tasks (scene_id, type, device_name, function, order_index)
SELECT id, type, device_name, function, order_index
FROM smart_scenes,
LATERAL (VALUES 
    ('control_device', 'Bedroom Lights', 'DIM', 0),
    ('control_device', 'Thermostat', 'SET_20', 1)
) AS task(type, device_name, function, order_index)
WHERE user_id = 1 AND name = 'Night Time Bliss';

-- Scene 6: Turn on the AC (Automation) - Example with temperature and humidity conditions
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, is_enabled)
VALUES (1, 1, 'Turn on the AC', 'automation', 'any', true);

INSERT INTO scene_conditions (scene_id, type, operator, value, unit, location)
SELECT id, type, operator, value, unit, location
FROM smart_scenes,
LATERAL (VALUES 
    ('temperature', '>', 20.0, 'C', 'New York City'),
    ('humidity', 'dry', NULL::DOUBLE PRECISION, NULL, 'New York City')
) AS cond(type, operator, value, unit, location)
WHERE user_id = 1 AND name = 'Turn on the AC';

INSERT INTO scene_tasks (scene_id, type, device_name, room_name, function, order_index)
SELECT id, 'control_device', 'Air Conditioner', 'Living Room', 'ON', 0
FROM smart_scenes WHERE user_id = 1 AND name = 'Turn on the AC';

-- Tap-to-Run Scenes
-- Scene 7: Quick Light On
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, is_enabled)
VALUES (1, 1, 'Quick Light On', 'tap_to_run', 'any', true);

INSERT INTO scene_tasks (scene_id, type, device_name, function, order_index)
SELECT id, 'control_device', 'All Lights', 'ON', 0
FROM smart_scenes WHERE user_id = 1 AND name = 'Quick Light On';

-- Scene 8: All Devices Off
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, is_enabled)
VALUES (1, 1, 'All Devices Off', 'tap_to_run', 'any', true);

INSERT INTO scene_tasks (scene_id, type, device_name, function, order_index)
SELECT id, 'control_device', 'All Devices', 'OFF', 0
FROM smart_scenes WHERE user_id = 1 AND name = 'All Devices Off';

-- Scene 9: Welcome Home Automation (Location-based: Arrive at)
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, icon, color, is_enabled)
VALUES (1, 1, 'Welcome Home Automation', 'automation', 'any', 'ic_sun', '#FF6B35', true);

INSERT INTO scene_conditions (scene_id, type, operator, location, metadata)
SELECT id, 'location_arrive_at', 'arrive_at', '701 7th Ave, New York, 10036, USA', 
    '{"latitude": 40.7579, "longitude": -73.9877, "radius": 50}'::jsonb
FROM smart_scenes WHERE user_id = 1 AND name = 'Welcome Home Automation';

INSERT INTO scene_tasks (scene_id, type, scene_id_target, scene_name, order_index)
SELECT id, 'select_scene', 
    (SELECT id FROM smart_scenes WHERE name = 'Turn ON All the Lights' AND user_id = 1),
    'Turn ON All the Lights', 0
FROM smart_scenes WHERE user_id = 1 AND name = 'Welcome Home Automation';

-- Scene 10: Bedtime Bliss Automation (Schedule Time + Delay)
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, icon, color, is_enabled)
VALUES (1, 1, 'Bedtime Bliss Automation', 'automation', 'any', 'ic_clock', '#9B59B6', true);

INSERT INTO scene_conditions (scene_id, type, operator, value, metadata)
SELECT id, 'schedule_time', 'every_day', 2145.0, 
    '{"hour": 21, "minute": 45, "repeat": "every_day"}'::jsonb
FROM smart_scenes WHERE user_id = 1 AND name = 'Bedtime Bliss Automation';

INSERT INTO scene_tasks (scene_id, type, delay_seconds, order_index)
SELECT id, 'delay', 930, 0  -- 15 minutes 30 seconds
FROM smart_scenes WHERE user_id = 1 AND name = 'Bedtime Bliss Automation';

INSERT INTO scene_tasks (scene_id, type, device_name, function, order_index)
SELECT id, 'control_device', 'All Lights', 'OFF', 1
FROM smart_scenes WHERE user_id = 1 AND name = 'Bedtime Bliss Automation';

-- Scene 11: Leave Home Automation (Location-based: Leave)
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, icon, color, is_enabled)
VALUES (1, 1, 'Leave Home Automation', 'automation', 'any', 'ic_location', '#E74C3C', true);

INSERT INTO scene_conditions (scene_id, type, operator, location, metadata)
SELECT id, 'location_leave', 'leave', '701 7th Ave, New York, 10036, USA', 
    '{"latitude": 40.7579, "longitude": -73.9877, "radius": 50}'::jsonb
FROM smart_scenes WHERE user_id = 1 AND name = 'Leave Home Automation';

INSERT INTO scene_tasks (scene_id, type, device_name, function, order_index)
SELECT id, 'control_device', 'All Devices', 'OFF', 0
FROM smart_scenes WHERE user_id = 1 AND name = 'Leave Home Automation';

-- Tap-to-Run Scenes (for Select Scene task)
-- Scene 12: Bedtime Prep (Tap-to-Run)
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, icon, color, is_enabled)
VALUES (1, 1, 'Bedtime Prep', 'tap_to_run', 'any', 'ic_moon', '#6C5CE7', true);

INSERT INTO scene_tasks (scene_id, type, device_name, function, order_index)
SELECT id, 'control_device', 'Bedroom Lights', 'DIM', 0
FROM smart_scenes WHERE user_id = 1 AND name = 'Bedtime Prep';

-- Scene 13: Evening Chill (Tap-to-Run)
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, icon, color, is_enabled)
VALUES (1, 1, 'Evening Chill', 'tap_to_run', 'any', 'ic_sun', '#FFA726', true);

INSERT INTO scene_tasks (scene_id, type, device_name, function, order_index)
SELECT id, 'control_device', 'Living Room Lights', 'ON', 0
FROM smart_scenes WHERE user_id = 1 AND name = 'Evening Chill';

-- Scene 14: Boost Productivity (Tap-to-Run)
INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, icon, color, is_enabled)
VALUES (1, 1, 'Boost Productivity', 'tap_to_run', 'any', 'ic_briefcase', '#26A69A', true);

INSERT INTO scene_tasks (scene_id, type, device_name, function, order_index)
SELECT id, 'control_device', 'Office Lights', 'ON', 0
FROM smart_scenes WHERE user_id = 1 AND name = 'Boost Productivity';
