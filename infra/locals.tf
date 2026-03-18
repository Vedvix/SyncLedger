# =============================================================================
# SYNCLEDGER - COMPUTED LOCALS
# =============================================================================

locals {
  name_prefix       = "${var.project_name}-${var.environment}"
  s3_storage_bucket = "${var.project_name}-${var.environment}-invoices"

  # AMI auto-detection
  is_arm = can(regex("g\\.", var.ec2_instance_type)) || can(regex("t4g", var.ec2_instance_type))
}

# Auto-resolve latest Amazon Linux 2023 AMI for the right architecture
data "aws_ssm_parameter" "al2023_x86" {
  count = local.is_arm ? 0 : 1
  name  = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

data "aws_ssm_parameter" "al2023_arm" {
  count = local.is_arm ? 1 : 0
  name  = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64"
}

locals {
  ami_id = (
    local.is_arm
    ? data.aws_ssm_parameter.al2023_arm[0].value
    : data.aws_ssm_parameter.al2023_x86[0].value
  )

  # CORS origins based on environment
  # Priority: explicit variable > domain-based > localhost defaults
  cors_origins = (
    var.cors_allowed_origins != "" ? var.cors_allowed_origins :
    var.domain_name != "" ? "https://${var.domain_name}" :
    "http://localhost:3000,http://localhost:5173"
  )

  # Cost estimates (heredocs can't be used in ternary expressions)
  cost_estimate_prod = <<-EOT

    =====================================================
      SYNCLEDGER PROD - COST ESTIMATE
    =====================================================
      EC2 ${var.ec2_instance_type} x ASG     ~$8-45/mo
      ALB                                    ~$16-22/mo
      RDS ${var.db_instance_type}            ~$12-15/mo
      EBS 20GB gp3 per instance              ~$1.60/mo
      S3 Storage (< 5GB)                     ~$0.12/mo
      Secrets Manager (2 secrets)            ~$0.80/mo
      CloudWatch Logs + Alarms               ~$3.50/mo
      Data Transfer (10GB)                   ~$0.90/mo
      NO NAT Gateway                         $0 (saves $32/mo)
    -----------------------------------------------------
      TOTAL (1 instance)                     ~$43-58/mo
      TOTAL (scaled to ${var.asg_max} instances)       ~$59-100/mo
    =====================================================
  EOT

  cost_estimate_nonprod = <<-EOT

    =====================================================
      SYNCLEDGER ${upper(var.environment)} - COST ESTIMATE
    =====================================================
      EC2 ${var.ec2_instance_type}           ~$8-15/mo
      RDS ${var.db_instance_type}            ~$12-15/mo
      EBS 20GB gp3                           ~$1.60/mo
      Elastic IP (attached)                  FREE
      S3 Storage (< 5GB)                     ~$0.12/mo
      Secrets Manager (2 secrets)            ~$0.80/mo
      CloudWatch Logs                        ~$2.50/mo
      Data Transfer (10GB)                   ~$0.90/mo
      NO ALB (direct EC2)                    $0 (saves $16/mo)
      NO NAT Gateway                         $0 (saves $32/mo)
    -----------------------------------------------------
      TOTAL                                  ~$25-35/mo
      With Free Tier (first 12 months)       ~$5-10/mo
    =====================================================
  EOT
}
