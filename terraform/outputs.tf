# =============================================================================
# SYNCLEDGER - TERRAFORM OUTPUTS
# =============================================================================

output "app_public_ip" {
  description = "Public IP address of the application server"
  value       = aws_eip.app.public_ip
}

output "app_url" {
  description = "Application URL"
  value       = var.domain_name != "" ? "https://${var.domain_name}" : "http://${aws_eip.app.public_ip}"
}

output "api_url" {
  description = "Backend API URL"
  value       = var.domain_name != "" ? "https://${var.domain_name}/api" : "http://${aws_eip.app.public_ip}/api"
}

output "s3_bucket_name" {
  description = "S3 bucket for invoice storage"
  value       = aws_s3_bucket.invoices.id
}

output "s3_bucket_arn" {
  description = "S3 bucket ARN"
  value       = aws_s3_bucket.invoices.arn
}

output "ec2_instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.app.id
}

output "security_group_id" {
  description = "Security group ID"
  value       = aws_security_group.syncledger.id
}

output "cloudwatch_log_group" {
  description = "CloudWatch log group name"
  value       = aws_cloudwatch_log_group.app.name
}

output "ssh_command" {
  description = "SSH command to connect to the instance"
  value       = var.ec2_key_name != "" ? "ssh -i ${var.ec2_key_name}.pem ec2-user@${aws_eip.app.public_ip}" : "Use EC2 Instance Connect or SSM Session Manager"
}

output "cost_estimate" {
  description = "Estimated monthly cost breakdown"
  value = <<-EOT
    
    ╔═══════════════════════════════════════════════════════════════╗
    ║           SYNCLEDGER - ESTIMATED MONTHLY COSTS               ║
    ╠═══════════════════════════════════════════════════════════════╣
    ║  EC2 ${var.instance_type} (on-demand)     ~$8-15/mo          ║
    ║  EBS 20GB gp3                              ~$1.60/mo         ║
    ║  Elastic IP (attached)                     FREE              ║
    ║  S3 Storage (< 5GB)                        ~$0.12/mo         ║
    ║  S3 Requests                               ~$0.05/mo         ║
    ║  CloudWatch Logs (5GB)                     ~$2.50/mo         ║
    ║  SSM Parameters                            FREE              ║
    ║  Data Transfer (10GB)                      ~$0.90/mo         ║
    ╠═══════════════════════════════════════════════════════════════╣
    ║  TOTAL ESTIMATED                           ~$15-25/mo        ║
    ║                                                               ║
    ║  💡 With Free Tier (first 12 months):      ~$3-8/mo          ║
    ║  💡 With Reserved Instance (1yr):          ~$5-10/mo         ║
    ╚═══════════════════════════════════════════════════════════════╝
  EOT
}
