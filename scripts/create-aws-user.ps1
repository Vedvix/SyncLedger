# =============================================================================
# SyncLedger - IAM User Bootstrap Script (PowerShell)
# =============================================================================
# Run this ONCE with your AWS root/admin credentials to create the
# dedicated project user. Then use its access keys for Terraform & GitHub Actions.
#
# Prerequisites:
#   1. AWS CLI v2 installed: https://aws.amazon.com/cli/
#   2. AWS CLI configured with admin/root credentials: aws configure
#
# Usage:
#   .\scripts\create-aws-user.ps1
#   .\scripts\create-aws-user.ps1 -Region "us-west-2"
#   .\scripts\create-aws-user.ps1 -Environment "staging"
# =============================================================================

param(
    [string]$Region = "us-east-1",
    [string]$Environment = "prod",
    [string]$ProjectName = "syncledger"
)

$ErrorActionPreference = "Stop"

$UserName = "$ProjectName-$Environment-deployer"
$PolicyName = "$ProjectName-$Environment-deployer-policy"
$IamPath = "/$ProjectName/"

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  SyncLedger - AWS IAM User Bootstrap" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  User:        $UserName" -ForegroundColor Yellow
Write-Host "  Policy:      $PolicyName" -ForegroundColor Yellow
Write-Host "  Region:      $Region" -ForegroundColor Yellow
Write-Host "  Environment: $Environment" -ForegroundColor Yellow
Write-Host ""

# ---- Verify AWS CLI is configured ----
Write-Host "[1/6] Verifying AWS credentials..." -ForegroundColor Green
try {
    $callerIdentity = aws sts get-caller-identity --output json 2>&1 | ConvertFrom-Json
    $AccountId = $callerIdentity.Account
    Write-Host "  Account: $AccountId" -ForegroundColor Gray
    Write-Host "  Caller:  $($callerIdentity.Arn)" -ForegroundColor Gray
} catch {
    Write-Host "  ERROR: AWS CLI not configured. Run 'aws configure' first." -ForegroundColor Red
    exit 1
}

# ---- Check if user already exists ----
Write-Host ""
Write-Host "[2/6] Checking if user already exists..." -ForegroundColor Green
$existingUser = aws iam get-user --user-name $UserName 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "  User '$UserName' already exists." -ForegroundColor Yellow
    $confirm = Read-Host "  Do you want to create NEW access keys? (y/N)"
    if ($confirm -ne 'y') {
        Write-Host "  Aborted." -ForegroundColor Red
        exit 0
    }
    $userExists = $true
} else {
    $userExists = $false
}

# ---- Create the IAM policy document ----
Write-Host ""
Write-Host "[3/6] Creating custom IAM policy..." -ForegroundColor Green

