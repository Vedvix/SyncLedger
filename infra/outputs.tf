# =============================================================================
# SYNCLEDGER - OUTPUTS
# Prod uses ALB + ASG; dev/staging use standalone EC2 + EIP
# =============================================================================

output "ec2_public_ip" {
  description = "Public IP address (EIP for dev/staging, ALB DNS for prod)"
  value       = var.environment == "prod" ? aws_lb.app[0].dns_name : aws_eip.app[0].public_ip
}

output "ec2_instance_id" {
  description = "EC2 instance ID (dev/staging) or ASG name (prod)"
  value       = var.environment == "prod" ? aws_autoscaling_group.app[0].name : aws_instance.app[0].id
}

output "ssm_connect_command" {
  description = "SSM Session Manager connect command (dev/staging only)"
  value       = var.environment != "prod" ? "aws ssm start-session --target ${aws_instance.app[0].id} --region ${var.aws_region}" : "Production uses ASG — connect to individual instances via AWS Console"
}

output "app_url" {
  description = "Application URL"
  value = (
    var.domain_name != "" ? "https://${var.domain_name}" :
    var.environment == "prod" ? "http://${aws_lb.app[0].dns_name}" :
    "http://${aws_eip.app[0].public_ip}"
  )
}

output "api_url" {
  description = "Backend API URL"
  value = (
    var.domain_name != "" ? "https://${var.domain_name}/api" :
    var.environment == "prod" ? "http://${aws_lb.app[0].dns_name}/api" :
    "http://${aws_eip.app[0].public_ip}/api"
  )
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.postgres.address
}

output "rds_port" {
  description = "RDS PostgreSQL port"
  value       = aws_db_instance.postgres.port
}

output "s3_storage_bucket" {
  description = "S3 bucket for invoice storage"
  value       = aws_s3_bucket.storage.id
}

output "s3_storage_bucket_arn" {
  description = "S3 bucket ARN"
  value       = aws_s3_bucket.storage.arn
}

output "cloudwatch_log_group" {
  description = "CloudWatch log group name"
  value       = aws_cloudwatch_log_group.app.name
}

output "alb_dns_name" {
  description = "ALB DNS name (prod only)"
  value       = var.environment == "prod" ? aws_lb.app[0].dns_name : "N/A — no ALB in ${var.environment}"
}

output "asg_name" {
  description = "Auto Scaling Group name (prod only)"
  value       = var.environment == "prod" ? aws_autoscaling_group.app[0].name : "N/A — no ASG in ${var.environment}"
}

output "acm_certificate_arn" {
  description = "ACM certificate ARN"
  value       = var.domain_name != "" ? aws_acm_certificate.app[0].arn : "N/A — set domain_name to enable HTTPS"
}

output "domain_url" {
  description = "Domain URL for this environment"
  value       = var.domain_name != "" ? (var.environment == "prod" ? "https://${var.domain_name}" : "http://${var.domain_name}") : "N/A — no domain configured"
}

output "cost_estimate" {
  description = "Estimated monthly cost"
  value       = var.environment == "prod" ? local.cost_estimate_prod : local.cost_estimate_nonprod
}
