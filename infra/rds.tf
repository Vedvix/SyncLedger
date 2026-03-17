# =============================================================================
# SYNCLEDGER - RDS POSTGRESQL
# db.t4g.micro = free-tier eligible (~$12/mo after free tier)
# =============================================================================

resource "aws_db_subnet_group" "main" {
  name       = "${local.name_prefix}-db-subnet"
  subnet_ids = aws_subnet.private[*].id

  tags = { Name = "${local.name_prefix}-db-subnet" }
}

resource "aws_db_instance" "postgres" {
  identifier = "${local.name_prefix}-postgres"

  engine         = "postgres"
  engine_version = "16.4"
  instance_class = var.db_instance_type

  allocated_storage     = 20
  max_allocated_storage = 50 # auto-scale up to 50GB
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  # Cost optimization
  publicly_accessible = var.environment == "dev" ? true : false
  multi_az            = false # single-AZ saves ~50%

  # Backup
  backup_retention_period = var.environment == "prod" ? 7 : 1
  backup_window           = "03:00-04:00"
  maintenance_window      = "sun:04:00-sun:05:00"

  # Lifecycle
  skip_final_snapshot       = var.environment != "prod"
  final_snapshot_identifier = var.environment == "prod" ? "${local.name_prefix}-final-snapshot" : null
  deletion_protection       = var.environment == "prod" ? true : false

  # Performance Insights (free tier includes basic)
  performance_insights_enabled = true

  tags = { Name = "${local.name_prefix}-postgres" }
}
