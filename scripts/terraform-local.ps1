# =============================================================================
# SyncLedger - Local Terraform Plan/Apply/Destroy
# Usage: .\scripts\terraform-local.ps1 -Action plan -Env dev
# =============================================================================
param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("plan", "apply", "destroy")]
    [string]$Action,

    [Parameter(Mandatory=$true)]
    [ValidateSet("dev", "staging", "prod")]
    [string]$Env,

    [string]$Region = "us-east-1"
)

$ErrorActionPreference = "Stop"
$InfraDir = Join-Path $PSScriptRoot ".." "infra"

# Prompt for secrets
$DbPassword = Read-Host "Enter DB password" -AsSecureString
$JwtSecret  = Read-Host "Enter JWT secret" -AsSecureString
$ConfigBucket = Read-Host "Enter S3 config bucket name (default: syncledger-config)"
if ([string]::IsNullOrEmpty($ConfigBucket)) { $ConfigBucket = "syncledger-config" }

# Convert SecureString to plain text
$DbPasswordPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($DbPassword))
$JwtSecretPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($JwtSecret))

Push-Location $InfraDir
try {
    # Init with remote backend
    Write-Host "`nInitializing Terraform for [$Env]..." -ForegroundColor Cyan
    terraform init `
        -backend-config="bucket=syncledger-terraform-state" `
        -backend-config="key=syncledger/$Env/terraform.tfstate" `
        -backend-config="region=$Region" `
        -backend-config="dynamodb_table=syncledger-terraform-locks" `
        -backend-config="encrypt=true" `
        -reconfigure

    $CommonArgs = @(
        "-var-file=environments/$Env.tfvars"
        "-var=db_password=$DbPasswordPlain"
        "-var=jwt_secret=$JwtSecretPlain"
        "-var=s3_config_bucket=$ConfigBucket"
        "-input=false"
    )

    switch ($Action) {
        "plan" {
            Write-Host "`nRunning terraform plan..." -ForegroundColor Yellow
            terraform plan @CommonArgs
        }
        "apply" {
            Write-Host "`nRunning terraform apply..." -ForegroundColor Green
            terraform plan @CommonArgs -out=tfplan
            $confirm = Read-Host "Apply this plan? (yes/no)"
            if ($confirm -eq "yes") {
                terraform apply tfplan
            }
        }
        "destroy" {
            Write-Host "`nRunning terraform destroy..." -ForegroundColor Red
            $confirm = Read-Host "Type 'DESTROY' to confirm destruction of [$Env]"
            if ($confirm -eq "DESTROY") {
                terraform destroy @CommonArgs -auto-approve
            } else {
                Write-Host "Aborted." -ForegroundColor Yellow
            }
        }
    }
} finally {
    Pop-Location
}
