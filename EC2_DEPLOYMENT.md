# SyncLedger - EC2 Deployment Guide

Quick deployment guide for running SyncLedger on AWS EC2.

## Prerequisites

- An EC2 instance (Amazon Linux 2, Ubuntu 20.04+, or similar)
- At least 2GB RAM, 2 vCPU recommended
- Security Group with ports open: **22** (SSH), **3000** (Frontend), **8080** (Backend)

## Quick Start (One-Click Deployment)

### Step 1: Connect to EC2

```bash
ssh -i your-key.pem ec2-user@your-ec2-ip
# or for Ubuntu:
ssh -i your-key.pem ubuntu@your-ec2-ip
```

### Step 2: Clone Repository

```bash
git clone https://github.com/your-repo/SyncLedger.git
cd SyncLedger
```

### Step 3: Run Setup Script (First Time Only)

```bash
chmod +x setup-ec2.sh
sudo ./setup-ec2.sh
```

This installs Docker, Docker Compose, and creates necessary configuration files.

### Step 4: Configure Environment

```bash
nano .env
```

Update these values:
```env
# Required for security
JWT_SECRET=your-unique-secret-key-here

# Optional: Azure AD for email polling
AZURE_CLIENT_ID=your-azure-client-id
AZURE_CLIENT_SECRET=your-azure-client-secret
AZURE_TENANT_ID=your-azure-tenant-id
EMAIL_POLLING_ENABLED=true
```

### Step 5: Start All Services

```bash
./start-all.sh
```

That's it! The application will be available at:
- **Frontend**: http://your-ec2-ip:3000
- **Backend**: http://your-ec2-ip:8080/api
- **Swagger**: http://your-ec2-ip:8080/api/swagger-ui.html

### Default Login

```
Email:    superadmin@syncledger.com
Password: SuperAdmin123!
```

## Managing the Application

### View Logs

```bash
docker compose logs -f           # All services
docker compose logs -f backend   # Backend only
docker compose logs -f frontend  # Frontend only
```

### Stop All Services

```bash
./stop-all.sh
```

### Restart Services

```bash
docker compose restart
```

### Update Application

```bash
git pull
./stop-all.sh
./start-all.sh
```

## Troubleshooting

### Port already in use

```bash
# Find what's using the port
sudo lsof -i :3000
sudo lsof -i :8080

# Kill if needed
sudo kill -9 <PID>
```

### Docker permission denied

```bash
# Add user to docker group
sudo usermod -aG docker $USER
newgrp docker
```

### View container status

```bash
docker compose ps
docker compose logs --tail=100 backend
```

### Reset database

```bash
docker compose down -v
./start-all.sh
```

## Security Recommendations

1. **Change default credentials** immediately after first login
2. **Use HTTPS** with a reverse proxy (nginx/traefik) for production
3. **Restrict Security Group** to only necessary IPs
4. **Use strong JWT_SECRET** - never use default value in production
5. **Enable email polling** only if needed

## File Structure

```
SyncLedger/
├── start-all.sh      # Start all services (Linux)
├── stop-all.sh       # Stop all services (Linux)
├── setup-ec2.sh      # First-time EC2 setup
├── start-all.bat     # Start all services (Windows)
├── stop-all.bat      # Stop all services (Windows)
├── .env              # Configuration (create from setup-ec2.sh)
├── docker-compose.yml
├── uploads/          # Local file storage (auto-created)
└── logs/             # Application logs (auto-created)
```

## Support

For issues or questions, contact vedvix.
