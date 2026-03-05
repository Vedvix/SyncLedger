# =============================================================================
# SYNCLEDGER - OUTPUTS
# =============================================================================

output "ec2_public_ip" {
  description = "Public IP address of the application server"
  value       = aws_eip.app.public_ip
}

output "ec2_instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.app.id
}

output "ssm_connect_command" {
  description = "SSM Session Manager connect command"
  value       = "aws ssm start-session --target ${aws_instance.app.id} --region ${var.aws_region}"
}

output "app_url" {
  description = "Application URL"
  value       = var.domain_name != "" ? "https://${var.domain_name}" : "http://${aws_eip.app.public_ip}"
}

output "api_url" {
  description = "Backend API URL"
  value       = var.domain_name != "" ? "https://${var.domain_name}/api" : "http://${aws_eip.app.public_ip}/api"
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

output "cost_estimate" {
  description = "Estimated monthly cost"
  value       = <<-EOT
    
    =====================================================
      SYNCLEDGER ${upper(var.environment)} - COST ESTIMATE
    =====================================================
      EC2 ${var.ec2_instance_type}          ~$8-15/mo
      RDS ${var.db_instance_type}           ~$12-15/mo
      EBS 20GB gp3                          ~$1.60/mo
      Elastic IP (attached)                 FREE
      S3 Storage (< 5GB)                    ~$0.12/mo
      Secrets Manager (2 secrets)           ~$0.80/mo
      CloudWatch Logs                       ~$2.50/mo
      Data Transfer (10GB)                  ~$0.90/mo
      NO ALB (direct EC2)                   $0 (saves $16/mo)
      NO NAT Gateway                        $0 (saves $32/mo)
    -----------------------------------------------------
      TOTAL                                 ~$25-35/mo
      With Free Tier (first 12 months)      ~$5-10/mo
    =====================================================
  EOT
}
