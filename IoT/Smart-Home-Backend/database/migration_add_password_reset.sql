-- Migration: Add Password Reset OTP Support
-- This migration adds support for password reset via OTP codes

-- Password reset OTP table
CREATE TABLE IF NOT EXISTS password_reset_otp (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_otp_user_id ON password_reset_otp(user_id);
CREATE INDEX IF NOT EXISTS idx_otp_email ON password_reset_otp(email);
CREATE INDEX IF NOT EXISTS idx_otp_code ON password_reset_otp(otp_code);
CREATE INDEX IF NOT EXISTS idx_otp_expires_at ON password_reset_otp(expires_at);

-- Clean up expired OTPs (optional: can be done via scheduled job)
-- DELETE FROM password_reset_otp WHERE expires_at < CURRENT_TIMESTAMP;

COMMENT ON TABLE password_reset_otp IS 'Stores OTP codes for password reset';
COMMENT ON COLUMN password_reset_otp.otp_code IS '6-digit OTP code';
COMMENT ON COLUMN password_reset_otp.expires_at IS 'OTP expiration timestamp (typically 10-15 minutes)';
COMMENT ON COLUMN password_reset_otp.used IS 'Whether the OTP has been used';

