-- V22: Add separate sender credentials for Sage Intacct XML API
-- Sage Intacct XML API requires TWO layers of authentication:
--   1. Sender credentials (sender_id + sender_password) — identifies the application
--   2. Login credentials (user_id + company_id + user_password) — identifies the user/company
-- Previously, the system conflated both or relied on app-level env vars.
-- This migration adds per-org sender credentials so each org can have its own.

ALTER TABLE organizations ADD COLUMN IF NOT EXISTS erp_sender_id VARCHAR(255);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS erp_sender_password_encrypted VARCHAR(1000);
