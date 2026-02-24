-- V13: Add ERP type configuration to organizations and create mapping_profiles table
-- Each organization can use a different ERP system (Sage, NetSuite, Oracle, etc.)
-- Mapping profiles are now persisted in DB and scoped per organization

-- 1) Add ERP type enum and columns to organizations
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'erp_type') THEN
        CREATE TYPE erp_type AS ENUM ('NONE', 'SAGE', 'NETSUITE', 'ORACLE', 'QUICKBOOKS', 'SAP', 'XERO', 'CUSTOM');
    END IF;
END$$;

ALTER TABLE organizations ADD COLUMN IF NOT EXISTS erp_type VARCHAR(20) DEFAULT 'NONE';
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS erp_api_endpoint VARCHAR(500);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS erp_api_key_encrypted VARCHAR(1000);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS erp_tenant_id VARCHAR(255);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS erp_company_id VARCHAR(255);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS erp_auto_sync BOOLEAN DEFAULT true;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS erp_config_json TEXT;

-- Migrate existing Sage config to new ERP fields
UPDATE organizations
SET erp_type = 'SAGE',
    erp_api_endpoint = sage_api_endpoint,
    erp_api_key_encrypted = sage_api_key
WHERE sage_api_endpoint IS NOT NULL AND sage_api_endpoint != '';

-- 2) Create mapping_profiles table (persisted, org-scoped)
CREATE TABLE IF NOT EXISTS mapping_profiles (
    id              VARCHAR(100) PRIMARY KEY,
    organization_id BIGINT       REFERENCES organizations(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    vendor_pattern  VARCHAR(500),
    is_default      BOOLEAN      DEFAULT false,
    is_builtin      BOOLEAN      DEFAULT false,
    erp_type        VARCHAR(20),
    rules_json      TEXT         NOT NULL DEFAULT '[]',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    CONSTRAINT uq_mapping_profile_name_org UNIQUE (organization_id, name)
);

CREATE INDEX IF NOT EXISTS idx_mapping_profiles_org ON mapping_profiles(organization_id);
CREATE INDEX IF NOT EXISTS idx_mapping_profiles_erp ON mapping_profiles(erp_type);
CREATE INDEX IF NOT EXISTS idx_mapping_profiles_default ON mapping_profiles(organization_id, is_default);

-- 3) Create erp_sync_logs table (generalized from sage_sync_logs)
CREATE TABLE IF NOT EXISTS erp_sync_logs (
    id                  BIGSERIAL PRIMARY KEY,
    invoice_id          BIGINT       NOT NULL REFERENCES invoices(id),
    organization_id     BIGINT       NOT NULL REFERENCES organizations(id),
    erp_type            VARCHAR(20)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    erp_invoice_id      VARCHAR(100),
    erp_transaction_id  VARCHAR(100),
    request_payload     TEXT,
    response_payload    TEXT,
    http_status_code    INTEGER,
    error_message       VARCHAR(1000),
    error_code          VARCHAR(100),
    attempt_number      INTEGER      DEFAULT 1,
    duration_ms         INTEGER,
    triggered_by_id     BIGINT       REFERENCES users(id),
    trigger_type        VARCHAR(50),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_erp_sync_invoice ON erp_sync_logs(invoice_id);
CREATE INDEX IF NOT EXISTS idx_erp_sync_org ON erp_sync_logs(organization_id);
CREATE INDEX IF NOT EXISTS idx_erp_sync_status ON erp_sync_logs(status);
CREATE INDEX IF NOT EXISTS idx_erp_sync_erp_type ON erp_sync_logs(erp_type);

-- 4) Add erp_type to invoices for tracking which ERP was used
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS erp_type VARCHAR(20);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS erp_invoice_id VARCHAR(100);
