-- ============================================================================
-- V8: Create vendors table and link invoices to vendors
-- ============================================================================

-- 1. Create vendors table
CREATE TABLE IF NOT EXISTS vendors (
    id                 BIGSERIAL PRIMARY KEY,
    organization_id    BIGINT NOT NULL REFERENCES organizations(id),
    name               VARCHAR(255) NOT NULL,
    normalized_name    VARCHAR(255) NOT NULL,
    code               VARCHAR(100),
    address            VARCHAR(500),
    email              VARCHAR(255),
    phone              VARCHAR(50),
    contact_person     VARCHAR(100),
    website            VARCHAR(255),
    tax_id             VARCHAR(50),
    payment_terms      VARCHAR(50),
    currency           VARCHAR(3) NOT NULL DEFAULT 'USD',
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes              VARCHAR(500),
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_vendor_org_normalized_name UNIQUE (organization_id, normalized_name)
);

-- 2. Indexes on vendors
CREATE INDEX IF NOT EXISTS idx_vendor_org_id ON vendors(organization_id);
CREATE INDEX IF NOT EXISTS idx_vendor_normalized_name ON vendors(normalized_name);
CREATE INDEX IF NOT EXISTS idx_vendor_status ON vendors(status);
CREATE INDEX IF NOT EXISTS idx_vendor_tax_id ON vendors(tax_id);

-- 3. Add vendor_id FK to invoices table
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS vendor_id BIGINT REFERENCES vendors(id);
CREATE INDEX IF NOT EXISTS idx_invoice_vendor_id ON invoices(vendor_id);

-- 4. Auto-update trigger for vendors.updated_at
CREATE OR REPLACE FUNCTION update_vendor_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_vendor_updated_at ON vendors;
CREATE TRIGGER set_vendor_updated_at
    BEFORE UPDATE ON vendors
    FOR EACH ROW
    EXECUTE FUNCTION update_vendor_updated_at();
