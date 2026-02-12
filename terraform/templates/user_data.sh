#!/bin/bash
# =============================================================================
# SYNCLEDGER - EC2 USER DATA SCRIPT
# Bootstraps the instance with Docker and deploys the application
# =============================================================================
set -euo pipefail

# Logging
exec > >(tee /var/log/syncledger-setup.log) 2>&1
echo "=========================================="
echo "SyncLedger Setup - $(date)"
echo "=========================================="

# ---- System Updates ----
dnf update -y
dnf install -y docker git

# ---- Install Docker Compose ----
DOCKER_COMPOSE_VERSION="v2.24.0"
curl -SL "https://github.com/docker/compose/releases/download/$${DOCKER_COMPOSE_VERSION}/docker-compose-linux-x86_64" \
  -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# ---- Start Docker ----
systemctl enable docker
systemctl start docker
usermod -aG docker ec2-user

# ---- Create app directory ----
mkdir -p /opt/syncledger
cd /opt/syncledger

# ---- Write environment file ----
cat > /opt/syncledger/.env <<'ENVEOF'
# Database
DB_PASSWORD=${db_password}
POSTGRES_PASSWORD=${db_password}

# JWT
JWT_SECRET=${jwt_secret}

# AWS / S3
AWS_REGION=${aws_region}
S3_BUCKET_NAME=${s3_bucket_name}
STORAGE_TYPE=${storage_type}

# Email
EMAIL_POLLING_ENABLED=${email_polling_enabled}

# Application
SPRING_PROFILES_ACTIVE=docker
CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:3000,http://localhost:80
ENVEOF

# Secure the env file
chmod 600 /opt/syncledger/.env

# ---- Create docker-compose for production ----
cat > /opt/syncledger/docker-compose.prod.yml <<'COMPOSEEOF'
services:
  postgres:
    image: postgres:16-alpine
    container_name: syncledger-postgres
    restart: always
    environment:
      POSTGRES_DB: syncledger
      POSTGRES_USER: syncledger
      POSTGRES_PASSWORD: $${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U syncledger -d syncledger"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - syncledger

  backend:
    image: ghcr.io/OWNER/syncledger-backend:latest
    container_name: syncledger-backend
    restart: always
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/syncledger
      SPRING_DATASOURCE_USERNAME: syncledger
      SPRING_DATASOURCE_PASSWORD: $${DB_PASSWORD}
      JWT_SECRET: $${JWT_SECRET}
      STORAGE_TYPE: $${STORAGE_TYPE}
      AWS_REGION: $${AWS_REGION}
      S3_BUCKET_NAME: $${S3_BUCKET_NAME}
      PDF_SERVICE_URL: http://pdf-service:8001
      EMAIL_POLLING_ENABLED: $${EMAIL_POLLING_ENABLED}
      CORS_ALLOWED_ORIGINS: $${CORS_ALLOWED_ORIGINS}
    ports:
      - "8080:8080"
    volumes:
      - uploads_data:/app/uploads
    networks:
      - syncledger

  pdf-service:
    image: ghcr.io/OWNER/syncledger-pdf:latest
    container_name: syncledger-pdf
    restart: always
    environment:
      DATABASE_URL: postgresql://syncledger:$${DB_PASSWORD}@postgres:5432/syncledger
    volumes:
      - pdf_temp:/tmp/pdf_processing
    networks:
      - syncledger

  frontend:
    image: ghcr.io/OWNER/syncledger-frontend:latest
    container_name: syncledger-frontend
    restart: always
    depends_on:
      - backend
    ports:
      - "80:80"
    networks:
      - syncledger

volumes:
  postgres_data:
  uploads_data:
  pdf_temp:

networks:
  syncledger:
    driver: bridge
COMPOSEEOF

# ---- Create systemd service for auto-restart ----
cat > /etc/systemd/system/syncledger.service <<'SERVICEEOF'
[Unit]
Description=SyncLedger Application
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/syncledger
ExecStart=/usr/local/bin/docker-compose -f docker-compose.prod.yml up -d
ExecStop=/usr/local/bin/docker-compose -f docker-compose.prod.yml down
EnvironmentFile=/opt/syncledger/.env

[Install]
WantedBy=multi-user.target
SERVICEEOF

systemctl enable syncledger.service

echo "=========================================="
echo "SyncLedger Setup Complete - $(date)"
echo "Instance is ready for deployment."
echo "=========================================="
