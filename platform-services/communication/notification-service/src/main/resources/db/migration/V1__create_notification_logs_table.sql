-- Create notification_logs table for tracking notification processing
CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    notification_type VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    message TEXT,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    correlation_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

-- Create indexes for better query performance
CREATE INDEX idx_event_id ON notification_logs(event_id);
CREATE INDEX idx_correlation_id ON notification_logs(correlation_id);
CREATE INDEX idx_status ON notification_logs(status);
CREATE INDEX idx_created_at ON notification_logs(created_at);
CREATE INDEX idx_recipient ON notification_logs(recipient);

-- Add comments for documentation
COMMENT ON TABLE notification_logs IS 'Tracks all notification processing for idempotency and auditing';
COMMENT ON COLUMN notification_logs.event_id IS 'Unique event identifier for idempotency';
COMMENT ON COLUMN notification_logs.notification_type IS 'Type of notification: EMAIL, SMS, WHATSAPP';
COMMENT ON COLUMN notification_logs.status IS 'Processing status: PENDING, SENT, FAILED';
COMMENT ON COLUMN notification_logs.correlation_id IS 'Correlation ID for distributed tracing';
