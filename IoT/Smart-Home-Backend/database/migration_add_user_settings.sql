-- Add settings columns to users table using JSONB for flexible storage
-- This consolidates all user settings into the main users table

-- Add notification preferences as JSONB
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS notification_preferences JSONB DEFAULT '{}'::jsonb;

-- Add security settings as JSONB
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS security_settings JSONB DEFAULT '{}'::jsonb;

-- Add profile metadata (gender, birthdate, profile_picture_url, etc.)
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS profile_metadata JSONB DEFAULT '{}'::jsonb;

-- Add additional settings as JSONB
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS additional_settings JSONB DEFAULT '{}'::jsonb;

-- Create indexes on JSONB columns for better query performance
CREATE INDEX IF NOT EXISTS idx_users_notification_preferences ON users USING GIN (notification_preferences);
CREATE INDEX IF NOT EXISTS idx_users_security_settings ON users USING GIN (security_settings);
CREATE INDEX IF NOT EXISTS idx_users_profile_metadata ON users USING GIN (profile_metadata);
CREATE INDEX IF NOT EXISTS idx_users_additional_settings ON users USING GIN (additional_settings);

-- Set default notification preferences for existing users
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

-- Set default security settings for existing users
UPDATE users 
SET security_settings = jsonb_build_object(
    'biometric_id', false,
    'face_id', false,
    'sms_authenticator', false,
    'google_authenticator', false
)
WHERE security_settings = '{}'::jsonb OR security_settings IS NULL;

-- Set default app appearance in profile_metadata for existing users
UPDATE users 
SET profile_metadata = jsonb_build_object(
    'appAppearance', jsonb_build_object(
        'theme', 'system',
        'language', 'en_US'
    )
)
WHERE profile_metadata = '{}'::jsonb OR profile_metadata IS NULL;
