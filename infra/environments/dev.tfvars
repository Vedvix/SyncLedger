# SyncLedger - Dev Environment
# Branch: develop
# Cost: ~$5-10/mo with free tier

environment       = "dev"
ec2_instance_type = "t3.micro"
db_instance_type  = "db.t4g.micro"
enable_ssh        = true
# my_ip           = "YOUR_IP/32"

email_polling_enabled = false

# ---- Domain ----
domain_name = "api.dev.viaflo.ai"
# route53_zone_id = ""   # set after running bootstrap with domain_name=viaflo.ai
