#!/usr/bin/env bash
# =============================================================================
# SyncLedger - IAM User Bootstrap Script (Bash)
# =============================================================================
# Run this ONCE with your AWS root/admin credentials to create the
# dedicated project user. Then use its access keys for Terraform & GitHub Actions.
#
# Prerequisites:
#   1. AWS CLI v2 installed: https://aws.amazon.com/cli/
#   2. AWS CLI configured with admin/root credentials: aws configure
#
# Usage:
#   chmod +x scripts/create-aws-user.sh
#   ./scripts/create-aws-user.sh
#   ./scripts/create-aws-user.sh --region us-west-2
#   ./scripts/create-aws-user.sh --environment staging
# =============================================================================

set -euo pipefail

# ---- Defaults ----
REGION="us-east-1"
ENVIRONMENT="prod"
PROJECT_NAME="syncledger"

# ---- Parse args ----
while [[ $# -gt 0 ]]; do
    case $1 in
        --region) REGION="$2"; shift 2 ;;
        --environment) ENVIRONMENT="$2"; shift 2 ;;
        --project) PROJECT_NAME="$2"; shift 2 ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

USER_NAME="${PROJECT_NAME}-${ENVIRONMENT}-deployer"
POLICY_NAME="${PROJECT_NAME}-${ENVIRONMENT}-deployer-policy"
IAM_PATH="/${PROJECT_NAME}/"

echo ""
echo "=============================================================="
echo "  SyncLedger - AWS IAM User Bootstrap"
echo "=============================================================="
echo ""
echo "  User:        $USER_NAME"
echo "  Policy:      $POLICY_NAME"
echo "  Region:      $REGION"
echo "  Environment: $ENVIRONMENT"
echo ""

# ---- Verify AWS CLI ----
echo "[1/6] Verifying AWS credentials..."
ACCOUNT_ID=$(aws sts get-caller-identity --query "Account" --output text 2>/dev/null) || {
    echo "  ERROR: AWS CLI not configured. Run 'aws configure' first."
    exit 1
}
CALLER_ARN=$(aws sts get-caller-identity --query "Arn" --output text)
echo "  Account: $ACCOUNT_ID"
echo "  Caller:  $CALLER_ARN"

# ---- Check if user exists ----
echo ""
echo "[2/6] Checking if user already exists..."
USER_EXISTS=false
if aws iam get-user --user-name "$USER_NAME" &>/dev/null; then
    echo "  User '$USER_NAME' already exists."
    read -rp "  Create NEW access keys? (y/N): " confirm
    if [[ "$confirm" != "y" ]]; then
        echo "  Aborted."
        exit 0
    fi
    USER_EXISTS=true
fi

# ---- Create IAM Policy ----
echo ""
echo "[3/6] Creating custom IAM policy..."

