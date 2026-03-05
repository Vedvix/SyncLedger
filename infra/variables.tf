# =============================================================================
# SYNCLEDGER - VARIABLES
# =============================================================================

# ---- General ----
variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "syncledger"
}

# ---- EC2 ----
variable "ec2_instance_type" {
  description = "EC2 instance type. t3.micro = free-tier eligible"
  type        = string
  default     = "t3.micro"
}

variable "ec2_key_name" {
  description = "SSH key pair name (optional, use SSM Session Manager instead)"
  type        = string
  default     = ""
}

variable "enable_ssh" {
  description = "Enable SSH access (non-prod only). Prefer SSM Session Manager."
  type        = bool
  default     = false
}

variable "my_ip" {
  description = "Your IP in CIDR notation for SSH/RDS debug access (e.g., 1.2.3.4/32)"
  type        = string
  default     = "0.0.0.0/0"
}

# ---- RDS ----
variable "db_instance_type" {
  description = "RDS instance type. db.t4g.micro = free-tier eligible"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "syncledger"
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
  default     = "syncledger"
}

variable "db_password" {
  description = "PostgreSQL master password"
  type        = string
  sensitive   = true
}

# ---- Application Secrets ----
variable "jwt_secret" {
  description = "JWT signing secret (min 256 bits)"
  type        = string
  sensitive   = true
}

variable "openai_api_key" {
  description = "OpenAI API key for PDF extraction"
  type        = string
  sensitive   = true
  default     = ""
}

# ---- Docker Image ----
variable "image_tag" {
  description = "Docker image tag to deploy (e.g., latest, sha-abc1234)"
  type        = string
  default     = "latest"
}

variable "ghcr_owner" {
  description = "GitHub Container Registry owner (github username or org)"
  type        = string
  default     = "vedvix"
}

# ---- S3 Config ----
variable "s3_config_bucket" {
  description = "S3 bucket containing docker-compose files (created by bootstrap)"
  type        = string
}

# ---- Networking ----
variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
  default     = ["10.0.11.0/24", "10.0.12.0/24"]
}

# ---- Optional ----
variable "domain_name" {
  description = "Custom domain name (optional). Leave empty to use EC2 public IP."
  type        = string
  default     = ""
}

variable "email_polling_enabled" {
  description = "Enable email polling for invoice ingestion"
  type        = bool
  default     = false
}

variable "azure_client_id" {
  description = "Azure AD client ID for Outlook integration"
  type        = string
  default     = ""
}

variable "azure_client_secret" {
  description = "Azure AD client secret"
  type        = string
  sensitive   = true
  default     = ""
}

variable "azure_tenant_id" {
  description = "Azure AD tenant ID"
  type        = string
  default     = ""
}
