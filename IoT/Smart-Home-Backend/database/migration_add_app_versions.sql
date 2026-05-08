-- Migration: Add app_versions table
-- This migration creates the app_versions table for managing mobile app version information

-- Create app_versions table
CREATE TABLE IF NOT EXISTS app_versions (
    id SERIAL PRIMARY KEY,
    platform VARCHAR(20) NOT NULL DEFAULT 'android', -- 'android' or 'ios'
    version_name VARCHAR(50) NOT NULL, -- e.g., "1.0.0"
    version_code INTEGER NOT NULL, -- e.g., 1
    minimum_required_version VARCHAR(50) NOT NULL, -- Minimum version required to use the app
    update_url TEXT, -- URL to download the update (Play Store, App Store, etc.)
    force_update BOOLEAN DEFAULT false, -- If true, user must update to continue
    release_notes TEXT, -- Release notes for this version
    is_active BOOLEAN DEFAULT true, -- Whether this version is currently active
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (platform, version_name, version_code)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_app_versions_platform ON app_versions(platform);
CREATE INDEX IF NOT EXISTS idx_app_versions_active ON app_versions(platform, is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_app_versions_version_code ON app_versions(platform, version_code DESC);

-- Add comments
COMMENT ON TABLE app_versions IS 'Stores mobile app version information for version checking and update management';
COMMENT ON COLUMN app_versions.platform IS 'Platform: android or ios';
COMMENT ON COLUMN app_versions.version_name IS 'Version name (e.g., "1.0.0")';
COMMENT ON COLUMN app_versions.version_code IS 'Version code (integer, e.g., 1)';
COMMENT ON COLUMN app_versions.minimum_required_version IS 'Minimum version required to use the app (version_name format)';
COMMENT ON COLUMN app_versions.update_url IS 'URL to download the update (Play Store, App Store, etc.)';
COMMENT ON COLUMN app_versions.force_update IS 'If true, user must update to continue using the app';
COMMENT ON COLUMN app_versions.release_notes IS 'Release notes for this version';
COMMENT ON COLUMN app_versions.is_active IS 'Whether this version configuration is currently active';

-- Insert default version (for Android)
INSERT INTO app_versions (platform, version_name, version_code, minimum_required_version, update_url, force_update, release_notes, is_active)
VALUES (
    'android',
    '1.0.0',
    1,
    '1.0.0',
    'https://play.google.com/store/apps/details?id=com.smarthome.iot',
    false,
    'Initial release',
    true
) ON CONFLICT (platform, version_name, version_code) DO NOTHING;