$policyDocument = @"
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "EC2ManageInstances",
            "Effect": "Allow",
            "Action": [
                "ec2:RunInstances",
                "ec2:TerminateInstances",
                "ec2:StartInstances",
                "ec2:StopInstances",
                "ec2:RebootInstances",
                "ec2:DescribeInstances",
                "ec2:DescribeInstanceStatus",
                "ec2:DescribeImages",
                "ec2:DescribeKeyPairs",
                "ec2:DescribeAvailabilityZones",
                "ec2:DescribeAccountAttributes",
                "ec2:CreateTags",
                "ec2:DeleteTags",
                "ec2:ModifyInstanceAttribute"
            ],
            "Resource": "*",
            "Condition": {
                "StringEquals": {
                    "aws:RequestedRegion": "$Region"
                }
            }
        },
        {
            "Sid": "EC2SecurityGroups",
            "Effect": "Allow",
            "Action": [
                "ec2:CreateSecurityGroup",
                "ec2:DeleteSecurityGroup",
                "ec2:DescribeSecurityGroups",
                "ec2:DescribeSecurityGroupRules",
                "ec2:AuthorizeSecurityGroupIngress",
                "ec2:AuthorizeSecurityGroupEgress",
                "ec2:RevokeSecurityGroupIngress",
                "ec2:RevokeSecurityGroupEgress"
            ],
            "Resource": "*"
        },
        {
            "Sid": "EC2ElasticIPs",
            "Effect": "Allow",
            "Action": [
                "ec2:AllocateAddress",
                "ec2:ReleaseAddress",
                "ec2:AssociateAddress",
                "ec2:DisassociateAddress",
                "ec2:DescribeAddresses",
                "ec2:DescribeAddressesAttribute"
            ],
            "Resource": "*"
        },
        {
            "Sid": "EC2VPCReadOnly",
            "Effect": "Allow",
            "Action": [
                "ec2:DescribeVpcs",
                "ec2:DescribeSubnets",
                "ec2:DescribeRouteTables",
                "ec2:DescribeInternetGateways",
                "ec2:DescribeNetworkInterfaces",
                "ec2:DescribeVolumes",
                "ec2:DescribeNetworkAcls",
                "ec2:DescribePrefixLists"
            ],
            "Resource": "*"
        },
        {
            "Sid": "S3ManageBucket",
            "Effect": "Allow",
            "Action": [
                "s3:CreateBucket",
                "s3:DeleteBucket",
                "s3:ListBucket",
                "s3:ListAllMyBuckets",
                "s3:GetBucketLocation",
                "s3:GetBucketVersioning",
                "s3:PutBucketVersioning",
                "s3:GetBucketEncryption",
                "s3:PutBucketEncryption",
                "s3:GetBucketPublicAccessBlock",
                "s3:PutBucketPublicAccessBlock",
                "s3:GetLifecycleConfiguration",
                "s3:PutLifecycleConfiguration",
                "s3:GetBucketPolicy",
                "s3:PutBucketPolicy",
                "s3:DeleteBucketPolicy",
                "s3:GetBucketTagging",
                "s3:PutBucketTagging",
                "s3:GetBucketAcl",
                "s3:PutBucketAcl",
                "s3:GetAccelerateConfiguration",
                "s3:GetBucketCORS",
                "s3:GetBucketLogging",
                "s3:GetBucketObjectLockConfiguration",
                "s3:GetBucketRequestPayment",
                "s3:GetBucketWebsite",
                "s3:GetReplicationConfiguration"
            ],
            "Resource": "arn:aws:s3:::${ProjectName}-*"
        },
        {
            "Sid": "S3ManageObjects",
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:PutObject",
                "s3:DeleteObject",
                "s3:ListBucketVersions",
                "s3:GetObjectVersion",
                "s3:DeleteObjectVersion"
            ],
            "Resource": "arn:aws:s3:::${ProjectName}-*/*"
        },
        {
            "Sid": "S3TerraformState",
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:PutObject",
                "s3:DeleteObject",
                "s3:ListBucket",
                "s3:GetBucketVersioning",
                "s3:GetBucketLocation"
            ],
            "Resource": [
                "arn:aws:s3:::${ProjectName}-terraform-state",
                "arn:aws:s3:::${ProjectName}-terraform-state/*"
            ]
        },
        {
            "Sid": "DynamoDBTerraformLocks",
            "Effect": "Allow",
            "Action": [
                "dynamodb:GetItem",
                "dynamodb:PutItem",
                "dynamodb:DeleteItem",
                "dynamodb:DescribeTable"
            ],
            "Resource": "arn:aws:dynamodb:${Region}:${AccountId}:table/${ProjectName}-terraform-locks"
        },
        {
            "Sid": "IAMManageProjectRoles",
            "Effect": "Allow",
            "Action": [
                "iam:CreateRole",
                "iam:DeleteRole",
                "iam:GetRole",
                "iam:UpdateRole",
                "iam:PutRolePolicy",
                "iam:DeleteRolePolicy",
                "iam:GetRolePolicy",
                "iam:ListRolePolicies",
                "iam:ListAttachedRolePolicies",
                "iam:AttachRolePolicy",
                "iam:DetachRolePolicy",
                "iam:TagRole",
                "iam:UntagRole",
                "iam:ListInstanceProfilesForRole",
                "iam:PassRole"
            ],
            "Resource": "arn:aws:iam::${AccountId}:role/${ProjectName}-*"
        },
        {
            "Sid": "IAMManageInstanceProfiles",
            "Effect": "Allow",
            "Action": [
                "iam:CreateInstanceProfile",
                "iam:DeleteInstanceProfile",
                "iam:GetInstanceProfile",
                "iam:AddRoleToInstanceProfile",
                "iam:RemoveRoleFromInstanceProfile",
                "iam:ListInstanceProfiles",
                "iam:TagInstanceProfile",
                "iam:UntagInstanceProfile"
            ],
            "Resource": "arn:aws:iam::${AccountId}:instance-profile/${ProjectName}-*"
        },
        {
            "Sid": "CloudWatchLogs",
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogGroup",
                "logs:DeleteLogGroup",
                "logs:DescribeLogGroups",
                "logs:PutRetentionPolicy",
                "logs:TagLogGroup",
                "logs:ListTagsLogGroup",
                "logs:ListTagsForResource",
                "logs:TagResource",
                "logs:UntagResource"
            ],
            "Resource": [
                "arn:aws:logs:${Region}:${AccountId}:log-group:/syncledger/*",
                "arn:aws:logs:${Region}:${AccountId}:log-group:/syncledger/*:*"
            ]
        },
        {
            "Sid": "SSMParameters",
            "Effect": "Allow",
            "Action": [
                "ssm:PutParameter",
                "ssm:GetParameter",
                "ssm:GetParameters",
                "ssm:DeleteParameter",
                "ssm:DescribeParameters",
                "ssm:GetParametersByPath",
                "ssm:AddTagsToResource",
                "ssm:RemoveTagsFromResource",
                "ssm:ListTagsForResource"
            ],
            "Resource": "arn:aws:ssm:${Region}:${AccountId}:parameter/${ProjectName}/*"
        },
        {
            "Sid": "SSMSendCommand",
            "Effect": "Allow",
            "Action": [
                "ssm:SendCommand",
                "ssm:GetCommandInvocation",
                "ssm:ListCommandInvocations"
            ],
            "Resource": "*"
        },
        {
            "Sid": "EC2InstanceConnect",
            "Effect": "Allow",
            "Action": [
                "ec2-instance-connect:SendSSHPublicKey"
            ],
            "Resource": "arn:aws:ec2:${Region}:${AccountId}:instance/*",
            "Condition": {
                "StringEquals": {
                    "ec2:ResourceTag/Project": "${ProjectName}"
                }
            }
        },
        {
            "Sid": "STSGetCallerIdentity",
            "Effect": "Allow",
            "Action": [
                "sts:GetCallerIdentity"
            ],
            "Resource": "*"
        }
    ]
}
"@

