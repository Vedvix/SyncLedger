# =============================================================================
# SYNCLEDGER - SECRETS MANAGER
# Stores application config + GHCR credentials
# Cost: ~$0.40/secret/month
# =============================================================================

# ---- Application Configuration Secret ----
resource "aws_secretsmanager_secret" "app_config" {
  name                    = "${var.project_name}/${var.environment}/app-config"
  description             = "SyncLedger ${var.environment} application configuration"
  recovery_window_in_days = var.environment == "prod" ? 7 : 0
}

resource "aws_secretsmanager_secret_version" "app_config" {
  secret_id = aws_secretsmanager_secret.app_config.id
  secret_string = jsonencode({
    DB_HOST               = aws_db_instance.postgres.address
    DB_PORT               = tostring(aws_db_instance.postgres.port)
    DB_NAME               = var.db_name
    DB_USERNAME           = var.db_username
    DB_PASSWORD           = var.db_password
    JWT_SECRET            = var.jwt_secret
    OPENAI_API_KEY        = var.openai_api_key
    AWS_REGION            = var.aws_region
    S3_BUCKET_NAME        = aws_s3_bucket.storage.id
    ENVIRONMENT           = var.environment
    EMAIL_POLLING_ENABLED = tostring(var.email_polling_enabled)
    AZURE_CLIENT_ID       = var.azure_client_id
    AZURE_CLIENT_SECRET   = var.azure_client_secret
    AZURE_TENANT_ID       = var.azure_tenant_id
    CORS_ALLOWED_ORIGINS  = local.cors_origins
    ENCRYPTION_MASTER_KEY = var.encryption_master_key
  })
}

# ---- GHCR Credentials Secret ----
# This is managed externally via GitHub Actions (upserted during deploy)
resource "aws_secretsmanager_secret" "ghcr" {
  name                    = "${var.project_name}/${var.environment}/ghcr"
  description             = "GitHub Container Registry credentials for ${var.environment}"
  recovery_window_in_days = 0
}
