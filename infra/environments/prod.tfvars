# SyncLedger - Production Environment
# Branch: main
# Uses ALB + Auto Scaling Group
# Cost: ~$43-58/mo (single instance), ~$59-100/mo (scaled)

environment       = "prod"
ec2_instance_type = "t3.small"
db_instance_type  = "db.t4g.micro"
enable_ssh        = false

email_polling_enabled = true
# domain_name         = "app.syncledger.com"

# Auto Scaling
asg_min     = 1
asg_max     = 3
asg_desired = 1
