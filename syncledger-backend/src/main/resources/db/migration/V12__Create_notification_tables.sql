-- ============================================================
-- V12: Create notification module tables
-- ============================================================

-- PostgreSQL enum type for notification_event_type (used as NAMED_ENUM by Hibernate)
CREATE TYPE notification_event_type AS ENUM (
    'CREATED', 'QUEUED', 'SENT', 'SMS_SENT', 'EMAIL_SENT', 'PUSH_SENT',
    'DELIVERED', 'READ', 'FAILED', 'RETRIED', 'CANCELLED', 'CLICKED', 'DISMISSED',
    'TRIAL_WELCOME', 'TRIAL_EXPIRING_3D', 'TRIAL_EXPIRING_1D', 'TRIAL_EXPIRED',
    'SUBSCRIPTION_ACTIVATED', 'SUBSCRIPTION_EXPIRING_7D', 'SUBSCRIPTION_EXPIRING_3D',
    'SUBSCRIPTION_EXPIRED', 'SUBSCRIPTION_CANCELLED',
    'SUBSCRIPTION_PAYMENT_SUCCEEDED', 'SUBSCRIPTION_PAYMENT_FAILED',
    'NEW_ORG_SIGNUP_ADMIN'
);

-- Notifications table
CREATE TABLE notifications (
    notification_id VARCHAR(100) PRIMARY KEY,
    channel        VARCHAR(20)  NOT NULL,
    template_name  VARCHAR(100) NOT NULL,
    recipients_json JSONB       NOT NULL,
    parameters_json TEXT,
    priority       VARCHAR(20)  NOT NULL,
    scheduled_at   TIMESTAMPTZ,
    metadata_json  TEXT,
    tenant_id      VARCHAR(100),
    status         VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ,
    sent_at        TIMESTAMPTZ,
    attempt_count  INTEGER      NOT NULL DEFAULT 0,
    error_message  TEXT,
    provider_id    VARCHAR(50),
    external_id    VARCHAR(255)
);

CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_tenant_created ON notifications (tenant_id, created_at);
CREATE INDEX idx_notifications_scheduled ON notifications (scheduled_at);
CREATE INDEX idx_notifications_channel ON notifications (channel);

-- Notification audit events table
CREATE TABLE notification_audit_events (
    event_id        BIGSERIAL PRIMARY KEY,
    notification_id VARCHAR(100) NOT NULL,
    event_type      notification_event_type NOT NULL,
    channel         VARCHAR(20),
    tenant_id       VARCHAR(100),
    user_id         BIGINT,
    event_data      JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_audit_notification_id ON notification_audit_events (notification_id);
CREATE INDEX idx_audit_tenant_timestamp ON notification_audit_events (tenant_id, created_at DESC);
CREATE INDEX idx_audit_event_type ON notification_audit_events (event_type);
CREATE INDEX idx_audit_timestamp ON notification_audit_events (created_at DESC);
