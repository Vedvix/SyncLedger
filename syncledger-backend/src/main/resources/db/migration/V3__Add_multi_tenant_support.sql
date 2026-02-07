-- =============================================================================
-- SYNCLEDGER DATABASE SCHEMA - MULTI-TENANT SUPPORT
-- Version: V3__Add_multi_tenant_support.sql
-- Author: vedvix
-- Description: Adds organizations table and updates users/invoices for multi-tenancy
-- =============================================================================

-- =============================================================================
-- ADD ORGANIZATION STATUS ENUM
-- =============================================================================

CREATE TYPE organization_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'ONBOARDING');

-- =============================================================================
-- UPDATE USER ROLE ENUM - ADD SUPER_ADMIN
-- =============================================================================

-- PostgreSQL doesn't allow direct ALTER of ENUM, so we need to add the new value
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'SUPER_ADMIN' BEFORE 'ADMIN';

-- =============================================================================
-- ORGANIZATIONS TABLE
-- =============================================================================

CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    email_address VARCHAR(255) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ONBOARDING',
    
    -- Sage Integration
    sage_api_endpoint VARCHAR(500),
    sage_api_key VARCHAR(500),
    sage_company_id VARCHAR(100),
    
    -- AWS Resources
    s3_folder_path VARCHAR(500),
    sqs_queue_name VARCHAR(255),
    
    -- Settings (JSON)
    settings JSONB,
    
    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Organizations indexes
CREATE INDEX idx_org_slug ON organizations(slug);
CREATE INDEX idx_org_status ON organizations(status);
CREATE INDEX idx_org_email ON organizations(email_address);

-- Apply update trigger to organizations
CREATE TRIGGER update_organizations_updated_at
    BEFORE UPDATE ON organizations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- UPDATE USERS TABLE - ADD ORGANIZATION REFERENCE
-- =============================================================================

-- Add organization_id column (nullable for SUPER_ADMIN)
ALTER TABLE users ADD COLUMN organization_id BIGINT REFERENCES organizations(id);

-- Create index on organization_id
CREATE INDEX idx_user_organization ON users(organization_id);

-- =============================================================================
-- UPDATE INVOICES TABLE - ADD ORGANIZATION REFERENCE
-- =============================================================================

-- Add organization_id column (will be NOT NULL after data migration)
ALTER TABLE invoices ADD COLUMN organization_id BIGINT REFERENCES organizations(id);

-- Create index on organization_id
CREATE INDEX idx_invoice_organization ON invoices(organization_id);

-- =============================================================================
-- UPDATE EMAIL_LOGS TABLE - ADD ORGANIZATION REFERENCE
-- =============================================================================

ALTER TABLE email_logs ADD COLUMN organization_id BIGINT REFERENCES organizations(id);
CREATE INDEX idx_email_log_organization ON email_logs(organization_id);

-- =============================================================================
-- UPDATE AUDIT_LOGS TABLE - ADD ORGANIZATION REFERENCE
-- =============================================================================

ALTER TABLE audit_logs ADD COLUMN organization_id BIGINT REFERENCES organizations(id);
CREATE INDEX idx_audit_log_organization ON audit_logs(organization_id);

-- =============================================================================
-- UPDATE SAGE_SYNC_LOGS TABLE - ADD ORGANIZATION REFERENCE
-- =============================================================================

ALTER TABLE sage_sync_logs ADD COLUMN organization_id BIGINT REFERENCES organizations(id);
CREATE INDEX idx_sage_sync_organization ON sage_sync_logs(organization_id);

-- =============================================================================
-- COMMENTS
-- =============================================================================

COMMENT ON TABLE organizations IS 'Multi-tenant organizations';
COMMENT ON COLUMN organizations.slug IS 'URL-friendly unique identifier';
COMMENT ON COLUMN organizations.email_address IS 'Shared mailbox for invoice receipt';
COMMENT ON COLUMN organizations.s3_folder_path IS 'Organization-specific S3 folder path';
COMMENT ON COLUMN organizations.sqs_queue_name IS 'Organization-specific SQS queue for async processing';
COMMENT ON COLUMN users.organization_id IS 'NULL for SUPER_ADMIN (platform-level), required for all other roles';
COMMENT ON COLUMN invoices.organization_id IS 'Required - every invoice belongs to an organization';