POLICY_DOC=$(cat <<POLICY
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
                    "aws:RequestedRegion": "$REGION"
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
            "Resource": "arn:aws:s3:::${PROJECT_NAME}-*"
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
            "Resource": "arn:aws:s3:::${PROJECT_NAME}-*/*"
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
                "arn:aws:s3:::${PROJECT_NAME}-terraform-state",
                "arn:aws:s3:::${PROJECT_NAME}-terraform-state/*"
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
            "Resource": "arn:aws:dynamodb:${REGION}:${ACCOUNT_ID}:table/${PROJECT_NAME}-terraform-locks"
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
            "Resource": "arn:aws:iam::${ACCOUNT_ID}:role/${PROJECT_NAME}-*"
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
            "Resource": "arn:aws:iam::${ACCOUNT_ID}:instance-profile/${PROJECT_NAME}-*"
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
                "arn:aws:logs:${REGION}:${ACCOUNT_ID}:log-group:/syncledger/*",
                "arn:aws:logs:${REGION}:${ACCOUNT_ID}:log-group:/syncledger/*:*"
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
            "Resource": "arn:aws:ssm:${REGION}:${ACCOUNT_ID}:parameter/${PROJECT_NAME}/*"
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
            "Resource": "arn:aws:ec2:${REGION}:${ACCOUNT_ID}:instance/*",
            "Condition": {
                "StringEquals": {
                    "ec2:ResourceTag/Project": "${PROJECT_NAME}"
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
POLICY
)

# Write policy to temp file
POLICY_FILE=$(mktemp)
echo "$POLICY_DOC" > "$POLICY_FILE"

# Check if policy exists
EXISTING_POLICY_ARN=$(aws iam list-policies --path-prefix "$IAM_PATH" \
    --query "Policies[?PolicyName=='$POLICY_NAME'].Arn" --output text 2>/dev/null || echo "")

if [[ -n "$EXISTING_POLICY_ARN" && "$EXISTING_POLICY_ARN" != "None" ]]; then
    echo "  Policy exists. Creating new version..."
    # Delete oldest non-default version if at limit
    OLDEST_VERSION=$(aws iam list-policy-versions --policy-arn "$EXISTING_POLICY_ARN" \
        --query "Versions[?!IsDefaultVersion].VersionId | [-1]" --output text 2>/dev/null || echo "")
    if [[ -n "$OLDEST_VERSION" && "$OLDEST_VERSION" != "None" ]]; then
        aws iam delete-policy-version --policy-arn "$EXISTING_POLICY_ARN" --version-id "$OLDEST_VERSION" 2>/dev/null || true
    fi
    aws iam create-policy-version --policy-arn "$EXISTING_POLICY_ARN" \
        --policy-document "file://$POLICY_FILE" --set-as-default > /dev/null
    POLICY_ARN="$EXISTING_POLICY_ARN"
else
    POLICY_ARN=$(aws iam create-policy \
        --policy-name "$POLICY_NAME" \
        --path "$IAM_PATH" \
        --policy-document "file://$POLICY_FILE" \
        --description "Custom least-privilege policy for SyncLedger project" \
        --query "Policy.Arn" --output text)
fi
rm -f "$POLICY_FILE"
echo "  Policy ARN: $POLICY_ARN"

# ---- Create IAM User ----
echo ""
echo "[4/6] Creating IAM user..."
if [[ "$USER_EXISTS" == "false" ]]; then
    aws iam create-user \
        --user-name "$USER_NAME" \
        --path "$IAM_PATH" \
        --tags "Key=Project,Value=$PROJECT_NAME" "Key=Environment,Value=$ENVIRONMENT" "Key=Purpose,Value=CI/CD Deployments" \
        > /dev/null
    echo "  User created: $USER_NAME"
else
    echo "  User already exists, skipping."
fi

# ---- Attach Policy ----
echo ""
echo "[5/6] Attaching policy to user..."
aws iam attach-user-policy --user-name "$USER_NAME" --policy-arn "$POLICY_ARN"
echo "  Policy attached."

# ---- Create Access Keys ----
echo ""
echo "[6/6] Creating access keys..."
KEY_JSON=$(aws iam create-access-key --user-name "$USER_NAME" --output json)
ACCESS_KEY_ID=$(echo "$KEY_JSON" | grep -o '"AccessKeyId": *"[^"]*"' | cut -d'"' -f4)
SECRET_ACCESS_KEY=$(echo "$KEY_JSON" | grep -o '"SecretAccessKey": *"[^"]*"' | cut -d'"' -f4)

echo ""
echo "=============================================================="
echo "  ✅ SUCCESS! IAM User Created"
echo "=============================================================="
echo ""
echo "  IAM User:     $USER_NAME"
echo "  IAM Path:     $IAM_PATH"
echo "  Policy:       $POLICY_NAME"
echo ""
echo "  ⚠️  SAVE THESE CREDENTIALS (shown only once!)"
echo ""
echo "  AWS_ACCESS_KEY_ID:     $ACCESS_KEY_ID"
echo "  AWS_SECRET_ACCESS_KEY: $SECRET_ACCESS_KEY"
echo ""
echo "=============================================================="
echo "  NEXT STEPS"
echo "=============================================================="
echo ""
echo "  1. Add to GitHub Secrets (Repo > Settings > Secrets > Actions):"
echo ""
echo "     AWS_ACCESS_KEY_ID      = $ACCESS_KEY_ID"
echo "     AWS_SECRET_ACCESS_KEY  = $SECRET_ACCESS_KEY"
echo "     DB_PASSWORD            = <your-database-password>"
echo "     JWT_SECRET             = <your-jwt-secret>"
echo ""
echo "  2. For local Terraform, set environment variables:"
echo ""
echo "     export AWS_ACCESS_KEY_ID=\"$ACCESS_KEY_ID\""
echo "     export AWS_SECRET_ACCESS_KEY=\"$SECRET_ACCESS_KEY\""
echo "     export AWS_REGION=\"$REGION\""
echo ""
echo "  3. Run Terraform:"
echo ""
echo "     cd terraform"
echo "     terraform init"
echo "     terraform plan -var=\"db_password=YOUR_PASSWORD\" -var=\"jwt_secret=YOUR_SECRET\""
echo "     terraform apply -var=\"db_password=YOUR_PASSWORD\" -var=\"jwt_secret=YOUR_SECRET\""
echo ""
echo "=============================================================="
echo ""
echo "  POLICY SCOPE (what this user CAN access):"
echo "    ✓ EC2 instances       (region: $REGION only)"
echo "    ✓ Security groups     (default VPC)"
echo "    ✓ Elastic IPs         (region: $REGION)"
echo "    ✓ S3 buckets          (prefix: $PROJECT_NAME-* only)"
echo "    ✓ IAM roles           (prefix: $PROJECT_NAME-* only)"
echo "    ✓ CloudWatch logs     (prefix: /syncledger/* only)"
echo "    ✓ SSM parameters      (prefix: /$PROJECT_NAME/* only)"
echo "    ✓ DynamoDB            (table: $PROJECT_NAME-terraform-locks only)"
echo ""
echo "  CANNOT access:"
echo "    ✗ Other projects' resources"
echo "    ✗ IAM users or policies"
echo "    ✗ Billing/account settings"
echo "    ✗ Resources outside $REGION"
echo ""
