-- Migration: Add Chatbot Messages Support
-- This migration adds support for chatbot conversation history

-- Chatbot messages table
CREATE TABLE IF NOT EXISTS chatbot_messages (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'user' or 'assistant'
    message TEXT NOT NULL,
    metadata JSONB NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_chatbot_user_id ON chatbot_messages(user_id);
CREATE INDEX IF NOT EXISTS idx_chatbot_user_created ON chatbot_messages(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chatbot_role ON chatbot_messages(role);

COMMENT ON TABLE chatbot_messages IS 'Stores chatbot conversation history';
COMMENT ON COLUMN chatbot_messages.role IS 'Message role: user or assistant';
COMMENT ON COLUMN chatbot_messages.metadata IS 'Additional data (e.g., suggested actions, links)';

