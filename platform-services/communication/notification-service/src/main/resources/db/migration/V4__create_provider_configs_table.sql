-- Create provider_configs table for managing notification provider configurations
-- Supports multi-provider routing with country-specific rules and rate limiting
-- Uses UUID for ID to align with BaseEntity from domain-starter

-- Ensure UUID extension is available
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE provider_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider_id VARCHAR(50) UNIQUE NOT NULL,
    provider_name VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    supported_countries TEXT[],
    priority INTEGER NOT NULL DEFAULT 100,
    enabled BOOLEAN DEFAULT true,
    credentials JSONB NOT NULL,
    configuration JSONB,
    rate_limit_config JSONB,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    CONSTRAINT chk_channel CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP', 'PUSH_NOTIFICATION')),
    CONSTRAINT chk_priority CHECK (priority > 0)
);

-- Create indexes for efficient querying
CREATE INDEX idx_provider_id ON provider_configs(provider_id);
CREATE INDEX idx_channel ON provider_configs(channel);
CREATE INDEX idx_enabled ON provider_configs(enabled);
CREATE INDEX idx_priority ON provider_configs(priority);
CREATE INDEX idx_channel_enabled ON provider_configs(channel, enabled) WHERE enabled = true;

-- Create GIN index for array search on supported_countries
CREATE INDEX idx_supported_countries ON provider_configs USING GIN(supported_countries);

-- Add comments for documentation
COMMENT ON TABLE provider_configs IS 'Configuration for notification providers with routing rules and rate limits';
COMMENT ON COLUMN provider_configs.provider_id IS 'Unique provider identifier (e.g., TWILIO, GUPSHUP, AWS_SNS, MAILCHIMP)';
COMMENT ON COLUMN provider_configs.provider_name IS 'Human-readable provider name';
COMMENT ON COLUMN provider_configs.channel IS 'Notification channel supported by this provider';
COMMENT ON COLUMN provider_configs.supported_countries IS 'Array of country codes supported (use * for global)';
COMMENT ON COLUMN provider_configs.priority IS 'Provider priority (lower number = higher priority)';
COMMENT ON COLUMN provider_configs.enabled IS 'Whether provider is currently enabled';
COMMENT ON COLUMN provider_configs.credentials IS 'Provider credentials (API keys, tokens) as JSON';
COMMENT ON COLUMN provider_configs.configuration IS 'Provider-specific configuration as JSON';
COMMENT ON COLUMN provider_configs.rate_limit_config IS 'Rate limiting configuration as JSON';

-- Insert default provider configurations (with placeholder credentials)
INSERT INTO provider_configs (provider_id, provider_name, channel, supported_countries, priority, enabled, credentials, configuration, rate_limit_config)
VALUES 
    ('TWILIO_SMS', 'Twilio SMS', 'SMS', ARRAY['US', 'CA', 'GB', 'AU'], 10, false, 
     '{"accountSid": "REPLACE_ME", "authToken": "REPLACE_ME"}'::jsonb,
     '{"fromNumber": "+1234567890"}'::jsonb,
     '{"requestsPerSecond": 10}'::jsonb),
    
    ('TWILIO_WHATSAPP', 'Twilio WhatsApp', 'WHATSAPP', ARRAY['US', 'CA', 'GB', 'AU'], 10, false,
     '{"accountSid": "REPLACE_ME", "authToken": "REPLACE_ME"}'::jsonb,
     '{"whatsappFrom": "whatsapp:+1234567890"}'::jsonb,
     '{"requestsPerSecond": 10}'::jsonb),
    
    ('GUPSHUP_SMS', 'Gupshup SMS', 'SMS', ARRAY['IN', 'PK', 'BD', 'LK'], 20, false,
     '{"apiKey": "REPLACE_ME"}'::jsonb,
     '{"appName": "MyApp"}'::jsonb,
     '{"requestsPerSecond": 5}'::jsonb),
    
    ('GUPSHUP_WHATSAPP', 'Gupshup WhatsApp', 'WHATSAPP', ARRAY['IN', 'PK', 'BD', 'LK'], 20, false,
     '{"apiKey": "REPLACE_ME"}'::jsonb,
     '{"appName": "MyApp"}'::jsonb,
     '{"requestsPerSecond": 5}'::jsonb),
    
    ('AWS_SNS_SMS', 'AWS SNS SMS', 'SMS', ARRAY['*'], 30, false,
     '{"accessKeyId": "REPLACE_ME", "secretAccessKey": "REPLACE_ME", "region": "us-east-1"}'::jsonb,
     '{}'::jsonb,
     '{"requestsPerSecond": 20}'::jsonb),
    
    ('AWS_SNS_PUSH', 'AWS SNS Push', 'PUSH_NOTIFICATION', ARRAY['*'], 30, false,
     '{"accessKeyId": "REPLACE_ME", "secretAccessKey": "REPLACE_ME", "region": "us-east-1"}'::jsonb,
     '{}'::jsonb,
     '{"requestsPerSecond": 20}'::jsonb),
    
    ('MAILCHIMP', 'Mailchimp Transactional', 'EMAIL', ARRAY['*'], 10, false,
     '{"apiKey": "REPLACE_ME"}'::jsonb,
     '{"fromEmail": "noreply@example.com", "fromName": "MyApp"}'::jsonb,
     '{"requestsPerSecond": 15}'::jsonb);

COMMENT ON TABLE provider_configs IS 'Default provider configurations are inserted with enabled=false. Update credentials and enable before use.';