# Save policy to temp file
$policyFile = [System.IO.Path]::GetTempFileName()
$policyDocument | Out-File -FilePath $policyFile -Encoding utf8 -NoNewline

# Create or update policy
$existingPolicy = aws iam list-policies --path-prefix $IamPath --query "Policies[?PolicyName=='$PolicyName'].Arn" --output text 2>&1
if ($existingPolicy -and $existingPolicy -ne "" -and $LASTEXITCODE -eq 0) {
    Write-Host "  Policy exists. Creating new version..." -ForegroundColor Yellow
    # Delete oldest non-default version if we're at the limit
    $versions = aws iam list-policy-versions --policy-arn $existingPolicy --query "Versions[?!IsDefaultVersion].VersionId" --output text 2>&1
    if ($versions -and $versions -ne "") {
        $oldestVersion = ($versions -split "`t")[-1]
        aws iam delete-policy-version --policy-arn $existingPolicy --version-id $oldestVersion 2>&1 | Out-Null
    }
    aws iam create-policy-version --policy-arn $existingPolicy --policy-document "file://$policyFile" --set-as-default | Out-Null
    $PolicyArn = $existingPolicy
} else {
    $policyResult = aws iam create-policy --policy-name $PolicyName --path $IamPath --policy-document "file://$policyFile" --description "Custom least-privilege policy for SyncLedger project - scoped to project resources only" --output json 2>&1 | ConvertFrom-Json
    $PolicyArn = $policyResult.Policy.Arn
}
Remove-Item $policyFile -Force
Write-Host "  Policy ARN: $PolicyArn" -ForegroundColor Gray

# ---- Create IAM User ----
Write-Host ""
Write-Host "[4/6] Creating IAM user..." -ForegroundColor Green
if (-not $userExists) {
    aws iam create-user `
        --user-name $UserName `
        --path $IamPath `
        --tags "Key=Project,Value=$ProjectName" "Key=Environment,Value=$Environment" "Key=Purpose,Value=CI/CD Deployments" `
        --output json | Out-Null
    Write-Host "  User created: $UserName" -ForegroundColor Gray
} else {
    Write-Host "  User already exists, skipping creation." -ForegroundColor Yellow
}

# ---- Attach Policy ----
Write-Host ""
Write-Host "[5/6] Attaching policy to user..." -ForegroundColor Green
aws iam attach-user-policy --user-name $UserName --policy-arn $PolicyArn
Write-Host "  Policy attached." -ForegroundColor Gray

