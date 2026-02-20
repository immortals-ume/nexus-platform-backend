-- Create user_preferences table for managing notification preferences
CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    enabled_channels VARCHAR(500),
    opted_out_categories VARCHAR(500),
    marketing_enabled BOOLEAN DEFAULT true,
    transactional_enabled BOOLEAN DEFAULT true,
    timezone VARCHAR(50) DEFAULT 'UTC',
    quiet_hours_start VARCHAR(5),
    quiet_hours_end VARCHAR(5),
    quiet_hours_enabled BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT chk_timezone CHECK (timezone IS NOT NULL)
);

-- Create indexes
CREATE INDEX idx_user_id ON user_preferences(user_id);
CREATE INDEX idx_marketing_enabled ON user_preferences(marketing_enabled);
CREATE INDEX idx_transactional_enabled ON user_preferences(transactional_enabled);

-- Add comments
COMMENT ON TABLE user_preferences IS 'User notification preferences and opt-in/opt-out settings';
COMMENT ON COLUMN user_preferences.enabled_channels IS 'Comma-separated list of enabled channels: EMAIL,SMS,WHATSAPP';
COMMENT ON COLUMN user_preferences.opted_out_categories IS 'Comma-separated list of opted-out categories';
COMMENT ON COLUMN user_preferences.quiet_hours_start IS 'Quiet hours start time in HH:mm format';
COMMENT ON COLUMN user_preferences.quiet_hours_end IS 'Quiet hours end time in HH:mm format';
