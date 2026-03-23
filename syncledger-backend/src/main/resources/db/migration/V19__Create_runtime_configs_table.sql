-- Runtime configuration table for dynamic config management
-- Allows Super Admin to change application settings at runtime without restart

CREATE TABLE runtime_configs (
    id              BIGSERIAL PRIMARY KEY,
    config_key      VARCHAR(255) NOT NULL UNIQUE,
    config_value    TEXT NOT NULL,
    default_value   TEXT NOT NULL,
    description     VARCHAR(500),
    category        VARCHAR(100) NOT NULL,
    data_type       VARCHAR(50) NOT NULL DEFAULT 'STRING',
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_runtime_config_key ON runtime_configs(config_key);
CREATE INDEX idx_runtime_config_category ON runtime_configs(category);

-- Seed default runtime configurations
INSERT INTO runtime_configs (config_key, config_value, default_value, description, category, data_type) VALUES
    ('cors.allowed-origins', 'http://localhost:3000,http://localhost:5173,http://localhost:8080', 'http://localhost:3000,http://localhost:5173,http://localhost:8080', 'Comma-separated list of allowed CORS origins', 'SECURITY', 'STRING'),
    ('logging.level.root', 'INFO', 'INFO', 'Root logging level (TRACE, DEBUG, INFO, WARN, ERROR)', 'LOGGING', 'STRING'),
    ('logging.level.com.vedvix.syncledger', 'DEBUG', 'DEBUG', 'Application logging level (TRACE, DEBUG, INFO, WARN, ERROR)', 'LOGGING', 'STRING'),
    ('logging.level.org.springframework.security', 'DEBUG', 'DEBUG', 'Spring Security logging level', 'LOGGING', 'STRING'),
    ('logging.level.org.hibernate.SQL', 'DEBUG', 'DEBUG', 'Hibernate SQL logging level', 'LOGGING', 'STRING'),
    ('invoice.auto-approval.confidence-threshold', '0.87', '0.87', 'Confidence threshold (0.0-1.0) for auto-approval of invoices', 'INVOICE', 'DECIMAL'),
    ('email.polling.enabled', 'false', 'false', 'Enable/disable email polling', 'EMAIL', 'BOOLEAN'),
    ('email.polling.interval', '300000', '300000', 'Email polling interval in milliseconds', 'EMAIL', 'INTEGER'),
    ('email.polling.max-emails-per-batch', '50', '50', 'Maximum emails to process per polling batch', 'EMAIL', 'INTEGER'),
    ('pdf-service.url', 'http://localhost:8001', 'http://localhost:8001', 'URL of the PDF extraction microservice', 'SERVICE', 'STRING'),
    ('pdf-service.timeout', '30000', '30000', 'PDF service timeout in milliseconds', 'SERVICE', 'INTEGER'),
    ('subscription.scheduler.cron', '0 0 6 * * *', '0 0 6 * * *', 'Cron expression for subscription lifecycle scheduler (requires restart)', 'SCHEDULER', 'STRING');
