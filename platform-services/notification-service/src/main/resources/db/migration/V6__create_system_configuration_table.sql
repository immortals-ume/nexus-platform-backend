-- Create system_configuration table for centralized configuration management
CREATE TABLE IF NOT EXISTS system_configuration (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(255) UNIQUE NOT NULL,
    config_value TEXT NOT NULL,
    config_type VARCHAR(50) NOT NULL DEFAULT 'STRING',
    description TEXT,
    category VARCHAR(100),
    is_encrypted BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER DEFAULT 0
);

CREATE INDEX idx_system_config_key ON system_configuration(config_key);
CREATE INDEX idx_system_config_category ON system_configuration(category);
CREATE INDEX idx_system_config_active ON system_configuration(is_active);

COMMENT ON TABLE system_configuration IS 'Centralized configuration table for dynamic application settings';
COMMENT ON COLUMN system_configuration.config_key IS 'Unique configuration key (e.g., notification.retry.max-attempts)';
COMMENT ON COLUMN system_configuration.config_value IS 'Configuration value (can be string, number, json, etc.)';
COMMENT ON COLUMN system_configuration.config_type IS 'Data type: STRING, INTEGER, BOOLEAN, JSON, DECIMAL';
COMMENT ON COLUMN system_configuration.is_encrypted IS 'Whether the value is encrypted (for sensitive data)';
COMMENT ON COLUMN system_configuration.is_active IS 'Whether this configuration is currently active';

-- Insert default configurations
INSERT INTO system_configuration (config_key, config_value, config_type, description, category, is_active) VALUES
-- Event Configuration
('notification.events.request-type', 'NotificationRequest', 'STRING', 'Event type for notification requests', 'EVENTS', true),
('notification.events.sent-type', 'NotificationSent', 'STRING', 'Event type for sent notifications', 'EVENTS', true),
('notification.events.sent-topic', 'notification-sent', 'STRING', 'Kafka topic for sent notifications', 'EVENTS', true),
('notification.events.metrics-listener', 'NotificationEventListener', 'STRING', 'Metrics listener name', 'EVENTS', true),

-- Provider Configuration
('notification.provider.sms-id', 'SMS_TWILIO', 'STRING', 'Default SMS provider ID', 'PROVIDERS', true),
('notification.provider.failover-strategy', 'FAILOVER', 'STRING', 'Provider failover strategy', 'PROVIDERS', true),

-- Retry Configuration
('notification.retry.max-attempts', '3', 'INTEGER', 'Maximum retry attempts', 'RETRY', true),
('notification.retry.initial-interval-ms', '1000', 'INTEGER', 'Initial retry interval in milliseconds', 'RETRY', true),
('notification.retry.multiplier', '2.0', 'DECIMAL', 'Retry backoff multiplier', 'RETRY', true),
('notification.retry.max-interval-ms', '10000', 'INTEGER', 'Maximum retry interval in milliseconds', 'RETRY', true),

-- Health Check Configuration
('notification.health.database-timeout-sec', '2', 'INTEGER', 'Database health check timeout in seconds', 'HEALTH', true),
('notification.health.redis-command', 'PONG', 'STRING', 'Expected Redis health check response', 'HEALTH', true),
('notification.health.healthy-status', 'UP', 'STRING', 'Healthy status indicator', 'HEALTH', true),
('notification.health.unhealthy-status', 'DOWN', 'STRING', 'Unhealthy status indicator', 'HEALTH', true),
('notification.health.healthy-message', 'Provider is healthy', 'STRING', 'Healthy status message', 'HEALTH', true),
('notification.health.unhealthy-message', 'Provider is unhealthy', 'STRING', 'Unhealthy status message', 'HEALTH', true),

-- Cache Configuration
('notification.cache.provider-health-prefix', 'provider:health', 'STRING', 'Cache key prefix for provider health', 'CACHE', true),
('notification.cache.deduplication-prefix', 'notification:dedup', 'STRING', 'Cache key prefix for deduplication', 'CACHE', true),
('notification.cache.rate-limit-prefix', 'notification:ratelimit', 'STRING', 'Cache key prefix for rate limiting', 'CACHE', true),
('notification.cache.template-prefix', 'notification:template', 'STRING', 'Cache key prefix for templates', 'CACHE', true),
('notification.cache.preference-prefix', 'notification:preference', 'STRING', 'Cache key prefix for user preferences', 'CACHE', true),

-- Metrics Configuration
('notification.metrics.sent-counter', 'notification.sent.total', 'STRING', 'Metric name for sent notifications counter', 'METRICS', true),
('notification.metrics.delivery-timer', 'notification.delivery.time', 'STRING', 'Metric name for delivery time timer', 'METRICS', true),
('notification.metrics.retry-counter', 'notification.retry.count', 'STRING', 'Metric name for retry counter', 'METRICS', true),
('notification.metrics.provider-health-gauge', 'provider.health.status', 'STRING', 'Metric name for provider health gauge', 'METRICS', true),
('notification.metrics.template-render-timer', 'template.render.time', 'STRING', 'Metric name for template render timer', 'METRICS', true),
('notification.metrics.rate-limit-counter', 'rate.limit.exceeded', 'STRING', 'Metric name for rate limit counter', 'METRICS', true),
('notification.metrics.failed-counter', 'notification.failed.total', 'STRING', 'Metric name for failed notifications counter', 'METRICS', true),
('notification.metrics.deduplication-counter', 'notification.deduplication.hit', 'STRING', 'Metric name for deduplication hits', 'METRICS', true),
('notification.metrics.scheduled-counter', 'notification.scheduled.processed', 'STRING', 'Metric name for scheduled notifications', 'METRICS', true),
('notification.metrics.webhook-counter', 'notification.webhook.received', 'STRING', 'Metric name for webhooks received', 'METRICS', true),

-- Rate Limiting Configuration
('notification.rate-limit.enabled', 'true', 'BOOLEAN', 'Enable rate limiting', 'RATE_LIMIT', true),
('notification.rate-limit.default-per-minute', '60', 'INTEGER', 'Default rate limit per minute', 'RATE_LIMIT', true),

-- Tracing Configuration
('notification.tracing.enabled', 'true', 'BOOLEAN', 'Enable distributed tracing', 'TRACING', true),
('notification.tracing.sampling-rate', '1.0', 'DECIMAL', 'Trace sampling rate (0.0 to 1.0)', 'TRACING', true);
