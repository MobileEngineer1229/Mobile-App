-- Migration: Add Demo Notifications for demo@smartify.com
-- This migration adds sample notifications for the demo user with both general and smart_home categories

-- First, ensure the demo user exists (if not, create them)
INSERT INTO users (email, password, first_name, last_name, phone)
VALUES ('demo@smartify.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Demo', 'User', '+1-555-0199')
ON CONFLICT (email) DO NOTHING;

-- Delete existing notifications for demo user to avoid duplicates
DELETE FROM notifications WHERE user_id = (SELECT id FROM users WHERE email = 'demo@smartify.com');

-- Insert General Notifications (category: general)
INSERT INTO notifications (user_id, title, message, type, icon, is_read, read_at, metadata, created_at, category)
SELECT 
    u.id,
    n.title,
    n.message,
    n.type,
    n.icon,
    n.is_read,
    n.read_at,
    n.metadata::jsonb,
    n.created_at,
    n.category
FROM (VALUES
    -- Today's General Notifications
    ('Account Security Alert', 
     'We''ve noticed some unusual activity on your account. Please review your recent logins and update your password if necessary.', 
     'security', 'security', false, NULL, '{"badge": "LOCK"}', NOW() - INTERVAL '30 minutes', 'general'),
    
    ('System Update Available', 
     'A new system update is ready for installation. It includes performance improvements and bug fixes.', 
     'system', 'update', false, NULL, '{}', NOW() - INTERVAL '2 hours', 'general'),
    
    ('Welcome to Smartify!', 
     'Thank you for joining Smartify. Explore all the features to make your home smarter.', 
     'general', 'welcome', true, NOW() - INTERVAL '1 day', '{}', NOW() - INTERVAL '3 hours', 'general'),
    
    -- Yesterday's General Notifications
    ('Password Reset Successful', 
     'Your password has been successfully reset. If you didn''t request this change, please contact support immediately.', 
     'security', 'password', true, NOW() - INTERVAL '1 day', '{}', NOW() - INTERVAL '1 day' - INTERVAL '4 hours', 'general'),
    
    ('Exciting New Feature', 
     'We''ve just launched a new feature that will enhance your user experience. Check it out now!', 
     'feature', 'feature', true, NOW() - INTERVAL '1 day', '{"badge": "NEW"}', NOW() - INTERVAL '1 day' - INTERVAL '8 hours', 'general'),
    
    ('Event Reminder', 
     'Don''t forget about your scheduled event tomorrow at 2:00 PM.', 
     'reminder', 'event', false, NULL, '{}', NOW() - INTERVAL '1 day' - INTERVAL '7 hours', 'general'),
    
    ('Monthly Newsletter', 
     'Check out this month''s smart home tips and tricks to maximize your energy savings.', 
     'general', 'newsletter', true, NOW() - INTERVAL '2 days', '{}', NOW() - INTERVAL '2 days' - INTERVAL '3 hours', 'general'),
    
    ('Account Verified', 
     'Your email address has been verified successfully. You now have full access to all features.', 
     'system', 'verified', true, NOW() - INTERVAL '3 days', '{}', NOW() - INTERVAL '3 days' - INTERVAL '5 hours', 'general'),
    
    ('Feedback Request', 
     'How was your experience with our app? We''d love to hear your thoughts!', 
     'general', 'feedback', false, NULL, '{}', NOW() - INTERVAL '1 day' - INTERVAL '10 hours', 'general'),
    
    ('Customer Support Update', 
     'Your support ticket #45678 has been resolved. Please let us know if you need further assistance.', 
     'general', 'support', true, NOW() - INTERVAL '2 days', '{"ticket_id": 45678}', NOW() - INTERVAL '2 days' - INTERVAL '6 hours', 'general'),

    -- Smart Home Notifications (category: smart_home)
    -- Today's Smart Home Notifications
    ('Device Status Alert', 
     'Your living room light has been turned on. Is this expected?', 
     'alert', 'device', false, NULL, '{"device_name": "Living Room Light"}', NOW() - INTERVAL '1 hour', 'smart_home'),
    
    ('Energy Consumption Alert', 
     'Your energy usage today is 15% higher than usual. Check your devices.', 
     'alert', 'energy', false, NULL, '{"increase_percent": 15}', NOW() - INTERVAL '2 hours 30 minutes', 'smart_home'),
    
    ('Device Maintenance Reminder', 
     'It''s time to check your air conditioner filter. Last maintenance was 3 months ago.', 
     'reminder', 'maintenance', false, NULL, '{"device_name": "Air Conditioner", "months_since": 3}', NOW() - INTERVAL '3 hours', 'smart_home'),
    
    ('Temperature Alert', 
     'Living room temperature has dropped below 18°C. Consider turning on the heater.', 
     'alert', 'temperature', false, NULL, '{"temperature": 17.5, "location": "Living Room"}', NOW() - INTERVAL '4 hours', 'smart_home'),
    
    ('Smart Scene Executed', 
     'Your "Good Morning" automation has been activated successfully.', 
     'feature', 'automation', true, NOW() - INTERVAL '5 hours', '{"scene_name": "Good Morning"}', NOW() - INTERVAL '7 hours', 'smart_home'),
    
    -- Yesterday's Smart Home Notifications
    ('Automation Triggered', 
     'Your ''Good Morning'' automation has been activated successfully.', 
     'feature', 'device', true, NOW() - INTERVAL '1 day' + INTERVAL '6 hours', '{"scene_id": 1}', NOW() - INTERVAL '1 day' + INTERVAL '6 hours', 'smart_home'),
    
    ('Security Alert', 
     'Motion detected in your backyard at 11:23 PM. Check your security camera.', 
     'security', 'security', false, NULL, '{"location": "Backyard", "time": "23:23"}', NOW() - INTERVAL '1 day' - INTERVAL '30 minutes', 'smart_home'),
    
    ('Device Offline', 
     'Your bedroom sensor has been offline for more than 2 hours. Please check the device.', 
     'alert', 'device', true, NOW() - INTERVAL '1 day', '{"device_name": "Bedroom Sensor", "offline_hours": 2}', NOW() - INTERVAL '1 day' - INTERVAL '5 hours', 'smart_home'),
    
    ('Weekly Energy Report', 
     'Your weekly energy consumption report is ready. You used 125 kWh this week.', 
     'reminder', 'energy', true, NOW() - INTERVAL '1 day', '{"consumption_kwh": 125, "period": "weekly"}', NOW() - INTERVAL '1 day' - INTERVAL '10 hours', 'smart_home'),
    
    ('New Device Connected', 
     'A new device "Kitchen Smart Plug" has been added to your home network.', 
     'feature', 'device', true, NOW() - INTERVAL '2 days', '{"device_name": "Kitchen Smart Plug"}', NOW() - INTERVAL '2 days' - INTERVAL '2 hours', 'smart_home'),
    
    ('Humidity Alert', 
     'Bathroom humidity level is above 70%. Consider turning on the exhaust fan.', 
     'alert', 'humidity', false, NULL, '{"humidity": 72, "location": "Bathroom"}', NOW() - INTERVAL '2 days' - INTERVAL '8 hours', 'smart_home'),
    
    ('Door Lock Alert', 
     'Front door was unlocked at 10:45 PM. Did you intend to unlock it?', 
     'security', 'lock', true, NOW() - INTERVAL '3 days', '{"device_name": "Front Door Lock", "time": "22:45"}', NOW() - INTERVAL '3 days' - INTERVAL '1 hour', 'smart_home'),
    
    ('Air Quality Update', 
     'Air quality in your home is excellent today. AQI: 35', 
     'general', 'air', true, NOW() - INTERVAL '2 days', '{"aqi": 35, "status": "excellent"}', NOW() - INTERVAL '2 days' - INTERVAL '12 hours', 'smart_home'),
    
    ('Smart Thermostat Adjusted', 
     'Thermostat automatically adjusted to 22°C based on your preferences.', 
     'feature', 'thermostat', true, NOW() - INTERVAL '4 hours', '{"temperature": 22, "mode": "auto"}', NOW() - INTERVAL '4 hours', 'smart_home')
    
) AS n(title, message, type, icon, is_read, read_at, metadata, created_at, category)
CROSS JOIN users u
WHERE u.email = 'demo@smartify.com'
ON CONFLICT DO NOTHING;

-- Also add notifications for john.doe@example.com with category field
UPDATE notifications 
SET category = CASE 
    WHEN type IN ('security', 'alert') THEN 'smart_home'
    ELSE 'general'
END
WHERE category IS NULL;

-- Verify notifications were added
-- SELECT COUNT(*) as total_notifications, category 
-- FROM notifications 
-- WHERE user_id = (SELECT id FROM users WHERE email = 'demo@smartify.com')
-- GROUP BY category;
