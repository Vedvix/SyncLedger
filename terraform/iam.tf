# =============================================================================
# SYNCLEDGER - IAM NOTES
# =============================================================================
# The deployer IAM user + custom policy are created BEFORE Terraform via:
#   scripts/create-aws-user.ps1  (Windows)
#   scripts/create-aws-user.sh   (Linux/Mac)
#
# This avoids the chicken-and-egg problem: you need AWS credentials to run
# Terraform, but Terraform would be the one creating those credentials.
#
# The EC2 instance role & profile (for S3 access from the app) are managed
# in main.tf (aws_iam_role.ec2_role, aws_iam_instance_profile.ec2_profile).
# =============================================================================
