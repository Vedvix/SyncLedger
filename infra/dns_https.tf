# =============================================================================
# SYNCLEDGER - ACM CERTIFICATE + DNS RECORDS + HTTPS
# Requires: Route53 hosted zone created in bootstrap (shared across envs)
# Set domain_name + route53_zone_id to enable
#
# Prod (ALB):     HTTPS via ACM + HTTP→HTTPS redirect
# Dev/Staging:    DNS A record → EIP (HTTP)
# =============================================================================

# ---- ACM Certificate (all envs with domain) ----
resource "aws_acm_certificate" "app" {
  count             = var.domain_name != "" ? 1 : 0
  domain_name       = var.domain_name
  validation_method = "DNS"

  tags = { Name = "${local.name_prefix}-cert" }

  lifecycle {
    create_before_destroy = true
  }
}

# ---- DNS Validation Records ----
resource "aws_route53_record" "cert_validation" {
  for_each = {
    for dvo in(var.domain_name != "" ? aws_acm_certificate.app[0].domain_validation_options : []) :
    dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  }

  allow_overwrite = true
  name            = each.value.name
  type            = each.value.type
  zone_id         = var.route53_zone_id
  records         = [each.value.record]
  ttl             = 60
}

# ---- Certificate Validation ----
resource "aws_acm_certificate_validation" "app" {
  count                   = var.domain_name != "" ? 1 : 0
  certificate_arn         = aws_acm_certificate.app[0].arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}

# ---- DNS Record: domain → ALB (prod, alias record) ----
resource "aws_route53_record" "app_alb" {
  count   = var.domain_name != "" && var.environment == "prod" ? 1 : 0
  zone_id = var.route53_zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = aws_lb.app[0].dns_name
    zone_id                = aws_lb.app[0].zone_id
    evaluate_target_health = true
  }
}

# ---- DNS Record: domain → EIP (dev/staging, standard A record) ----
resource "aws_route53_record" "app_eip" {
  count   = var.domain_name != "" && var.environment != "prod" ? 1 : 0
  zone_id = var.route53_zone_id
  name    = var.domain_name
  type    = "A"
  ttl     = 300
  records = [aws_eip.app[0].public_ip]
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
