-- Migration: Add Demo Home Members for demo@smartify.com
-- This migration adds sample home members for the demo user's home

-- First, ensure the demo user exists (if not, create them)
INSERT INTO users (email, password, first_name, last_name, phone)
VALUES ('demo@smartify.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Demo', 'User', '+1-555-0199')
ON CONFLICT (email) DO NOTHING;

-- Create sample users who will be home members
INSERT INTO users (email, password, first_name, last_name, phone)
VALUES 
    ('jenny.wilson@smartify.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Jenny', 'Wilson', '+1-555-0201'),
    ('robert.hawkins@smartify.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Robert', 'Hawkins', '+1-555-0202'),
    ('sarah.wilona@smartify.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Sarah', 'Wilona', '+1-555-0203'),
    ('michael.chen@smartify.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Michael', 'Chen', '+1-555-0204')
ON CONFLICT (email) DO NOTHING;

-- Create a home for the demo user if it doesn't exist
INSERT INTO homes (user_id, name, address, latitude, longitude, country, is_primary)
SELECT u.id, 'Demo Home', '123 Smart Street, New York, NY 10001, USA', 40.7128, -74.0060, 'United States', true
FROM users u
WHERE u.email = 'demo@smartify.com'
ON CONFLICT (user_id, name) DO NOTHING;

-- Delete existing home_members for the demo home to avoid duplicates
DELETE FROM home_members 
WHERE home_id IN (
    SELECT h.id FROM homes h
    JOIN users u ON h.user_id = u.id
    WHERE u.email = 'demo@smartify.com'
);

-- Add the demo user as owner of their home
INSERT INTO home_members (home_id, user_id, role, added_by, created_at)
SELECT h.id, u.id, 'owner', u.id, NOW()
FROM homes h
JOIN users owner_user ON h.user_id = owner_user.id
JOIN users u ON u.email = 'demo@smartify.com'
WHERE owner_user.email = 'demo@smartify.com'
  AND h.name = 'Demo Home'
ON CONFLICT DO NOTHING;

-- Add Jenny Wilson as admin
INSERT INTO home_members (home_id, user_id, role, added_by, created_at)
SELECT h.id, member.id, 'admin', owner_user.id, NOW() - INTERVAL '30 days'
FROM homes h
JOIN users owner_user ON h.user_id = owner_user.id
JOIN users member ON member.email = 'jenny.wilson@smartify.com'
WHERE owner_user.email = 'demo@smartify.com'
  AND h.name = 'Demo Home'
ON CONFLICT DO NOTHING;

-- Add Robert Hawkins as member
INSERT INTO home_members (home_id, user_id, role, added_by, created_at)
SELECT h.id, member.id, 'member', owner_user.id, NOW() - INTERVAL '20 days'
FROM homes h
JOIN users owner_user ON h.user_id = owner_user.id
JOIN users member ON member.email = 'robert.hawkins@smartify.com'
WHERE owner_user.email = 'demo@smartify.com'
  AND h.name = 'Demo Home'
ON CONFLICT DO NOTHING;

-- Add Sarah Wilona as member
INSERT INTO home_members (home_id, user_id, role, added_by, created_at)
SELECT h.id, member.id, 'member', owner_user.id, NOW() - INTERVAL '10 days'
FROM homes h
JOIN users owner_user ON h.user_id = owner_user.id
JOIN users member ON member.email = 'sarah.wilona@smartify.com'
WHERE owner_user.email = 'demo@smartify.com'
  AND h.name = 'Demo Home'
ON CONFLICT DO NOTHING;

-- Add rooms to the demo home
INSERT INTO rooms (user_id, name, home_id)
SELECT u.id, r.name, h.id
FROM users u
CROSS JOIN (VALUES 
    ('Living Room'),
    ('Bedroom'),
    ('Kitchen'),
    ('Bathroom'),
    ('Office'),
    ('Dining Room')
) AS r(name)
JOIN homes h ON h.user_id = u.id AND h.name = 'Demo Home'
WHERE u.email = 'demo@smartify.com'
ON CONFLICT (user_id, name) DO UPDATE SET home_id = EXCLUDED.home_id;

-- Verify members were added
-- SELECT hm.id, hm.role, u.first_name, u.last_name, u.email
-- FROM home_members hm
-- JOIN users u ON hm.user_id = u.id
-- JOIN homes h ON hm.home_id = h.id
-- JOIN users owner ON h.user_id = owner.id
-- WHERE owner.email = 'demo@smartify.com';
