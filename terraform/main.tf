# =============================================================================
# SYNCLEDGER - MAIN INFRASTRUCTURE
# Cheapest production setup: Single EC2 + Docker Compose + S3
# Estimated cost: ~$15-25/month
# =============================================================================

locals {
  name_prefix = "${var.project_name}-${var.environment}"
  common_tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# =============================================================================
# DATA SOURCES
# =============================================================================

# Latest Amazon Linux 2023 AMI
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

data "aws_availability_zones" "available" {
  state = "available"
}

# =============================================================================
# VPC - Default VPC (Free, no NAT gateway costs)
# =============================================================================

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# =============================================================================
# SECURITY GROUP
# =============================================================================

resource "aws_security_group" "syncledger" {
  name        = "${local.name_prefix}-sg"
  description = "Security group for SyncLedger application"
  vpc_id      = data.aws_vpc.default.id

  # SSH access
  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.allowed_ssh_cidrs
  }

  # HTTP
  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS
  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # All outbound
  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name_prefix}-sg"
  }
}

# =============================================================================
# S3 BUCKET - PDF/Invoice Storage
# =============================================================================

resource "aws_s3_bucket" "invoices" {
  bucket        = "${local.name_prefix}-invoices"
  force_destroy = var.environment != "prod"

  tags = {
    Name = "${local.name_prefix}-invoices"
  }
}

resource "aws_s3_bucket_versioning" "invoices" {
  bucket = aws_s3_bucket.invoices.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "invoices" {
  bucket = aws_s3_bucket.invoices.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "invoices" {
  bucket = aws_s3_bucket.invoices.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "invoices" {
  bucket = aws_s3_bucket.invoices.id

  rule {
    id     = "transition-to-ia"
    status = "Enabled"

    transition {
      days          = 90
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 365
      storage_class = "GLACIER"
    }
  }
}

# =============================================================================
# IAM ROLE FOR EC2 (Instance Profile)
# =============================================================================

resource "aws_iam_role" "ec2_role" {
  name = "${local.name_prefix}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "ec2_s3_access" {
  name = "${local.name_prefix}-ec2-s3-policy"
  role = aws_iam_role.ec2_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.invoices.arn,
          "${aws_s3_bucket.invoices.arn}/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${local.name_prefix}-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# =============================================================================
# EC2 INSTANCE - Single instance running all services via Docker Compose
# This is the CHEAPEST production option (~$8-15/month)
# =============================================================================

resource "aws_instance" "app" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.instance_type
  key_name               = var.ec2_key_name != "" ? var.ec2_key_name : null
  vpc_security_group_ids = [aws_security_group.syncledger.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  root_block_device {
    volume_size           = 20
    volume_type           = "gp3"
    encrypted             = true
    delete_on_termination = true
  }

  user_data = base64encode(templatefile("${path.module}/templates/user_data.sh", {
    project_name          = var.project_name
    environment           = var.environment
    db_password           = var.db_password
    jwt_secret            = var.jwt_secret
    s3_bucket_name        = aws_s3_bucket.invoices.id
    aws_region            = var.aws_region
    storage_type          = var.storage_type
    email_polling_enabled = var.email_polling_enabled
    domain_name           = var.domain_name
  }))

  tags = {
    Name = "${local.name_prefix}-app"
  }

  lifecycle {
    ignore_changes = [ami, user_data]
  }
}

# =============================================================================
# ELASTIC IP - Static public IP (Free when attached to running instance)
# =============================================================================

resource "aws_eip" "app" {
  instance = aws_instance.app.id
  domain   = "vpc"

  tags = {
    Name = "${local.name_prefix}-eip"
  }
}

# =============================================================================
# CLOUDWATCH LOG GROUP
# =============================================================================

resource "aws_cloudwatch_log_group" "app" {
  name              = "/syncledger/${var.environment}"
  retention_in_days = 30

  tags = {
    Name = "${local.name_prefix}-logs"
  }
}

# =============================================================================
# SSM PARAMETERS - Secure secret storage (Free tier: standard parameters)
# =============================================================================

resource "aws_ssm_parameter" "db_password" {
  name  = "/${var.project_name}/${var.environment}/db-password"
  type  = "SecureString"
  value = var.db_password

  tags = {
    Name = "${local.name_prefix}-db-password"
  }
}

resource "aws_ssm_parameter" "jwt_secret" {
  name  = "/${var.project_name}/${var.environment}/jwt-secret"
  type  = "SecureString"
  value = var.jwt_secret

  tags = {
    Name = "${local.name_prefix}-jwt-secret"
  }
}
