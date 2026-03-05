-- ============================================================
-- V17: Create invoice_audit_events table for lifecycle tracking
-- ============================================================

CREATE TABLE invoice_audit_events (
    id              BIGSERIAL       PRIMARY KEY,
    invoice_id      BIGINT          NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    organization_id BIGINT          NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    event_type      VARCHAR(50)     NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30),
    performed_by_user_id BIGINT     REFERENCES users(id) ON DELETE SET NULL,
    performed_by_name    VARCHAR(200),
    description     VARCHAR(1000),
    metadata        JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Indexes for fast lookups
CREATE INDEX idx_iae_invoice_id  ON invoice_audit_events(invoice_id);
CREATE INDEX idx_iae_org_id      ON invoice_audit_events(organization_id);
CREATE INDEX idx_iae_event_type  ON invoice_audit_events(event_type);
CREATE INDEX idx_iae_created_at  ON invoice_audit_events(created_at);

COMMENT ON TABLE invoice_audit_events IS 'Full lifecycle audit trail for invoices — receipt to ERP sync';
