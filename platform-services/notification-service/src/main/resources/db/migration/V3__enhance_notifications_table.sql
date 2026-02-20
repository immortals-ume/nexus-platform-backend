-- Enhance notification_logs table with new fields for multi-channel notification system
-- This migration adds support for provider routing, delivery tracking, and localization
-- Also migrates to UUID-based IDs to align with BaseEntity from domain-starter

-- Add UUID extension if not exists
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Add new columns to notification_logs table
ALTER TABLE notification_logs
    ADD COLUMN country_code VARCHAR(10),
    ADD COLUMN locale VARCHAR(10) DEFAULT 'en_US',
    ADD COLUMN delivery_status VARCHAR(20),
    ADD COLUMN provider_message_id VARCHAR(255),
    ADD COLUMN delivered_at TIMESTAMP,
    ADD COLUMN read_at TIMESTAMP,
    ADD COLUMN metadata JSONB,
    ADD COLUMN template_code VARCHAR(100),
    ADD COLUMN template_variables JSONB,
    ADD COLUMN html_content TEXT,
    ADD COLUMN scheduled_at TIMESTAMP,
    ADD COLUMN max_retries INTEGER DEFAULT 3,
    ADD COLUMN provider_id VARCHAR(50),
    ADD COLUMN version BIGINT DEFAULT 0 NOT NULL,
    ADD COLUMN updated_at TIMESTAMP DEFAULT NOW() NOT NULL,
    ADD COLUMN created_by VARCHAR(100),
    ADD COLUMN updated_by VARCHAR(100),
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by VARCHAR(100);

-- Update the status constraint to include new statuses
ALTER TABLE notification_logs DROP CONSTRAINT IF EXISTS chk_status;
ALTER TABLE notification_logs ADD CONSTRAINT chk_status 
    CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'SCHEDULED', 'CANCELLED'));

-- Add constraint for delivery_status
ALTER TABLE notification_logs ADD CONSTRAINT chk_delivery_status 
    CHECK (delivery_status IS NULL OR delivery_status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'READ'));

-- Update notification_type constraint to include PUSH_NOTIFICATION
ALTER TABLE notification_logs DROP CONSTRAINT IF EXISTS notification_logs_notification_type_check;
ALTER TABLE notification_logs ADD CONSTRAINT chk_notification_type 
    CHECK (notification_type IN ('EMAIL', 'SMS', 'WHATSAPP', 'PUSH_NOTIFICATION'));

-- Create additional indexes for new fields
CREATE INDEX idx_country_code ON notification_logs(country_code);
CREATE INDEX idx_locale ON notification_logs(locale);
CREATE INDEX idx_delivery_status ON notification_logs(delivery_status);
CREATE INDEX idx_provider_id ON notification_logs(provider_id);
CREATE INDEX idx_provider_message_id ON notification_logs(provider_message_id);
CREATE INDEX idx_scheduled_at ON notification_logs(scheduled_at) WHERE scheduled_at IS NOT NULL;
CREATE INDEX idx_template_code ON notification_logs(template_code) WHERE template_code IS NOT NULL;

-- Add comments for new columns
COMMENT ON COLUMN notification_logs.country_code IS 'Country code extracted from recipient for provider routing';
COMMENT ON COLUMN notification_logs.locale IS 'Locale for template localization (e.g., en_US, es_ES)';
COMMENT ON COLUMN notification_logs.delivery_status IS 'Delivery status from provider: PENDING, SENT, DELIVERED, FAILED, READ';
COMMENT ON COLUMN notification_logs.provider_message_id IS 'Provider tracking ID for status updates';
COMMENT ON COLUMN notification_logs.delivered_at IS 'Timestamp when notification was delivered to recipient';
COMMENT ON COLUMN notification_logs.read_at IS 'Timestamp when notification was read by recipient';
COMMENT ON COLUMN notification_logs.metadata IS 'Additional context and metadata as JSON';
COMMENT ON COLUMN notification_logs.template_code IS 'Template code used for rendering';
COMMENT ON COLUMN notification_logs.template_variables IS 'Variables used for template rendering as JSON';
COMMENT ON COLUMN notification_logs.scheduled_at IS 'Scheduled delivery time for future notifications';
COMMENT ON COLUMN notification_logs.provider_id IS 'Provider used for delivery (TWILIO, GUPSHUP, AWS_SNS, MAILCHIMP)';
