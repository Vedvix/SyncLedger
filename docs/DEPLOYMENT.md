# SyncLedger - CI/CD & Infrastructure Guide

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    AWS Cloud (VPC)                       │
│                                                         │
│  ┌─────────── Public Subnet ──────────┐                │
│  │                                     │                │
│  │  ┌─────────────────────────────┐   │                │
│  │  │     EC2 (t3.micro/small)    │   │                │
│  │  │  ┌────────┐  ┌──────────┐  │   │                │
│  │  │  │Frontend│  │ Backend  │  │   │                │
│  │  │  │(Nginx) │  │(Spring)  │  │   │                │
│  │  │  └────────┘  └──────────┘  │   │                │
│  │  │       ┌──────────┐         │   │                │
│  │  │       │PDF Service│         │   │                │
│  │  │       │ (FastAPI) │         │   │                │
│  │  │       └──────────┘         │   │                │
│  │  └─────────────┬───────────────┘   │                │
│  └────────────────┼───────────────────┘                │
│                   │                                     │
│  ┌─────────── Private Subnet ─────────┐                │
│  │                │                    │                │
│  │  ┌─────────────▼───────────────┐   │                │
│  │  │   RDS PostgreSQL 16         │   │                │
│  │  │   (db.t4g.micro)            │   │                │
│  │  └─────────────────────────────┘   │                │
│  └─────────────────────────────────────┘                │
│                                                         │
│  ┌─────────────────────────────────────┐                │
│  │  S3 Bucket (Invoice PDFs)           │                │
│  └─────────────────────────────────────┘                │
└─────────────────────────────────────────────────────────┘
```

## Branch → Environment Mapping

| Branch        | Environment | EC2 Type   | RDS Type       | Est. Cost    |
|---------------|-------------|------------|----------------|--------------|
| `develop`     | dev         | t3.micro   | db.t4g.micro   | ~$5-10/mo*   |
| `release/*`   | staging     | t3.small   | db.t4g.micro   | ~$25-30/mo   |
| `main`        | prod        | t3.small   | db.t4g.micro   | ~$25-35/mo   |

\* With AWS Free Tier (first 12 months)

## CI/CD Pipeline

### Workflow 1: Build & Push Images (`build.yml`)
- **Trigger:** Push to `main`, `develop`, or `release/*` branches
- **What it does:**
  1. Detects which services changed (frontend/backend/pdf)
  2. Builds only changed services
  3. Pushes to GHCR with branch-prefixed tags (e.g., `main-latest`, `develop-abc1234`)
  4. Creates a git tag: `{branch}-{date}-{sha}`

### Workflow 2: Deploy (`deploy.yml`)
- **Trigger:** Manual dispatch (workflow_dispatch)
- **Inputs:** branch name, image tag, plan-only option
- **What it does:**
  1. Maps branch → environment (main→prod, develop→dev, release/*→staging)
  2. Terraform init with environment-specific state file
  3. Terraform plan/apply with environment-specific tfvars
  4. Uploads docker-compose.deploy.yml to S3 config bucket
  5. Optionally updates GHCR credentials in Secrets Manager
  6. Deploys to EC2 via SSM Run Command (no SSH needed)
  7. Health checks the application

### Workflow 3: Destroy (`destroy.yml`)
- **Trigger:** Manual dispatch with "DESTROY" confirmation
- **What it does:** Terraform destroy + cleanup Secrets Manager entries

## Initial Setup (One-time)

### Step 1: Bootstrap Terraform State Backend
```bash
cd infra/bootstrap
terraform init
terraform apply
```
This creates:
- S3 bucket for Terraform state (`syncledger-terraform-state`)
- DynamoDB table for state locking (`syncledger-terraform-locks`)
- S3 bucket for config files (`syncledger-config`)

### Step 2: Configure GitHub Repository

#### Secrets (Settings → Secrets and variables → Actions → Secrets)
| Secret                  | Description                         | Required |
|-------------------------|-------------------------------------|----------|
| `AWS_ACCESS_KEY_ID`     | AWS IAM user access key             | Yes      |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM user secret key             | Yes      |
| `DB_PASSWORD`           | PostgreSQL password                 | Yes      |
| `JWT_SECRET`            | JWT signing secret (min 256 bits)   | Yes      |
| `OPENAI_API_KEY`        | OpenAI API key for PDF extraction   | Yes      |
| `GHCR_PAT`             | GitHub PAT with `read:packages`     | Yes      |
| `AZURE_CLIENT_ID`       | Azure AD client ID (email polling)  | No       |
| `AZURE_CLIENT_SECRET`   | Azure AD client secret              | No       |
| `AZURE_TENANT_ID`       | Azure AD tenant ID                  | No       |

#### Variables (Settings → Secrets and variables → Actions → Variables)
| Variable            | Description                    | Default                         |
|---------------------|--------------------------------|---------------------------------|
| `AWS_REGION`        | AWS region                     | `us-east-1`                     |
| `S3_CONFIG_BUCKET`  | S3 bucket for docker-compose   | `syncledger-config`             |
| `TF_STATE_BUCKET`   | S3 bucket for TF state         | `syncledger-terraform-state`    |
| `TF_LOCK_TABLE`     | DynamoDB table for TF locks    | `syncledger-terraform-locks`    |
| `EC2_KEY_NAME`      | SSH key pair name (optional)   | `syncledger-key`                |
| `DOMAIN_NAME`       | Custom domain (optional)       | (empty)                         |

#### Environments (Settings → Environments)
Create these environments for deployment protection:
- `dev` — no protection rules
- `staging` — optional: require reviewer
- `prod` — require reviewer
- `destroy-dev` — require reviewer
- `destroy-staging` — require reviewer
- `destroy-prod` — require reviewer

### Step 3: Create AWS IAM User
The deployer IAM user needs these permissions:
- EC2, VPC, RDS, S3, IAM, SecretsManager, CloudWatch, SSM
- Terraform state S3 bucket + DynamoDB access

### Step 4: First Deployment
```
1. Push code to `develop` branch → triggers build
2. Go to Actions → Deploy → Run workflow
   - deploy_branch: develop
   - action: deploy
   - update_secrets: true (first time)
3. Wait for deployment to complete
4. Access app at the EC2 public IP shown in the summary
```

## Local Development

Local development uses `docker-compose.yml` with a local Postgres container:
```bash
docker-compose up -d        # Start PostgreSQL + LocalStack
cd syncledger-backend
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

## Manual Deployment (without CI/CD)

```bash
# Initialize terraform for a specific environment
cd infra
terraform init \
  -backend-config="bucket=syncledger-terraform-state" \
  -backend-config="key=syncledger/dev/terraform.tfstate" \
  -backend-config="region=us-east-1" \
  -backend-config="dynamodb_table=syncledger-terraform-locks"

# Plan
terraform plan -var-file="environments/dev.tfvars" \
  -var="db_password=YOUR_PASSWORD" \
  -var="jwt_secret=YOUR_JWT_SECRET" \
  -var="s3_config_bucket=syncledger-config"

# Apply
terraform apply -var-file="environments/dev.tfvars" \
  -var="db_password=YOUR_PASSWORD" \
  -var="jwt_secret=YOUR_JWT_SECRET" \
  -var="s3_config_bucket=syncledger-config"
```

## Cost Optimization Tips

1. **Use Free Tier**: t3.micro EC2 + db.t4g.micro RDS = ~$0/mo first year
2. **Run dev only when needed**: Destroy dev env overnight with the destroy workflow
3. **No ALB**: Direct EC2 access saves ~$16/mo per environment
4. **No NAT Gateway**: Saves ~$32/mo per environment
5. **S3 lifecycle rules**: Auto-transition old invoices to cheaper storage
6. **Single AZ RDS**: Saves ~50% vs Multi-AZ (fine for dev/staging)
7. **Reserved Instances**: 1-year RI saves ~30-40% for prod

## File Structure

```
infra/
├── main.tf              # Provider, backend, data sources
├── variables.tf         # All variables
├── locals.tf            # Computed values, AMI detection
├── vpc.tf               # VPC, public/private subnets
├── network.tf           # Internet gateway, route tables
├── security.tf          # Security groups (EC2, RDS)
├── ec2.tf               # EC2 instance, IAM, EIP, CloudWatch
├── rds.tf               # RDS PostgreSQL
├── s3.tf                # S3 bucket with lifecycle rules
├── secrets.tf           # Secrets Manager
├── outputs.tf           # Terraform outputs
├── user_data.sh.tpl     # EC2 bootstrap script
├── bootstrap/
│   └── main.tf          # One-time state backend setup
└── environments/
    ├── dev.tfvars        # Dev environment config
    ├── staging.tfvars    # Staging environment config
    └── prod.tfvars       # Prod environment config

.github/workflows/
├── build.yml            # Build & push Docker images to GHCR
├── deploy.yml           # Terraform + deploy to EC2 via SSM
└── destroy.yml          # Terraform destroy

docker-compose.yml           # Local development (with Postgres)
docker-compose.deploy.yml    # EC2 deployment (connects to RDS)
docker-compose.prod.yml      # Alias for deploy (backward compat)
```
