# SyncLedger - Production Environment
# Branch: main
# Cost: ~$25-35/mo

environment       = "prod"
ec2_instance_type = "t3.small"
db_instance_type  = "db.t4g.micro"
enable_ssh        = false

email_polling_enabled = true
# domain_name         = "app.syncledger.com"
