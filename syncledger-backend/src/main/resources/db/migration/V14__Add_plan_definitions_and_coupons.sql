-- =============================================================================
-- V14 — Dynamic subscription plans + coupon / voucher codes
-- Managed by Super Admin through the admin portal; no code changes needed.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. plan_definitions — fully editable plan catalog
-- ---------------------------------------------------------------------------
CREATE TABLE plan_definitions (
    id              BIGSERIAL PRIMARY KEY,
    plan_key        VARCHAR(50)    NOT NULL UNIQUE,          -- e.g. STARTER, PROFESSIONAL
    display_name    VARCHAR(100)   NOT NULL,
    description     VARCHAR(500),
    monthly_price   BIGINT         NOT NULL DEFAULT 0,       -- cents
    annual_price    BIGINT         NOT NULL DEFAULT 0,       -- cents
    invoices_per_month VARCHAR(30) NOT NULL DEFAULT '0',
    max_users       VARCHAR(30)    NOT NULL DEFAULT '0',
    max_organizations VARCHAR(30)  NOT NULL DEFAULT '0',
    max_email_inboxes VARCHAR(30)  NOT NULL DEFAULT '0',
    storage         VARCHAR(30)    NOT NULL DEFAULT '0',
    approval_type   VARCHAR(50)    NOT NULL DEFAULT 'Basic',
    support_level   VARCHAR(100)   NOT NULL DEFAULT 'Email',
    uptime_sla      VARCHAR(20)    NOT NULL DEFAULT '99.5%',
    highlight       BOOLEAN        NOT NULL DEFAULT FALSE,   -- "Most Popular" flag
    sort_order      INT            NOT NULL DEFAULT 0,
    active          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_plan_definitions_active ON plan_definitions(active);
CREATE INDEX idx_plan_definitions_sort   ON plan_definitions(sort_order);

CREATE TRIGGER update_plan_definitions_updated_at
    BEFORE UPDATE ON plan_definitions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Seed the four default plans ------------------------------------------------
INSERT INTO plan_definitions
    (plan_key, display_name, description, monthly_price, annual_price,
     invoices_per_month, max_users, max_organizations, max_email_inboxes,
     storage, approval_type, support_level, uptime_sla, highlight, sort_order)
VALUES
    ('STARTER',      'Starter',      'For small teams — up to 1,000 invoices/month',
     34900,  349000,  '1,000',   '3',  '1', '1', '50 GB',  'Basic',       'Email (24 hr)',    '99.5%', FALSE, 1),
    ('PROFESSIONAL', 'Professional', 'For growing teams — up to 5,000 invoices/month',
     64900,  649000,  '5,000',   '15', '3', '3', '200 GB', 'Multi-level', 'Priority (4 hr)',  '99.7%', TRUE,  2),
    ('BUSINESS',     'Business',     'For mid-size companies — up to 10,000 invoices/month',
     79900,  799000,  '10,000',  '30', '5', '5', '500 GB', 'Custom',      'Dedicated (2 hr)', '99.8%', FALSE, 3),
    ('ENTERPRISE',   'Enterprise',   'Unlimited usage with dedicated support & SLA',
     149900, 1499000, '20,000+', 'Unlimited', 'Unlimited', 'Unlimited', 'Unlimited', 'Custom', '24/7 (1 hr)', '99.9%', FALSE, 4);

-- ---------------------------------------------------------------------------
-- 2. coupons — voucher / coupon codes
-- ---------------------------------------------------------------------------
CREATE TABLE coupons (
    id                 BIGSERIAL    PRIMARY KEY,
    code               VARCHAR(50)  NOT NULL UNIQUE,
    description        VARCHAR(500),
    discount_type      VARCHAR(20)  NOT NULL DEFAULT 'PERCENTAGE',  -- PERCENTAGE | FIXED_AMOUNT
    discount_value     BIGINT       NOT NULL DEFAULT 0,             -- % (e.g. 20) or cents
    applicable_plans   VARCHAR(255),                                -- comma-separated plan_keys, NULL = all
    max_redemptions    INT,                                         -- NULL = unlimited
    current_redemptions INT         NOT NULL DEFAULT 0,
    valid_from         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until        TIMESTAMP,                                   -- NULL = no expiry
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by         BIGINT,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coupons_code   ON coupons(code);
CREATE INDEX idx_coupons_active ON coupons(active);

CREATE TRIGGER update_coupons_updated_at
    BEFORE UPDATE ON coupons
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ---------------------------------------------------------------------------
-- 3. coupon_redemptions — tracks which orgs used which coupon
-- ---------------------------------------------------------------------------
CREATE TABLE coupon_redemptions (
    id              BIGSERIAL  PRIMARY KEY,
    coupon_id       BIGINT     NOT NULL REFERENCES coupons(id),
    organization_id BIGINT     NOT NULL REFERENCES organizations(id),
    plan_key        VARCHAR(50),
    discount_amount BIGINT     NOT NULL DEFAULT 0,        -- actual discount applied (cents)
    redeemed_by     BIGINT,
    redeemed_at     TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coupon_redemptions_coupon ON coupon_redemptions(coupon_id);
CREATE INDEX idx_coupon_redemptions_org    ON coupon_redemptions(organization_id);

-- Comments -------------------------------------------------------------------
COMMENT ON TABLE  plan_definitions IS 'Admin-editable subscription plan catalog';
COMMENT ON TABLE  coupons          IS 'Voucher / coupon codes managed by Super Admin';
COMMENT ON TABLE  coupon_redemptions IS 'Audit trail – which org redeemed which coupon';
