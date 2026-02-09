-- =============================================================================
-- SYNCLEDGER SEED DATA
-- Version: V2__Seed_data.sql
-- Author: vedvix
-- Description: Inserts initial seed data including default admin user
-- =============================================================================

-- =============================================================================
-- DEFAULT ADMIN USER
-- Email: admin@syncledger.local
-- Password: Admin@123 (BCrypt encoded)
-- =============================================================================

INSERT INTO users (
    first_name,
    last_name,
    email,
    password_hash,
    role,
    is_active,
    department,
    job_title,
    created_at,
    updated_at
) VALUES (
    'System',
    'Administrator',
    'admin@syncledger.local',
    -- BCrypt hash of 'Admin@123' (cost factor 10)
    '$2b$10$CHN7eX6tHAouC8JXHMVz/OUCXH48LLK1HE1DBLKg1T7ez65Yw6koG',
    'SUPER_ADMIN',
    TRUE,
    'IT',
    'System Administrator',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =============================================================================
-- SAMPLE USERS FOR TESTING (Optional - can be removed in production)
-- =============================================================================

-- Approver User (Password: Test@123)
INSERT INTO users (
    first_name,
    last_name,
    email,
    password_hash,
    role,
    is_active,
    department,
    job_title,
    created_by_id,
    created_at,
    updated_at
) VALUES (
    'John',
    'Approver',
    'approver@syncledger.local',
    '$2b$10$CHN7eX6tHAouC8JXHMVz/OUCXH48LLK1HE1DBLKg1T7ez65Yw6koG',
    'APPROVER',
    TRUE,
    'Finance',
    'Finance Manager',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Viewer User (Password: Test@123)
INSERT INTO users (
    first_name,
    last_name,
    email,
    password_hash,
    role,
    is_active,
    department,
    job_title,
    created_by_id,
    created_at,
    updated_at
) VALUES (
    'Jane',
    'Viewer',
    'viewer@syncledger.local',
    '$2b$10$CHN7eX6tHAouC8JXHMVz/OUCXH48LLK1HE1DBLKg1T7ez65Yw6koG',
    'VIEWER',
    TRUE,
    'Operations',
    'Operations Analyst',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =============================================================================
-- AUDIT LOG FOR INITIAL SETUP
-- =============================================================================

INSERT INTO audit_logs (
    user_email,
    action,
    entity_type,
    description,
    created_at
) VALUES (
    'system',
    'DATABASE_INITIALIZED',
    'SYSTEM',
    'Database initialized with Flyway migration V2 - Seed data',
    CURRENT_TIMESTAMP
);
