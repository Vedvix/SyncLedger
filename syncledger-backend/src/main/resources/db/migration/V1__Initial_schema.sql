-- =============================================================================
-- SYNCLEDGER DATABASE SCHEMA
-- Version: V1__Initial_schema.sql
-- Author: vedvix
-- Description: Creates all tables for the SyncLedger invoice processing system
-- =============================================================================

-- =============================================================================
-- ENUM TYPES (PostgreSQL native enums for better performance)
-- =============================================================================

CREATE TYPE user_role AS ENUM ('ADMIN', 'APPROVER', 'VIEWER');
CREATE TYPE invoice_status AS ENUM ('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'SYNCED', 'SYNC_FAILED', 'ARCHIVED');
CREATE TYPE sync_status AS ENUM ('PENDING', 'IN_PROGRESS', 'SUCCESS', 'FAILED', 'RETRYING');
CREATE TYPE approval_action AS ENUM ('APPROVED', 'REJECTED', 'ESCALATED', 'RETURNED_FOR_REVIEW');

-- =============================================================================
-- USERS TABLE
-- =============================================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    profile_picture_url VARCHAR(500),
    phone VARCHAR(20),
    department VARCHAR(100),
    job_title VARCHAR(200),
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    password_reset_token VARCHAR(255),
    password_reset_token_expiry TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id BIGINT REFERENCES users(id)
);

-- Users indexes
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_role ON users(role);
CREATE INDEX idx_user_active ON users(is_active);

-- =============================================================================
-- INVOICES TABLE
-- =============================================================================

CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    
    -- Invoice Identification
    invoice_number VARCHAR(100) NOT NULL,
    po_number VARCHAR(100),
    
    -- Vendor Information
    vendor_name VARCHAR(255) NOT NULL,
    vendor_address VARCHAR(255),
    vendor_email VARCHAR(100),
    vendor_phone VARCHAR(50),
    vendor_tax_id VARCHAR(50),
    
    -- Financial Details
    subtotal DECIMAL(15, 2) NOT NULL,
    tax_amount DECIMAL(15, 2) DEFAULT 0,
    discount_amount DECIMAL(15, 2) DEFAULT 0,
    shipping_amount DECIMAL(15, 2) DEFAULT 0,
    total_amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    
    -- Dates
    invoice_date DATE NOT NULL,
    due_date DATE,
    received_date DATE,
    
    -- Status & Processing
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    confidence_score DECIMAL(5, 2),
    requires_manual_review BOOLEAN DEFAULT FALSE,
    review_notes VARCHAR(500),
    
    -- File Storage
    original_file_name VARCHAR(500) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    s3_url VARCHAR(500),
    file_size_bytes BIGINT,
    mime_type VARCHAR(50),
    page_count INTEGER,
    
    -- Email Source
    source_email_id VARCHAR(500),
    source_email_from VARCHAR(255),
    source_email_subject VARCHAR(500),
    source_email_received_at TIMESTAMP,
    
    -- Extraction Metadata
    extraction_method VARCHAR(50),
    extracted_at TIMESTAMP,
    extraction_duration_ms INTEGER,
    raw_extracted_data TEXT,
    
    -- Sage Integration
    sage_invoice_id VARCHAR(100),
    sage_vendor_id VARCHAR(100),
    sync_status VARCHAR(20),
    last_sync_attempt TIMESTAMP,
    sync_attempt_count INTEGER DEFAULT 0,
    sync_error_message VARCHAR(500),
    
    -- Audit Fields
    assigned_to_id BIGINT REFERENCES users(id),
    processed_by_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Invoices indexes
CREATE INDEX idx_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoice_status ON invoices(status);
CREATE INDEX idx_invoice_vendor ON invoices(vendor_name);
CREATE INDEX idx_invoice_date ON invoices(invoice_date);
CREATE INDEX idx_invoice_due_date ON invoices(due_date);
CREATE INDEX idx_invoice_sync_status ON invoices(sync_status);
CREATE INDEX idx_invoice_assigned_to ON invoices(assigned_to_id);

-- =============================================================================
-- INVOICE LINE ITEMS TABLE
-- =============================================================================

CREATE TABLE invoice_line_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    description VARCHAR(500),
    item_code VARCHAR(100),
    unit VARCHAR(50),
    quantity DECIMAL(15, 4),
    unit_price DECIMAL(15, 4),
    tax_rate DECIMAL(5, 2),
    tax_amount DECIMAL(15, 2),
    discount_amount DECIMAL(15, 2),
    line_total DECIMAL(15, 2) NOT NULL,
    gl_account_code VARCHAR(100),
    cost_center VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Line items indexes
CREATE INDEX idx_line_item_invoice ON invoice_line_items(invoice_id);

-- =============================================================================
-- APPROVALS TABLE
-- =============================================================================

CREATE TABLE approvals (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    approver_id BIGINT NOT NULL REFERENCES users(id),
    action VARCHAR(30) NOT NULL,
    comments VARCHAR(1000),
    rejection_reason VARCHAR(500),
    approval_level INTEGER,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Approvals indexes
CREATE INDEX idx_approval_invoice ON approvals(invoice_id);
CREATE INDEX idx_approval_user ON approvals(approver_id);
CREATE INDEX idx_approval_action ON approvals(action);

-- =============================================================================
-- AUDIT LOGS TABLE
-- =============================================================================

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    user_email VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    description VARCHAR(500),
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    request_uri VARCHAR(500),
    request_method VARCHAR(10),
    response_status INTEGER,
    duration_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Audit logs indexes
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- =============================================================================
-- EMAIL LOGS TABLE
-- =============================================================================

CREATE TABLE email_logs (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(500) NOT NULL UNIQUE,
    internet_message_id VARCHAR(500),
    from_address VARCHAR(255),
    from_name VARCHAR(255),
    to_addresses VARCHAR(1000),
    subject VARCHAR(500),
    body_preview TEXT,
    received_at TIMESTAMP NOT NULL,
    has_attachments BOOLEAN DEFAULT FALSE,
    attachment_count INTEGER,
    attachment_names VARCHAR(1000),
    is_processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP,
    invoices_extracted INTEGER,
    has_error BOOLEAN DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Email logs indexes
CREATE INDEX idx_email_log_message_id ON email_logs(message_id);
CREATE INDEX idx_email_log_from ON email_logs(from_address);
CREATE INDEX idx_email_log_processed ON email_logs(is_processed);
CREATE INDEX idx_email_log_received ON email_logs(received_at);

-- =============================================================================
-- SAGE SYNC LOGS TABLE
-- =============================================================================

CREATE TABLE sage_sync_logs (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    sage_invoice_id VARCHAR(100),
    sage_transaction_id VARCHAR(100),
    request_payload TEXT,
    response_payload TEXT,
    http_status_code INTEGER,
    error_message VARCHAR(1000),
    error_code VARCHAR(100),
    attempt_number INTEGER,
    duration_ms INTEGER,
    triggered_by_id BIGINT REFERENCES users(id),
    trigger_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Sage sync logs indexes
CREATE INDEX idx_sage_sync_invoice ON sage_sync_logs(invoice_id);
CREATE INDEX idx_sage_sync_status ON sage_sync_logs(status);
CREATE INDEX idx_sage_sync_created ON sage_sync_logs(created_at);

-- =============================================================================
-- UPDATE TIMESTAMP TRIGGER FUNCTION
-- =============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to tables with updated_at column
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_invoices_updated_at
    BEFORE UPDATE ON invoices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