# ---- Create Access Keys ----
Write-Host ""
Write-Host "[6/6] Creating access keys..." -ForegroundColor Green
$keyResult = aws iam create-access-key --user-name $UserName --output json 2>&1 | ConvertFrom-Json
$AccessKeyId = $keyResult.AccessKey.AccessKeyId
$SecretAccessKey = $keyResult.AccessKey.SecretAccessKey

# ---- Output ----
Write-Host ""
Write-Host "==============================================================" -ForegroundColor Green
Write-Host "  SUCCESS! IAM User Created" -ForegroundColor Green
Write-Host "==============================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  IAM User:     $UserName" -ForegroundColor White
Write-Host "  IAM Path:     $IamPath" -ForegroundColor White
Write-Host "  Policy:       $PolicyName" -ForegroundColor White
Write-Host ""
Write-Host "  ---- SAVE THESE CREDENTIALS (shown only once!) ----" -ForegroundColor Red
Write-Host ""
Write-Host "  AWS_ACCESS_KEY_ID:     $AccessKeyId" -ForegroundColor Yellow
Write-Host "  AWS_SECRET_ACCESS_KEY: $SecretAccessKey" -ForegroundColor Yellow
Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  NEXT STEPS" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  1. Add to GitHub Secrets (Repo > Settings > Secrets > Actions):" -ForegroundColor White
Write-Host ""
Write-Host "     AWS_ACCESS_KEY_ID      = $AccessKeyId" -ForegroundColor Gray
Write-Host "     AWS_SECRET_ACCESS_KEY  = $SecretAccessKey" -ForegroundColor Gray
Write-Host "     DB_PASSWORD            = <your-database-password>" -ForegroundColor Gray
Write-Host "     JWT_SECRET             = <your-jwt-secret>" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. For local Terraform, set environment variables:" -ForegroundColor White
Write-Host ""
Write-Host '     $env:AWS_ACCESS_KEY_ID = "' -NoNewline -ForegroundColor Gray
Write-Host $AccessKeyId -NoNewline -ForegroundColor Yellow
Write-Host '"' -ForegroundColor Gray
Write-Host '     $env:AWS_SECRET_ACCESS_KEY = "' -NoNewline -ForegroundColor Gray
Write-Host $SecretAccessKey -NoNewline -ForegroundColor Yellow
Write-Host '"' -ForegroundColor Gray
Write-Host '     $env:AWS_REGION = "' -NoNewline -ForegroundColor Gray
Write-Host $Region -NoNewline -ForegroundColor Yellow
Write-Host '"' -ForegroundColor Gray
Write-Host ""
Write-Host "  3. Run Terraform:" -ForegroundColor White
Write-Host ""
Write-Host "     cd terraform" -ForegroundColor Gray
Write-Host "     terraform init" -ForegroundColor Gray
Write-Host "     terraform plan -var=`"db_password=YOUR_PASSWORD`" -var=`"jwt_secret=YOUR_SECRET`"" -ForegroundColor Gray
Write-Host "     terraform apply -var=`"db_password=YOUR_PASSWORD`" -var=`"jwt_secret=YOUR_SECRET`"" -ForegroundColor Gray
Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  POLICY SCOPE (what this user CAN access):" -ForegroundColor White
Write-Host "    - EC2 instances       (region: $Region only)" -ForegroundColor Gray
Write-Host "    - Security groups     (default VPC)" -ForegroundColor Gray
Write-Host "    - Elastic IPs         (region: $Region)" -ForegroundColor Gray
Write-Host "    - S3 buckets          (prefix: $ProjectName-* only)" -ForegroundColor Gray
Write-Host "    - IAM roles           (prefix: $ProjectName-* only)" -ForegroundColor Gray
Write-Host "    - CloudWatch logs     (prefix: /syncledger/* only)" -ForegroundColor Gray
Write-Host "    - SSM parameters      (prefix: /$ProjectName/* only)" -ForegroundColor Gray
Write-Host "    - DynamoDB            (table: $ProjectName-terraform-locks only)" -ForegroundColor Gray
Write-Host ""
Write-Host "  CANNOT access:" -ForegroundColor White
Write-Host "    - Other projects' resources" -ForegroundColor Gray
Write-Host "    - IAM users or policies" -ForegroundColor Gray
Write-Host "    - Billing/account settings" -ForegroundColor Gray
Write-Host "    - Resources outside $Region" -ForegroundColor Gray
Write-Host ""
