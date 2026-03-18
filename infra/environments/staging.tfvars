# SyncLedger - Staging Environment
# Branch: release/*
# Cost: ~$25-30/mo

environment       = "staging"
ec2_instance_type = "t3.small"
db_instance_type  = "db.t4g.micro"
enable_ssh        = true
# my_ip           = "YOUR_IP/32"

email_polling_enabled = false

# Domain DNS managed in Squarespace (CNAME → EIP)
# domain_name = "api.sandbox.viaflo.ai"
