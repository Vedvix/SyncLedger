#!/bin/bash
# =============================================================================
# SYNCLEDGER - EC2 FIRST-TIME SETUP
# by vedvix
# =============================================================================
# This script installs all prerequisites and sets up SyncLedger on a fresh EC2
# Run with: chmod +x setup-ec2.sh && sudo ./setup-ec2.sh
# =============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║${NC}                                                                           ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}              SYNCLEDGER - EC2 FIRST-TIME SETUP                            ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}                          by vedvix                                        ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}                                                                           ${BLUE}║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo -e "${YELLOW}Note: Some operations may require sudo privileges${NC}"
fi

# =============================================================================
# Detect OS
# =============================================================================
echo -e "${YELLOW}[1/5]${NC} Detecting operating system..."

if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
    VERSION=$VERSION_ID
else
    OS=$(uname -s)
fi

echo -e "${GREEN}✓ Detected: $OS $VERSION${NC}"

# =============================================================================
# Update system packages
# =============================================================================
echo -e "${YELLOW}[2/5]${NC} Updating system packages..."

case $OS in
    ubuntu|debian)
        apt-get update -y
        apt-get upgrade -y
        ;;
    amzn|rhel|centos|fedora)
        yum update -y
        ;;
    *)
        echo -e "${YELLOW}⚠ Unknown OS, skipping system update${NC}"
        ;;
esac

echo -e "${GREEN}✓ System packages updated${NC}"

# =============================================================================
# Install Docker
# =============================================================================
echo -e "${YELLOW}[3/5]${NC} Installing Docker..."

if command -v docker &> /dev/null; then
    echo -e "${GREEN}✓ Docker already installed${NC}"
else
    case $OS in
        ubuntu|debian)
            # Remove old versions
            apt-get remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true
            
            # Install prerequisites
            apt-get install -y \
                ca-certificates \
                curl \
                gnupg \
                lsb-release
            
            # Add Docker's official GPG key
            mkdir -p /etc/apt/keyrings
            curl -fsSL https://download.docker.com/linux/$OS/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
            
            # Add repository
            echo \
                "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/$OS \
                $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
            
            # Install Docker Engine
            apt-get update -y
            apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
            ;;
            
        amzn)
            # Amazon Linux
            yum install -y docker
            ;;
            
        rhel|centos|fedora)
            # Remove old versions
            yum remove -y docker docker-client docker-client-latest docker-common docker-latest docker-latest-logrotate docker-logrotate docker-engine 2>/dev/null || true
            
            # Install prerequisites
            yum install -y yum-utils
            
            # Add repository
            yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
            
            # Install Docker
            yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
            ;;
            
        *)
            echo -e "${RED}ERROR: Unsupported OS for automatic Docker installation${NC}"
            echo "Please install Docker manually: https://docs.docker.com/engine/install/"
            exit 1
            ;;
    esac
    
    echo -e "${GREEN}✓ Docker installed${NC}"
fi

# =============================================================================
# Configure Docker
# =============================================================================
echo -e "${YELLOW}[4/5]${NC} Configuring Docker..."

# Start Docker daemon
systemctl start docker 2>/dev/null || service docker start 2>/dev/null || true
systemctl enable docker 2>/dev/null || chkconfig docker on 2>/dev/null || true

# Add current user to docker group (if not root)
if [ "$EUID" -ne 0 ]; then
    CURRENT_USER=$(whoami)
    if ! groups $CURRENT_USER | grep -q docker; then
        usermod -aG docker $CURRENT_USER
        echo -e "${YELLOW}⚠ Added $CURRENT_USER to docker group. Please log out and back in for changes to take effect.${NC}"
    fi
else
    # Add ec2-user or ubuntu user to docker group
    for user in ec2-user ubuntu admin; do
        if id "$user" &>/dev/null; then
            usermod -aG docker $user 2>/dev/null || true
        fi
    done
fi

echo -e "${GREEN}✓ Docker configured${NC}"

# =============================================================================
# Install Docker Compose (standalone) if not available
# =============================================================================
if ! docker compose version &> /dev/null && ! command -v docker-compose &> /dev/null; then
    echo -e "${YELLOW}Installing Docker Compose standalone...${NC}"
    curl -SL https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    echo -e "${GREEN}✓ Docker Compose installed${NC}"
fi

# =============================================================================
# Create .env if not exists
# =============================================================================
echo -e "${YELLOW}[5/5]${NC} Setting up environment..."

if [ ! -f .env ]; then
    cat > .env << 'EOF'
# =============================================================================
# SYNCLEDGER CONFIGURATION
# =============================================================================

# JWT Secret (CHANGE THIS IN PRODUCTION!)
JWT_SECRET=your-super-secret-jwt-key-change-in-production

# Azure AD (for email polling - optional)
AZURE_CLIENT_ID=your-azure-client-id
AZURE_CLIENT_SECRET=your-azure-client-secret
AZURE_TENANT_ID=your-azure-tenant-id

# Email Polling Configuration
EMAIL_POLLING_ENABLED=false
EMAIL_POLLING_INTERVAL=300000

# Storage Configuration (local or s3)
STORAGE_TYPE=local

# AWS S3 (only if STORAGE_TYPE=s3)
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key
AWS_REGION=ap-south-1
S3_BUCKET_NAME=syncledger-invoices

# Database (defaults work with docker-compose)
DB_HOST=postgres
DB_PORT=5432
DB_NAME=syncledger
DB_USERNAME=syncledger
DB_PASSWORD=syncledger123
EOF
    echo -e "${YELLOW}⚠ Created .env file - please update with your credentials!${NC}"
else
    echo -e "${GREEN}✓ .env file exists${NC}"
fi

# Create required directories
mkdir -p uploads logs

# Make scripts executable
chmod +x *.sh 2>/dev/null || true

# =============================================================================
# Done!
# =============================================================================
echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║${NC}                    SETUP COMPLETED SUCCESSFULLY!                          ${GREEN}║${NC}"
echo -e "${GREEN}╠═══════════════════════════════════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║${NC}                                                                           ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  ${YELLOW}Next Steps:${NC}                                                             ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}                                                                           ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  1. Edit .env file with your credentials:                                ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}     ${BLUE}nano .env${NC}                                                            ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}                                                                           ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  2. Start all services:                                                  ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}     ${BLUE}./start-all.sh${NC}                                                       ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}                                                                           ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  3. Access the application:                                              ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}     ${BLUE}http://<your-ec2-ip>:3000${NC}                                            ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}                                                                           ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  ${RED}Important:${NC} Open ports 3000, 8080 in EC2 Security Group!               ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}                                                                           ${GREEN}║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check Docker version
echo -e "${BLUE}Installed versions:${NC}"
docker --version
docker compose version 2>/dev/null || docker-compose --version 2>/dev/null

echo ""
echo -e "${YELLOW}If you just added yourself to the docker group, run: newgrp docker${NC}"
echo -e "${YELLOW}Or log out and log back in for changes to take effect.${NC}"
