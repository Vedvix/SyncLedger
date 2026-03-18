# =============================================================================
# SYNCLEDGER - ACM CERTIFICATE + HTTPS
# DNS is managed manually in Squarespace.
# ACM uses DNS validation — add the CNAME from the output to Squarespace.
#
# Prod (ALB):  HTTPS via ACM cert on port 443 + HTTP→HTTPS redirect
# =============================================================================

# ---- ACM Certificate (prod with domain) ----
resource "aws_acm_certificate" "app" {
  count             = var.domain_name != "" && var.environment == "prod" ? 1 : 0
  domain_name       = var.domain_name
  validation_method = "DNS"

  tags = { Name = "${local.name_prefix}-cert" }

  lifecycle {
    create_before_destroy = true
  }
}

# ---- Certificate Validation (waits until CNAME is added in Squarespace) ----
resource "aws_acm_certificate_validation" "app" {
  count           = var.domain_name != "" && var.environment == "prod" ? 1 : 0
  certificate_arn = aws_acm_certificate.app[0].arn
  # No validation_record_fqdns — Terraform polls ACM until cert is ISSUED.
  # Add the CNAME from `acm_validation_records` output to Squarespace DNS.
}

# ---- HTTPS Listener (port 443, prod only) ----
resource "aws_lb_listener" "https" {
  count             = var.environment == "prod" && var.domain_name != "" ? 1 : 0
  load_balancer_arn = aws_lb.app[0].arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate_validation.app[0].certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app[0].arn
  }
}
