-- Create voice_assistants table
CREATE TABLE IF NOT EXISTS voice_assistants (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create user_linked_assistants table
CREATE TABLE IF NOT EXISTS user_linked_assistants (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assistant_id INTEGER NOT NULL REFERENCES voice_assistants(id) ON DELETE CASCADE,
    access_token TEXT,
    refresh_token TEXT,
    metadata JSONB,
    linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, assistant_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_user_linked_assistants_user_id ON user_linked_assistants(user_id);
CREATE INDEX IF NOT EXISTS idx_user_linked_assistants_assistant_id ON user_linked_assistants(assistant_id);

-- Insert default voice assistants
INSERT INTO voice_assistants (name) VALUES
    ('Google Assistant'),
    ('Amazon Alexa'),
    ('Microsoft Cortana'),
    ('Samsung Bixby'),
    ('Naver Clova'),
    ('Apple Siri')
ON CONFLICT (name) DO NOTHING;

-- Update trigger for updated_at
CREATE OR REPLACE FUNCTION update_voice_assistants_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_voice_assistants_updated_at
    BEFORE UPDATE ON voice_assistants
    FOR EACH ROW
    EXECUTE FUNCTION update_voice_assistants_updated_at();

