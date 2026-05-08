-- Migration: Add home_members table
-- This migration creates the home_members table for managing home memberships and roles

-- Create home_members table
CREATE TABLE IF NOT EXISTS home_members (
    id SERIAL PRIMARY KEY,
    home_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'member', -- 'owner', 'admin', 'member'
    added_by INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (home_id) REFERENCES homes(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (added_by) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (home_id, user_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_home_members_home_id ON home_members(home_id);
CREATE INDEX IF NOT EXISTS idx_home_members_user_id ON home_members(user_id);
CREATE INDEX IF NOT EXISTS idx_home_members_role ON home_members(home_id, role);

-- Create trigger to update updated_at (if needed in future)
-- Note: No updated_at column for now, but structure allows for it

-- Add comments
COMMENT ON TABLE home_members IS 'Stores home memberships with roles (owner, admin, member)';
COMMENT ON COLUMN home_members.role IS 'Member role: owner (full control), admin (manage members/devices), member (view and control devices)';
COMMENT ON COLUMN home_members.added_by IS 'User ID who added this member to the home';
