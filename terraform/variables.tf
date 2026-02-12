# =============================================================================
# SYNCLEDGER - TERRAFORM VARIABLES
# =============================================================================

variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "syncledger"
}

# ------------------------------------------------------------------------------
# EC2 Configuration
# ------------------------------------------------------------------------------
variable "instance_type" {
  description = "EC2 instance type - t3.micro is free-tier eligible"
  type        = string
  default     = "t3.small"
}

variable "ec2_key_name" {
  description = "Name of the EC2 key pair for SSH access"
  type        = string
  default     = ""
}

# ------------------------------------------------------------------------------
# Database Configuration
# ------------------------------------------------------------------------------
variable "db_password" {
  description = "PostgreSQL database password"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT signing secret (min 256 bits)"
  type        = string
  sensitive   = true
}

# ------------------------------------------------------------------------------
# Domain & Networking
# ------------------------------------------------------------------------------
variable "domain_name" {
  description = "Custom domain name (optional). Leave empty to use EC2 public IP"
  type        = string
  default     = ""
}

variable "allowed_ssh_cidrs" {
  description = "CIDR blocks allowed for SSH access"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# ------------------------------------------------------------------------------
# Application Configuration
# ------------------------------------------------------------------------------
variable "storage_type" {
  description = "Storage type: 'local' or 's3'"
  type        = string
  default     = "s3"
}

variable "email_polling_enabled" {
  description = "Enable email polling from Outlook"
  type        = bool
  default     = false
}
