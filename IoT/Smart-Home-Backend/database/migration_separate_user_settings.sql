-- Migration: Separate user settings into individual tables
-- This migration creates separate tables for notification_preferences, security_settings, 
-- profile_metadata, and additional_settings, replacing the JSONB columns

-- 1. Create user_notification_preferences table
CREATE TABLE IF NOT EXISTS user_notification_preferences (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    preference_value JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (user_id, preference_key)
);

CREATE INDEX IF NOT EXISTS idx_notification_pref_user_id ON user_notification_preferences(user_id);

-- 2. Create user_security_settings table
CREATE TABLE IF NOT EXISTS user_security_settings (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (user_id, setting_key)
);

CREATE INDEX IF NOT EXISTS idx_security_setting_user_id ON user_security_settings(user_id);

-- 3. Create user_profile_metadata table
CREATE TABLE IF NOT EXISTS user_profile_metadata (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (user_id, metadata_key)
);

CREATE INDEX IF NOT EXISTS idx_profile_metadata_user_id ON user_profile_metadata(user_id);

-- 4. Create user_additional_settings table
CREATE TABLE IF NOT EXISTS user_additional_settings (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (user_id, setting_key)
);

CREATE INDEX IF NOT EXISTS idx_additional_setting_user_id ON user_additional_settings(user_id);

-- 5. Migrate data from JSONB columns to separate tables
DO $$
DECLARE
    user_record RECORD;
    pref_key TEXT;
    pref_value JSONB;
    setting_key TEXT;
    setting_value JSONB;
    meta_key TEXT;
    meta_value JSONB;
    add_key TEXT;
    add_value JSONB;
BEGIN
    -- Migrate notification_preferences
    FOR user_record IN SELECT id, notification_preferences FROM users WHERE notification_preferences IS NOT NULL AND notification_preferences != '{}'::jsonb
    LOOP
        FOR pref_key, pref_value IN SELECT * FROM jsonb_each(user_record.notification_preferences)
        LOOP
            INSERT INTO user_notification_preferences (user_id, preference_key, preference_value)
            VALUES (user_record.id, pref_key, pref_value)
            ON CONFLICT (user_id, preference_key) DO UPDATE
            SET preference_value = EXCLUDED.preference_value, updated_at = CURRENT_TIMESTAMP;
        END LOOP;
    END LOOP;

    -- Migrate security_settings
    FOR user_record IN SELECT id, security_settings FROM users WHERE security_settings IS NOT NULL AND security_settings != '{}'::jsonb
    LOOP
        FOR setting_key, setting_value IN SELECT * FROM jsonb_each(user_record.security_settings)
        LOOP
            INSERT INTO user_security_settings (user_id, setting_key, setting_value)
            VALUES (user_record.id, setting_key, setting_value)
            ON CONFLICT (user_id, setting_key) DO UPDATE
            SET setting_value = EXCLUDED.setting_value, updated_at = CURRENT_TIMESTAMP;
        END LOOP;
    END LOOP;

    -- Migrate profile_metadata
    FOR user_record IN SELECT id, profile_metadata FROM users WHERE profile_metadata IS NOT NULL AND profile_metadata != '{}'::jsonb
    LOOP
        FOR meta_key, meta_value IN SELECT * FROM jsonb_each(user_record.profile_metadata)
        LOOP
            INSERT INTO user_profile_metadata (user_id, metadata_key, metadata_value)
            VALUES (user_record.id, meta_key, meta_value)
            ON CONFLICT (user_id, metadata_key) DO UPDATE
            SET metadata_value = EXCLUDED.metadata_value, updated_at = CURRENT_TIMESTAMP;
        END LOOP;
    END LOOP;

    -- Migrate additional_settings
    FOR user_record IN SELECT id, additional_settings FROM users WHERE additional_settings IS NOT NULL AND additional_settings != '{}'::jsonb
    LOOP
        FOR add_key, add_value IN SELECT * FROM jsonb_each(user_record.additional_settings)
        LOOP
            INSERT INTO user_additional_settings (user_id, setting_key, setting_value)
            VALUES (user_record.id, add_key, add_value)
            ON CONFLICT (user_id, setting_key) DO UPDATE
            SET setting_value = EXCLUDED.setting_value, updated_at = CURRENT_TIMESTAMP;
        END LOOP;
    END LOOP;
END $$;

-- 6. Create triggers for updated_at
DROP TRIGGER IF EXISTS update_notification_pref_updated_at ON user_notification_preferences;
CREATE TRIGGER update_notification_pref_updated_at BEFORE UPDATE ON user_notification_preferences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_security_setting_updated_at ON user_security_settings;
CREATE TRIGGER update_security_setting_updated_at BEFORE UPDATE ON user_security_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_profile_metadata_updated_at ON user_profile_metadata;
CREATE TRIGGER update_profile_metadata_updated_at BEFORE UPDATE ON user_profile_metadata
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_additional_setting_updated_at ON user_additional_settings;
CREATE TRIGGER update_additional_setting_updated_at BEFORE UPDATE ON user_additional_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Note: We keep the JSONB columns in users table for backward compatibility
-- They can be removed in a future migration if needed
