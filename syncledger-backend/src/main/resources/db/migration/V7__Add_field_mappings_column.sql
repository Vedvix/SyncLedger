-- =============================================================================
-- V7: Add field_mappings column to invoices table
-- Stores the JSON trace of mapping rules applied during extraction
-- =============================================================================

ALTER TABLE invoices ADD COLUMN IF NOT EXISTS field_mappings TEXT;

COMMENT ON COLUMN invoices.field_mappings IS 'JSON array of field mapping rules applied during extraction (target, source, value, rule)';
