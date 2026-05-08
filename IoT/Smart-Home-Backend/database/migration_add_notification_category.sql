-- Migration: Add category field to notifications table
-- This migration adds a category field to support General/Smart Home notification types

-- Add category column to notifications table
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS category VARCHAR(20) NULL;

-- Set default category based on existing type
UPDATE notifications 
SET category = CASE 
    WHEN type IN ('security', 'alert') THEN 'smart_home'
    ELSE 'general'
END
WHERE category IS NULL;

-- Create index for category
CREATE INDEX IF NOT EXISTS idx_notification_category ON notifications(user_id, category);

-- Add comment
COMMENT ON COLUMN notifications.category IS 'Notification category: general or smart_home';

-- Make category NOT NULL for new records (optional, can be enforced at application level)
-- ALTER TABLE notifications ALTER COLUMN category SET NOT NULL;
