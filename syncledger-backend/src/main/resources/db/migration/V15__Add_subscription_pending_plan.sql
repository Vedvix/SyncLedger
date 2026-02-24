-- V15: Add pending plan columns to subscriptions for Stripe checkout flow
-- Stores the intended plan & billing cycle during Stripe Checkout Session,
-- so the webhook handler knows which plan to activate after payment.

ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS pending_plan VARCHAR(30);
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS pending_billing_cycle VARCHAR(20);
