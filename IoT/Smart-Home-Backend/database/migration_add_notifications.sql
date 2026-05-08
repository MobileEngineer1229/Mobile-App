-- Migration: Add Notifications Support
-- This migration adds support for user notifications

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'general',
    icon VARCHAR(50) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    metadata JSONB NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_notification_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_user_read ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_type ON notifications(type);

COMMENT ON TABLE notifications IS 'Stores user notifications';
COMMENT ON COLUMN notifications.type IS 'Notification type: general, security, system, feature, reminder, etc.';
COMMENT ON COLUMN notifications.icon IS 'Icon identifier for UI display';
COMMENT ON COLUMN notifications.metadata IS 'Additional data in JSON format';

