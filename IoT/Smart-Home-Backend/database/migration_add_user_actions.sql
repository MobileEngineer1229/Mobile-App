-- Migration: Add User Actions Logging
-- This migration adds support for logging all user actions for audit and analytics

-- User actions table
CREATE TABLE IF NOT EXISTS user_actions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    action_category VARCHAR(50) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    request_body JSONB NULL,
    response_status INTEGER NULL,
    response_body JSONB NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    device_info JSONB NULL,
    session_id VARCHAR(255) NULL,
    duration_ms INTEGER NULL,
    error_message TEXT NULL,
    metadata JSONB NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_user_actions_user_id ON user_actions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_actions_created_at ON user_actions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_actions_action_type ON user_actions(action_type);
CREATE INDEX IF NOT EXISTS idx_user_actions_action_category ON user_actions(action_category);
CREATE INDEX IF NOT EXISTS idx_user_actions_endpoint ON user_actions(endpoint);
CREATE INDEX IF NOT EXISTS idx_user_actions_user_created ON user_actions(user_id, created_at DESC);

COMMENT ON TABLE user_actions IS 'Stores all user actions for audit trail and analytics';
COMMENT ON COLUMN user_actions.action_type IS 'Type of action: login, logout, device_control, settings_update, etc.';
COMMENT ON COLUMN user_actions.action_category IS 'Category: authentication, device_management, settings, reports, etc.';
COMMENT ON COLUMN user_actions.endpoint IS 'API endpoint that was called';
COMMENT ON COLUMN user_actions.method IS 'HTTP method: GET, POST, PUT, DELETE';
COMMENT ON COLUMN user_actions.request_body IS 'Request body data (sanitized, no passwords)';
COMMENT ON COLUMN user_actions.response_status IS 'HTTP response status code';
COMMENT ON COLUMN user_actions.response_body IS 'Response body (may be truncated for large responses)';
COMMENT ON COLUMN user_actions.ip_address IS 'IP address of the request';
COMMENT ON COLUMN user_actions.user_agent IS 'User agent string';
COMMENT ON COLUMN user_actions.device_info IS 'Device information extracted from user agent';
COMMENT ON COLUMN user_actions.session_id IS 'Session identifier if available';
COMMENT ON COLUMN user_actions.duration_ms IS 'Request duration in milliseconds';
COMMENT ON COLUMN user_actions.error_message IS 'Error message if action failed';
COMMENT ON COLUMN user_actions.metadata IS 'Additional metadata in JSON format';
