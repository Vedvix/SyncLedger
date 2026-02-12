-- V6__Add_mapping_fields.sql
-- Add configurable field mapping support columns to invoices and invoice_line_items tables.
-- These fields store the results of the mapping engine applied during extraction.

-- Invoice-level mapped fields
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='invoices' AND column_name='gl_account') THEN
        ALTER TABLE invoices ADD COLUMN gl_account VARCHAR(100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='invoices' AND column_name='project') THEN
        ALTER TABLE invoices ADD COLUMN project VARCHAR(255);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='invoices' AND column_name='item_category') THEN
        ALTER TABLE invoices ADD COLUMN item_category VARCHAR(255);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='invoices' AND column_name='location') THEN
        ALTER TABLE invoices ADD COLUMN location VARCHAR(500);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='invoices' AND column_name='cost_center') THEN
        ALTER TABLE invoices ADD COLUMN cost_center VARCHAR(100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='invoices' AND column_name='mapping_profile_id') THEN
        ALTER TABLE invoices ADD COLUMN mapping_profile_id VARCHAR(100);
    END IF;
END $$;

-- Add indexes for commonly queried mapping fields
CREATE INDEX IF NOT EXISTS idx_invoice_gl_account ON invoices(gl_account);
CREATE INDEX IF NOT EXISTS idx_invoice_project ON invoices(project);
CREATE INDEX IF NOT EXISTS idx_invoice_mapping_profile ON invoices(mapping_profile_id);

-- Add audit log entry for migration
INSERT INTO audit_logs (user_email, action, entity_type, description, created_at)
VALUES ('system', 'DATABASE_MIGRATION', 'SYSTEM',
        'Added configurable field mapping columns (gl_account, project, item_category, location, cost_center, mapping_profile_id) to invoices table - V6',
        CURRENT_TIMESTAMP);
