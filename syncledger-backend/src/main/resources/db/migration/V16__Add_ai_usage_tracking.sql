-- =============================================================================
-- V16: Add AI usage tracking for per-organization token/cost metering
-- =============================================================================

-- AI usage log: one row per extraction, tracks tokens and cost per organization
CREATE TABLE IF NOT EXISTS ai_usage_logs (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    invoice_id      BIGINT REFERENCES invoices(id),
    ai_tier         VARCHAR(30),            -- gpt4o_vision, gpt4o_text, regex_parser
    model_name      VARCHAR(50),            -- gpt-4o, gpt-4o-mini, etc.
    input_tokens    INTEGER NOT NULL DEFAULT 0,
    output_tokens   INTEGER NOT NULL DEFAULT 0,
    total_tokens    INTEGER NOT NULL DEFAULT 0,
    estimated_cost_usd DECIMAL(10, 6) NOT NULL DEFAULT 0,
    processing_time_ms INTEGER,
    success         BOOLEAN NOT NULL DEFAULT true,
    error_message   VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_usage_org_id ON ai_usage_logs(organization_id);
CREATE INDEX idx_ai_usage_created_at ON ai_usage_logs(created_at);
CREATE INDEX idx_ai_usage_org_created ON ai_usage_logs(organization_id, created_at);

-- Add AI metadata columns to invoices table
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS ai_tier_used VARCHAR(30);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS ai_model_name VARCHAR(50);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS ai_input_tokens INTEGER;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS ai_output_tokens INTEGER;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS ai_total_tokens INTEGER;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS ai_cost_usd DECIMAL(10, 6);
