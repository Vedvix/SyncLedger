-- =============================================================================
-- SYNCLEDGER DATABASE SCHEMA - EMAIL CONFIGURATION
-- Version: V5__Email_config_updates.sql
-- Author: vedvix
-- Description: Updates for email polling and organization configuration
-- =============================================================================

-- =============================================================================
-- ADD ADDITIONAL ORGANIZATION FIELDS
-- =============================================================================

-- Add contact fields to organizations if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'organizations' AND column_name = 'contact_name') THEN
        ALTER TABLE organizations ADD COLUMN contact_name VARCHAR(255);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'organizations' AND column_name = 'contact_email') THEN
        ALTER TABLE organizations ADD COLUMN contact_email VARCHAR(255);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'organizations' AND column_name = 'contact_phone') THEN
        ALTER TABLE organizations ADD COLUMN contact_phone VARCHAR(50);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'organizations' AND column_name = 'sage_auto_sync') THEN
        ALTER TABLE organizations ADD COLUMN sage_auto_sync BOOLEAN DEFAULT TRUE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'organizations' AND column_name = 'subscription_plan') THEN
        ALTER TABLE organizations ADD COLUMN subscription_plan VARCHAR(50);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'organizations' AND column_name = 'subscription_expires_at') THEN
        ALTER TABLE organizations ADD COLUMN subscription_expires_at TIMESTAMP;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'organizations' AND column_name = 'created_by') THEN
        ALTER TABLE organizations ADD COLUMN created_by BIGINT;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'organizations' AND column_name = 'updated_by') THEN
        ALTER TABLE organizations ADD COLUMN updated_by BIGINT;
    END IF;
END $$;

-- =============================================================================
-- FIX ADMIN PASSWORD (ensure bcrypt hash is correct)
-- =============================================================================

-- Update admin password if exists with invalid hash
-- Password: Admin@123
UPDATE users 
SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMye4tOPmJbWBjAK0F/lPIb93qvHxUFPh8K'
WHERE email = 'admin@syncledger.local' 
AND (password_hash IS NULL OR LENGTH(password_hash) < 50);

-- =============================================================================
-- COMMENTS
-- =============================================================================

COMMENT ON COLUMN organizations.contact_name IS 'Primary contact person name';
COMMENT ON COLUMN organizations.contact_email IS 'Primary contact email address';
COMMENT ON COLUMN organizations.contact_phone IS 'Primary contact phone number';
COMMENT ON COLUMN organizations.sage_auto_sync IS 'Enable automatic sync to Sage';

-- =============================================================================
-- END OF MIGRATION
-- =============================================================================
