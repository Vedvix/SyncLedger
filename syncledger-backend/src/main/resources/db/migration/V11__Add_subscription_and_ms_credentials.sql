-- =============================================================================
-- SYNCLEDGER DATABASE SCHEMA - SUBSCRIPTION & MS CREDENTIALS
-- Version: V11__Add_subscription_and_ms_credentials.sql
-- Author: vedvix
-- Description: Adds subscription management, per-org Microsoft credentials,
--              and self-signup support for multi-tenant SaaS.
-- =============================================================================

-- =============================================================================
-- ADD TRIAL STATUS TO ORGANIZATION
-- =============================================================================

ALTER TYPE organization_status ADD VALUE IF NOT EXISTS 'TRIAL' AFTER 'ONBOARDING';

-- =============================================================================
-- SUBSCRIPTIONS TABLE
-- =============================================================================

CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Plan & Status
    plan VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    status VARCHAR(30) NOT NULL DEFAULT 'TRIAL',

    -- Trial period
    trial_start_date TIMESTAMP NOT NULL,
    trial_end_date TIMESTAMP NOT NULL,

    -- Subscription period (set when user converts from trial)
    subscription_start_date TIMESTAMP,
    subscription_end_date TIMESTAMP,

    -- Billing
    billing_cycle VARCHAR(20) DEFAULT 'MONTHLY',
    price_cents BIGINT DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),

    -- Cancellation
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(500),

    -- Notification tracking
    trial_expiry_notified_3d BOOLEAN DEFAULT FALSE,
    trial_expiry_notified_1d BOOLEAN DEFAULT FALSE,
    trial_expired_notified BOOLEAN DEFAULT FALSE,
    subscription_expiry_notified_7d BOOLEAN DEFAULT FALSE,
    subscription_expiry_notified_3d BOOLEAN DEFAULT FALSE,
    subscription_expired_notified BOOLEAN DEFAULT FALSE,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Subscriptions indexes
CREATE UNIQUE INDEX idx_subscription_org ON subscriptions(organization_id);
CREATE INDEX idx_subscription_status ON subscriptions(status);
CREATE INDEX idx_subscription_plan ON subscriptions(plan);
CREATE INDEX idx_subscription_trial_end ON subscriptions(trial_end_date);
CREATE INDEX idx_subscription_end ON subscriptions(subscription_end_date);

-- Apply update trigger
CREATE TRIGGER update_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- ADD MICROSOFT CREDENTIALS TO ORGANIZATIONS (per-org, encrypted at rest)
-- =============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'organizations' AND column_name = 'ms_client_id') THEN
        ALTER TABLE organizations ADD COLUMN ms_client_id VARCHAR(500);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'organizations' AND column_name = 'ms_client_secret_encrypted') THEN
        ALTER TABLE organizations ADD COLUMN ms_client_secret_encrypted VARCHAR(1000);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'organizations' AND column_name = 'ms_tenant_id') THEN
        ALTER TABLE organizations ADD COLUMN ms_tenant_id VARCHAR(500);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'organizations' AND column_name = 'ms_mailbox_email') THEN
        ALTER TABLE organizations ADD COLUMN ms_mailbox_email VARCHAR(255);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'organizations' AND column_name = 'ms_credentials_verified') THEN
        ALTER TABLE organizations ADD COLUMN ms_credentials_verified BOOLEAN DEFAULT FALSE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'organizations' AND column_name = 'ms_credentials_verified_at') THEN
        ALTER TABLE organizations ADD COLUMN ms_credentials_verified_at TIMESTAMP;
    END IF;
END $$;

-- =============================================================================
-- SUBSCRIPTION AUDIT LOG TABLE
-- =============================================================================

CREATE TABLE subscription_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    subscription_id BIGINT REFERENCES subscriptions(id),
    event_type VARCHAR(50) NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30),
    old_plan VARCHAR(30),
    new_plan VARCHAR(30),
    metadata JSONB,
    performed_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sub_audit_org ON subscription_audit_logs(organization_id);
CREATE INDEX idx_sub_audit_event ON subscription_audit_logs(event_type);

-- =============================================================================
-- COMMENTS
-- =============================================================================

COMMENT ON TABLE subscriptions IS 'Organization subscription and trial management';
COMMENT ON COLUMN subscriptions.plan IS 'TRIAL, STARTER, PROFESSIONAL, ENTERPRISE';
COMMENT ON COLUMN subscriptions.status IS 'TRIAL, ACTIVE, PAST_DUE, CANCELLED, EXPIRED, SUSPENDED';
COMMENT ON COLUMN organizations.ms_client_secret_encrypted IS 'AES-256-GCM encrypted Microsoft client secret';
COMMENT ON COLUMN organizations.ms_credentials_verified IS 'Whether MS Graph credentials have been validated';
COMMENT ON TABLE subscription_audit_logs IS 'Audit trail for all subscription lifecycle events';
