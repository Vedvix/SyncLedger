# =============================================================================
# SYNCLEDGER - TERRAFORM BOOTSTRAP
# One-time setup: creates the initial IAM admin user and Terraform state bucket
# Run this FIRST with an AWS root/admin account, then use the deployer for everything else
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

# =============================================================================
# S3 Bucket for Terraform State (Optional - for remote state)
# =============================================================================

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

# =============================================================================
# DynamoDB for Terraform State Locking
# =============================================================================

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

# =============================================================================
# OUTPUTS
# =============================================================================

output "state_bucket" {
  value = aws_s3_bucket.terraform_state.id
}

output "lock_table" {
  value = aws_dynamodb_table.terraform_locks.name
}

output "next_steps" {
  value = <<-EOT
    
    ✅ Bootstrap complete! Next steps:
    
    1. Uncomment the backend "s3" block in terraform/versions.tf
    2. Update the bucket name to: ${aws_s3_bucket.terraform_state.id}
    3. Run: cd ../terraform && terraform init
    4. Create terraform.tfvars with your DB password and JWT secret
    5. Run: terraform plan
    6. Run: terraform apply
    
    The deployer IAM user credentials will be in the Terraform output.
    Add them to your GitHub repository secrets.
  EOT
}
