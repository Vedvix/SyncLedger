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
  cors_origins = var.domain_name != "" ? "https://${var.domain_name}" : "http://localhost"
}
