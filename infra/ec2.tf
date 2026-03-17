# =============================================================================
# SYNCLEDGER - EC2 INSTANCE + IAM
# Single instance running all services via Docker Compose
# =============================================================================

# ---- IAM Role for EC2 ----
resource "aws_iam_role" "ec2" {
  name = "${local.name_prefix}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# S3 storage bucket access (invoice PDFs)
resource "aws_iam_role_policy" "ec2_s3_storage" {
  name = "${local.name_prefix}-s3-storage"
  role = aws_iam_role.ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ]
      Resource = [
        aws_s3_bucket.storage.arn,
        "${aws_s3_bucket.storage.arn}/*"
      ]
    }]
  })
}

# S3 config bucket access (docker-compose download)
resource "aws_iam_role_policy" "ec2_s3_config" {
  name = "${local.name_prefix}-s3-config"
  role = aws_iam_role.ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "s3:GetObject",
        "s3:ListBucket"
      ]
      Resource = [
        "arn:aws:s3:::${var.s3_config_bucket}",
        "arn:aws:s3:::${var.s3_config_bucket}/${var.environment}/*"
      ]
    }]
  })
}

# Secrets Manager access
resource "aws_iam_role_policy" "ec2_secrets" {
  name = "${local.name_prefix}-secrets"
  role = aws_iam_role.ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "secretsmanager:GetSecretValue"
      ]
      Resource = [
        aws_secretsmanager_secret.app_config.arn,
        aws_secretsmanager_secret.ghcr.arn
      ]
    }]
  })
}

# CloudWatch Logs
resource "aws_iam_role_policy" "ec2_logs" {
  name = "${local.name_prefix}-logs"
  role = aws_iam_role.ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ]
      Resource = "arn:aws:logs:*:*:*"
    }]
  })
}

# SSM Session Manager (no SSH key needed)
resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${local.name_prefix}-ec2-profile"
  role = aws_iam_role.ec2.name
}

# ---- EC2 Instance (dev/staging only — prod uses ASG) ----
resource "aws_instance" "app" {
  count                  = var.environment != "prod" ? 1 : 0
  ami                    = local.ami_id
  instance_type          = var.ec2_instance_type
  key_name               = var.ec2_key_name != "" ? var.ec2_key_name : null
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.ec2.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2.name

  root_block_device {
    volume_size           = 20
    volume_type           = "gp3"
    encrypted             = true
    delete_on_termination = true
  }

  user_data = base64encode(templatefile("${path.module}/user_data.sh.tpl", {
    environment       = var.environment
    region            = var.aws_region
    project_name      = var.project_name
    db_host           = aws_db_instance.postgres.address
    db_port           = aws_db_instance.postgres.port
    db_name           = var.db_name
    image_tag         = var.image_tag
    ghcr_owner        = var.ghcr_owner
    s3_config_bucket  = var.s3_config_bucket
    s3_storage_bucket = aws_s3_bucket.storage.id
  }))

  tags = { Name = "${local.name_prefix}-app" }

  lifecycle {
    ignore_changes = [ami, user_data]
  }
}

# ---- Elastic IP (dev/staging only — prod uses ALB DNS) ----
resource "aws_eip" "app" {
  count    = var.environment != "prod" ? 1 : 0
  instance = aws_instance.app[0].id
  domain   = "vpc"

  tags = { Name = "${local.name_prefix}-eip" }
}

# ---- CloudWatch Log Group ----
resource "aws_cloudwatch_log_group" "app" {
  name              = "/${var.project_name}/${var.environment}"
  retention_in_days = var.environment == "prod" ? 30 : 7

  tags = { Name = "${local.name_prefix}-logs" }
}
