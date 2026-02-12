# =============================================================================
# SYNCLEDGER - TERRAFORM PROVIDER CONFIGURATION
# =============================================================================

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Remote state in S3 - uncomment after initial bootstrap
  # backend "s3" {
  #   bucket         = "syncledger-terraform-state"
  #   key            = "infra/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "syncledger-terraform-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "SyncLedger"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Owner       = "vedvix"
    }
  }
}
