# =============================================================================
# SYNCLEDGER - SECURITY GROUPS
# =============================================================================

# ---- EC2 Security Group ----
resource "aws_security_group" "ec2" {
  name        = "${local.name_prefix}-ec2-sg"
  description = "Security group for EC2 application instances"
  vpc_id      = aws_vpc.main.id

  # All outbound
  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-ec2-sg" }
}

# HTTP/HTTPS directly to EC2 (dev/staging only — prod goes through ALB)
resource "aws_security_group_rule" "ec2_http_direct" {
  count             = var.environment != "prod" ? 1 : 0
  type              = "ingress"
  description       = "HTTP direct access (non-prod)"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.ec2.id
}

resource "aws_security_group_rule" "ec2_https_direct" {
  count             = var.environment != "prod" ? 1 : 0
  type              = "ingress"
  description       = "HTTPS direct access (non-prod)"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.ec2.id
}

# HTTP from ALB only (prod — EC2 sits behind ALB)
resource "aws_security_group_rule" "ec2_http_from_alb" {
  count                    = var.environment == "prod" ? 1 : 0
  type                     = "ingress"
  description              = "HTTP from ALB only (prod)"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.alb[0].id
  security_group_id        = aws_security_group.ec2.id
}

# SSH access (optional, non-prod only)
resource "aws_security_group_rule" "ec2_ssh" {
  count             = var.enable_ssh && var.environment != "prod" ? 1 : 0
  type              = "ingress"
  description       = "SSH access for debugging (non-prod only)"
  from_port         = 22
  to_port           = 22
  protocol          = "tcp"
  cidr_blocks       = [var.my_ip]
  security_group_id = aws_security_group.ec2.id
}

# ---- RDS Security Group ----
resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds-sg"
  description = "Security group for RDS PostgreSQL"
  vpc_id      = aws_vpc.main.id

  # PostgreSQL from EC2 only
  ingress {
    description     = "PostgreSQL from EC2"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-rds-sg" }
}

# Direct RDS access for debugging (dev only)
resource "aws_security_group_rule" "rds_debug" {
  count             = var.environment == "dev" ? 1 : 0
  type              = "ingress"
  description       = "PostgreSQL debug access (dev only)"
  from_port         = 5432
  to_port           = 5432
  protocol          = "tcp"
  cidr_blocks       = [var.my_ip]
  security_group_id = aws_security_group.rds.id
}
