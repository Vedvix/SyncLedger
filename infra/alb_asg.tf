# =============================================================================
# SYNCLEDGER - APPLICATION LOAD BALANCER + AUTO SCALING
# Only created for production environment
# Dev/staging use the standalone EC2 instance directly
# =============================================================================

# Migrate from split http/http_redirect listeners to single http listener
moved {
  from = aws_lb_listener.http_redirect[0]
  to   = aws_lb_listener.http[0]
}

# ---- ALB Security Group ----
resource "aws_security_group" "alb" {
  count       = var.environment == "prod" ? 1 : 0
  name        = "${local.name_prefix}-alb-sg"
  description = "Security group for Application Load Balancer"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-alb-sg" }
}

# ---- Application Load Balancer ----
resource "aws_lb" "app" {
  count              = var.environment == "prod" ? 1 : 0
  name               = "${local.name_prefix}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb[0].id]
  subnets            = aws_subnet.public[*].id

  tags = { Name = "${local.name_prefix}-alb" }
}

# ---- Target Group ----
resource "aws_lb_target_group" "app" {
  count       = var.environment == "prod" ? 1 : 0
  name        = "${local.name_prefix}-tg"
  port        = 80
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "instance"

  health_check {
    enabled             = true
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 10
    interval            = 30
    matcher             = "200"
  }

  stickiness {
    type            = "lb_cookie"
    cookie_duration = 86400
    enabled         = true
  }

  tags = { Name = "${local.name_prefix}-tg" }
}

# ---- HTTP Listener (port 80) ----
# Redirects to HTTPS when domain_name is set, otherwise forwards to target group
resource "aws_lb_listener" "http" {
  count             = var.environment == "prod" ? 1 : 0
  load_balancer_arn = aws_lb.app[0].arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = var.domain_name != "" ? "redirect" : "forward"
    target_group_arn = var.domain_name == "" ? aws_lb_target_group.app[0].arn : null

    dynamic "redirect" {
      for_each = var.domain_name != "" ? [1] : []
      content {
        port        = "443"
        protocol    = "HTTPS"
        status_code = "HTTP_301"
      }
    }
  }
}

# ---- Launch Template (prod only, replaces direct EC2 for ASG) ----
resource "aws_launch_template" "app" {
  count         = var.environment == "prod" ? 1 : 0
  name_prefix   = "${local.name_prefix}-lt-"
  image_id      = local.ami_id
  instance_type = var.ec2_instance_type
  key_name      = var.ec2_key_name != "" ? var.ec2_key_name : null

  iam_instance_profile {
    name = aws_iam_instance_profile.ec2.name
  }

  vpc_security_group_ids = [aws_security_group.ec2.id]

  block_device_mappings {
    device_name = "/dev/xvda"
    ebs {
      volume_size           = 20
      volume_type           = "gp3"
      encrypted             = true
      delete_on_termination = true
    }
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

  tag_specifications {
    resource_type = "instance"
    tags = {
      Name = "${local.name_prefix}-asg-instance"
    }
  }

  lifecycle {
    create_before_destroy = true
  }
}

# ---- Auto Scaling Group (prod only) ----
resource "aws_autoscaling_group" "app" {
  count               = var.environment == "prod" ? 1 : 0
  name                = "${local.name_prefix}-asg"
  desired_capacity    = var.asg_desired
  min_size            = var.asg_min
  max_size            = var.asg_max
  vpc_zone_identifier = aws_subnet.public[*].id
  target_group_arns   = [aws_lb_target_group.app[0].arn]
  health_check_type   = "ELB"

  # Wait for instances to pass ALB health check before marking healthy
  health_check_grace_period = 300

  launch_template {
    id      = aws_launch_template.app[0].id
    version = "$Latest"
  }

  instance_refresh {
    strategy = "Rolling"
    preferences {
      min_healthy_percentage = 50
    }
  }

  tag {
    key                 = "Name"
    value               = "${local.name_prefix}-asg-instance"
    propagate_at_launch = true
  }

  lifecycle {
    ignore_changes = [desired_capacity]
  }
}

# ---- Scaling Policies ----
resource "aws_autoscaling_policy" "scale_up" {
  count                  = var.environment == "prod" ? 1 : 0
  name                   = "${local.name_prefix}-scale-up"
  autoscaling_group_name = aws_autoscaling_group.app[0].name
  adjustment_type        = "ChangeInCapacity"
  scaling_adjustment     = 1
  cooldown               = 300
}

resource "aws_autoscaling_policy" "scale_down" {
  count                  = var.environment == "prod" ? 1 : 0
  name                   = "${local.name_prefix}-scale-down"
  autoscaling_group_name = aws_autoscaling_group.app[0].name
  adjustment_type        = "ChangeInCapacity"
  scaling_adjustment     = -1
  cooldown               = 300
}

# ---- CloudWatch Alarms for Scaling ----
resource "aws_cloudwatch_metric_alarm" "high_cpu" {
  count               = var.environment == "prod" ? 1 : 0
  alarm_name          = "${local.name_prefix}-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 120
  statistic           = "Average"
  threshold           = 70
  alarm_description   = "Scale up when CPU > 70% for 4 minutes"
  alarm_actions       = [aws_autoscaling_policy.scale_up[0].arn]

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.app[0].name
  }
}

resource "aws_cloudwatch_metric_alarm" "low_cpu" {
  count               = var.environment == "prod" ? 1 : 0
  alarm_name          = "${local.name_prefix}-low-cpu"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 3
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 120
  statistic           = "Average"
  threshold           = 25
  alarm_description   = "Scale down when CPU < 25% for 6 minutes"
  alarm_actions       = [aws_autoscaling_policy.scale_down[0].arn]

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.app[0].name
  }
}
