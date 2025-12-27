-- Create notification_templates table for template management with multi-language support
-- Supports template versioning by locale for internationalization
-- Uses UUID for ID to align with BaseEntity from domain-starter

-- Ensure UUID extension is available
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_code VARCHAR(100) NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    locale VARCHAR(10) NOT NULL DEFAULT 'en_US',
    subject VARCHAR(500),
    body_template TEXT NOT NULL,
    html_template TEXT,
    engine VARCHAR(20) NOT NULL DEFAULT 'PLAIN_TEXT',
    active BOOLEAN DEFAULT true,
    default_variables JSONB,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    CONSTRAINT chk_channel CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP', 'PUSH_NOTIFICATION')),
    CONSTRAINT chk_engine CHECK (engine IN ('THYMELEAF', 'FREEMARKER', 'PLAIN_TEXT')),
    UNIQUE (template_code, locale)
);

-- Create indexes for efficient querying
CREATE INDEX idx_template_code ON notification_templates(template_code);
CREATE INDEX idx_active ON notification_templates(active);
CREATE INDEX idx_channel ON notification_templates(channel);
CREATE INDEX idx_locale ON notification_templates(locale);
CREATE INDEX idx_template_code_locale ON notification_templates(template_code, locale);
CREATE INDEX idx_template_code_active ON notification_templates(template_code, active) WHERE active = true;

-- Add comments for documentation
COMMENT ON TABLE notification_templates IS 'Notification templates with multi-language support and variable substitution';
COMMENT ON COLUMN notification_templates.template_code IS 'Unique template identifier (same code can have multiple locales)';
COMMENT ON COLUMN notification_templates.template_name IS 'Human-readable template name';
COMMENT ON COLUMN notification_templates.channel IS 'Notification channel this template is for';
COMMENT ON COLUMN notification_templates.locale IS 'Language/region code (e.g., en_US, es_ES, fr_FR)';
COMMENT ON COLUMN notification_templates.subject IS 'Email subject or notification title (supports variables)';
COMMENT ON COLUMN notification_templates.body_template IS 'Template body with variable placeholders';
COMMENT ON COLUMN notification_templates.html_template IS 'HTML version of template (for emails)';
COMMENT ON COLUMN notification_templates.engine IS 'Template engine: THYMELEAF, FREEMARKER, or PLAIN_TEXT';
COMMENT ON COLUMN notification_templates.active IS 'Whether template is active and can be used';
COMMENT ON COLUMN notification_templates.default_variables IS 'Default values for template variables as JSON';

-- Insert sample templates for common use cases
INSERT INTO notification_templates (template_code, template_name, channel, locale, subject, body_template, engine, active, default_variables)
VALUES 
    ('WELCOME_EMAIL', 'Welcome Email', 'EMAIL', 'en_US', 
     'Welcome to {{appName}}!',
     'Hello {{userName}},\n\nWelcome to {{appName}}! We''re excited to have you on board.\n\nBest regards,\nThe {{appName}} Team',
     'PLAIN_TEXT', true,
     '{"appName": "MyApp"}'::jsonb),
    
    ('PASSWORD_RESET', 'Password Reset', 'EMAIL', 'en_US',
     'Reset Your Password',
     'Hello {{userName}},\n\nYou requested to reset your password. Click the link below:\n\n{{resetLink}}\n\nThis link expires in {{expiryMinutes}} minutes.\n\nIf you didn''t request this, please ignore this email.',
     'PLAIN_TEXT', true,
     '{"expiryMinutes": "30"}'::jsonb),
    
    ('ORDER_CONFIRMATION', 'Order Confirmation', 'EMAIL', 'en_US',
     'Order Confirmation - {{orderId}}',
     'Hello {{userName}},\n\nYour order {{orderId}} has been confirmed!\n\nOrder Total: {{orderTotal}}\nEstimated Delivery: {{deliveryDate}}\n\nThank you for your purchase!',
     'PLAIN_TEXT', true,
     '{}'::jsonb),
    
    ('OTP_SMS', 'OTP SMS', 'SMS', 'en_US',
     NULL,
     'Your {{appName}} verification code is: {{otp}}. Valid for {{expiryMinutes}} minutes.',
     'PLAIN_TEXT', true,
     '{"appName": "MyApp", "expiryMinutes": "10"}'::jsonb),
    
    ('WELCOME_EMAIL', 'Welcome Email (Spanish)', 'EMAIL', 'es_ES',
     '¡Bienvenido a {{appName}}!',
     'Hola {{userName}},\n\n¡Bienvenido a {{appName}}! Estamos emocionados de tenerte con nosotros.\n\nSaludos cordiales,\nEl equipo de {{appName}}',
     'PLAIN_TEXT', true,
     '{"appName": "MyApp"}'::jsonb);

COMMENT ON TABLE notification_templates IS 'Sample templates are provided. Update or create new templates as needed.';
