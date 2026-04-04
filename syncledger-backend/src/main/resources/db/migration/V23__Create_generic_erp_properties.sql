-- V23: Create generic erp_properties table for dynamic ERP configuration
-- Instead of hardcoded columns per ERP (erp_tenant_id, erp_sender_id, etc.),
-- each ERP type defines its own property keys and values are stored as key-value pairs.
-- Secret values are encrypted at rest by the application layer.

CREATE TABLE IF NOT EXISTS erp_properties (
    id                  BIGSERIAL PRIMARY KEY,
    organization_id     BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    erp_type            VARCHAR(20)  NOT NULL,
    property_key        VARCHAR(100) NOT NULL,
    property_value      VARCHAR(2000),           -- plaintext or encrypted (app decides)
    is_encrypted        BOOLEAN      NOT NULL DEFAULT false,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_erp_prop_org_type_key UNIQUE (organization_id, erp_type, property_key)
);

CREATE INDEX IF NOT EXISTS idx_erp_props_org     ON erp_properties(organization_id);
CREATE INDEX IF NOT EXISTS idx_erp_props_org_type ON erp_properties(organization_id, erp_type);

-- Migrate existing Sage Intacct credentials from organizations columns → erp_properties rows
-- This preserves existing configurations for organizations that already have Sage set up.
INSERT INTO erp_properties (organization_id, erp_type, property_key, property_value, is_encrypted)
SELECT id, 'SAGE', 'company_id', erp_company_id, false
FROM organizations WHERE erp_type = 'SAGE' AND erp_company_id IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO erp_properties (organization_id, erp_type, property_key, property_value, is_encrypted)
SELECT id, 'SAGE', 'user_id', erp_tenant_id, false
FROM organizations WHERE erp_type = 'SAGE' AND erp_tenant_id IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO erp_properties (organization_id, erp_type, property_key, property_value, is_encrypted)
SELECT id, 'SAGE', 'user_password', erp_api_key_encrypted, true
FROM organizations WHERE erp_type = 'SAGE' AND erp_api_key_encrypted IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO erp_properties (organization_id, erp_type, property_key, property_value, is_encrypted)
SELECT id, 'SAGE', 'sender_id', erp_sender_id, false
FROM organizations WHERE erp_type = 'SAGE' AND erp_sender_id IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO erp_properties (organization_id, erp_type, property_key, property_value, is_encrypted)
SELECT id, 'SAGE', 'sender_password', erp_sender_password_encrypted, true
FROM organizations WHERE erp_type = 'SAGE' AND erp_sender_password_encrypted IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO erp_properties (organization_id, erp_type, property_key, property_value, is_encrypted)
SELECT id, 'SAGE', 'gateway_url', erp_api_endpoint, false
FROM organizations WHERE erp_type = 'SAGE' AND erp_api_endpoint IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO erp_properties (organization_id, erp_type, property_key, property_value, is_encrypted)
SELECT id, 'SAGE', 'auto_sync', CASE WHEN erp_auto_sync = true THEN 'true' ELSE 'false' END, false
FROM organizations WHERE erp_type = 'SAGE'
ON CONFLICT DO NOTHING;
