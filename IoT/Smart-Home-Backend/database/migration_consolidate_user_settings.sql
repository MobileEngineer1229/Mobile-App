-- Migration to consolidate all user settings into the users table
-- This migration:
-- 1. Adds JSONB columns to users table if they don't exist
-- 2. Migrates data from old separate tables (if they exist) to JSONB columns
-- 3. Drops old separate tables after migration

-- Step 1: Add JSONB columns to users table
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS notification_preferences JSONB DEFAULT '{}'::jsonb;

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS security_settings JSONB DEFAULT '{}'::jsonb;

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS profile_metadata JSONB DEFAULT '{}'::jsonb;

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS additional_settings JSONB DEFAULT '{}'::jsonb;

-- Step 2: Create indexes on JSONB columns for better query performance
CREATE INDEX IF NOT EXISTS idx_users_notification_preferences ON users USING GIN (notification_preferences);
CREATE INDEX IF NOT EXISTS idx_users_security_settings ON users USING GIN (security_settings);
CREATE INDEX IF NOT EXISTS idx_users_profile_metadata ON users USING GIN (profile_metadata);
CREATE INDEX IF NOT EXISTS idx_users_additional_settings ON users USING GIN (additional_settings);

-- Step 3: Migrate data from old tables if they exist (only if JSONB is empty)
-- Migrate notification preferences
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_notification_preferences') THEN
        UPDATE users u
        SET notification_preferences = COALESCE(
            (SELECT jsonb_object_agg(notification_type, enabled)
             FROM user_notification_preferences unp
             WHERE unp.user_id = u.id),
            '{}'::jsonb
        )
        WHERE notification_preferences = '{}'::jsonb OR notification_preferences IS NULL;
    END IF;
END $$;

-- Migrate security settings
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_security_settings') THEN
        UPDATE users u
        SET security_settings = COALESCE(
            (SELECT jsonb_object_agg(
                setting_type,
                CASE 
                    WHEN metadata IS NOT NULL THEN jsonb_build_object('enabled', enabled) || metadata
                    ELSE enabled
                END
             )
             FROM user_security_settings uss
             WHERE uss.user_id = u.id),
            '{}'::jsonb
        )
        WHERE security_settings = '{}'::jsonb OR security_settings IS NULL;
    END IF;
END $$;

-- Migrate profile metadata
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_profile_metadata') THEN
        UPDATE users u
        SET profile_metadata = COALESCE(
            (SELECT jsonb_build_object(
                'gender', upm.gender,
                'birthdate', upm.birthdate::text,
                'profilePictureUrl', upm.profile_picture_url
            ) || COALESCE(upm.metadata, '{}'::jsonb)
             FROM user_profile_metadata upm
             WHERE upm.user_id = u.id),
            '{}'::jsonb
        )
        WHERE profile_metadata = '{}'::jsonb OR profile_metadata IS NULL;
    END IF;
END $$;

-- Migrate additional settings
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_additional_settings') THEN
        UPDATE users u
        SET additional_settings = COALESCE(
            (SELECT jsonb_object_agg(
                setting_key,
                CASE 
                    WHEN metadata IS NOT NULL THEN jsonb_build_object('value', setting_value) || metadata
                    ELSE setting_value
                END
             )
             FROM user_additional_settings uas
             WHERE uas.user_id = u.id),
            '{}'::jsonb
        )
        WHERE additional_settings = '{}'::jsonb OR additional_settings IS NULL;
    END IF;
END $$;

-- Step 4: Set default values for users who don't have settings yet
UPDATE users 
SET notification_preferences = jsonb_build_object(
    'device_status_alerts', true,
    'energy_consumption_alerts', true,
    'bill_reminders', true,
    'automation_updates', false,
    'device_maintenance_reminders', false,
    'security_alerts', true,
    'weather_based_suggestions', true,
    'community_updates', false,
    'home_invitations', true,
    'user_access_alerts', false,
    'customer_support_updates', false,
    'feedback_updates', false
)
WHERE notification_preferences = '{}'::jsonb OR notification_preferences IS NULL;

UPDATE users 
SET security_settings = jsonb_build_object(
    'biometric_id', false,
    'face_id', false,
    'sms_authenticator', false,
    'google_authenticator', false
)
WHERE security_settings = '{}'::jsonb OR security_settings IS NULL;

UPDATE users 
SET profile_metadata = COALESCE(
    profile_metadata,
    jsonb_build_object(
        'appAppearance', jsonb_build_object(
            'theme', 'system',
            'language', 'en_US'
        )
    )
)
WHERE profile_metadata = '{}'::jsonb OR profile_metadata IS NULL;

-- Step 5: Drop old tables if they exist (after data migration)
DROP TABLE IF EXISTS user_notification_preferences CASCADE;
DROP TABLE IF EXISTS user_security_settings CASCADE;
DROP TABLE IF EXISTS user_profile_metadata CASCADE;
DROP TABLE IF EXISTS user_additional_settings CASCADE;
