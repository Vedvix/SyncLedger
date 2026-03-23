-- Increase s3_url column size on invoices table
-- AWS presigned URLs are typically 1000-2000+ characters, exceeding VARCHAR(500)
ALTER TABLE invoices ALTER COLUMN s3_url TYPE VARCHAR(2048);
