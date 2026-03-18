# =============================================================================
# SYNCLEDGER - BOOTSTRAP
# Run this ONCE to create Terraform state backend (S3 + DynamoDB)
# Usage:
#   cd infra/bootstrap
#   terraform init
#   terraform apply
# =============================================================================

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  default = "us-east-1"
}

variable "project_name" {
  default = "syncledger"
}

# ---- S3 Bucket for Terraform State ----
resource "aws_s3_bucket" "terraform_state" {
  bucket = "${var.project_name}-terraform-state"

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    Name    = "${var.project_name}-terraform-state"
    Project = var.project_name
  }
}

resource "aws_s3_bucket_versioning" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "terraform_state" {
  bucket                  = aws_s3_bucket.terraform_state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# ---- DynamoDB Table for State Locking ----
resource "aws_dynamodb_table" "terraform_locks" {
  name         = "${var.project_name}-terraform-locks"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  tags = {
    Name    = "${var.project_name}-terraform-locks"
    Project = var.project_name
  }
}

# ---- S3 Bucket for Config Files (docker-compose per env) ----
resource "aws_s3_bucket" "config" {
  bucket = "${var.project_name}-config"

  tags = {
    Name    = "${var.project_name}-config"
    Project = var.project_name
  }
}

resource "aws_s3_bucket_versioning" "config" {
  bucket = aws_s3_bucket.config.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "config" {
  bucket                  = aws_s3_bucket.config.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# ---- Outputs ----
output "state_bucket" {
  value = aws_s3_bucket.terraform_state.id
}

output "lock_table" {
  value = aws_dynamodb_table.terraform_locks.name
}

output "config_bucket" {
  value = aws_s3_bucket.config.id
}

output "next_steps" {
  value = <<-EOT
    
    Bootstrap complete! Next steps:
    
    1. Note the config_bucket name: ${aws_s3_bucket.config.id}
    2. Set up GitHub repository secrets (see DEPLOYMENT.md)
    3. Push code to trigger the build workflow
    4. Run the deploy workflow for your environment
    
    State backend config for GitHub Actions:
      bucket         = "${aws_s3_bucket.terraform_state.id}"
      dynamodb_table = "${aws_dynamodb_table.terraform_locks.name}"
      region         = "${var.aws_region}"
      key            = "syncledger/<ENV>/terraform.tfstate"
    
  EOT
}
