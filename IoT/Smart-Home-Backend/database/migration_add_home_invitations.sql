-- Migration: Add home_invitations table
-- This migration creates the home_invitations table for managing home invitation codes

-- Create home_invitations table
CREATE TABLE IF NOT EXISTS home_invitations (
    id SERIAL PRIMARY KEY,
    home_id INTEGER NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    created_by INTEGER NOT NULL,
    expires_at TIMESTAMP NULL,
    max_uses INTEGER DEFAULT 1,
    current_uses INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (home_id) REFERENCES homes(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_home_invitations_home_id ON home_invitations(home_id);
CREATE INDEX IF NOT EXISTS idx_home_invitations_code ON home_invitations(code);
CREATE INDEX IF NOT EXISTS idx_home_invitations_active ON home_invitations(is_active, expires_at) WHERE is_active = true;

-- Add comments
COMMENT ON TABLE home_invitations IS 'Stores invitation codes for joining homes';
COMMENT ON COLUMN home_invitations.code IS 'Unique invitation code (e.g., ABC123)';
COMMENT ON COLUMN home_invitations.max_uses IS 'Maximum number of times this invitation can be used (0 = unlimited)';
COMMENT ON COLUMN home_invitations.current_uses IS 'Number of times this invitation has been used';
COMMENT ON COLUMN home_invitations.expires_at IS 'Expiration timestamp (NULL = never expires)';
