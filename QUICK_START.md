# SyncLedger - Quick Start Guide

Multi-tenant Invoice Processing SaaS Platform with Email Integration

## Prerequisites

- **Docker** & **Docker Compose** (for containerized deployment)
  - Or **Java 21**, **Maven**, **Python 3.11+** (for local development)
- **PostgreSQL** (via Docker or local installation)
- **Azure AD** credentials for Outlook email integration

## Quick Start (Docker)

### 1. Environment Setup

```bash
# Copy environment template
cp .env.example .env

# Edit .env with your Azure AD credentials
# AZURE_CLIENT_ID=your-azure-app-id
# AZURE_CLIENT_SECRET=your-azure-client-secret
# AZURE_TENANT_ID=your-tenant-id
```

### 2. Start Services

```bash
# Start core services (PostgreSQL only)
docker-compose up -d

# Start with backend & PDF service
docker-compose --profile backend up -d

# Start complete stack (all services)
docker-compose --profile all up -d
```

### 3. Access Services

| Service         | URL                      | Default Credentials      |
|-----------------|--------------------------|--------------------------|
| Backend API     | `http://localhost:8080`  | N/A                      |
| PDF Service     | `http://localhost:8001`  | N/A                      |
| Frontend        | `http://localhost:3000`  | N/A                      |
| pgAdmin         | `http://localhost:5050`  | admin@syncledger.local / admin123 |
| LocalStack      | `http://localhost:4566`  | test / test              |

## Local Development (Without Docker)

### Backend (Spring Boot)

```bash
cd syncledger-backend
./mvnw.cmd spring-boot:run

# Or build and run JAR
./mvnw.cmd clean package
java -jar target/syncledger-0.0.1-SNAPSHOT.jar
```

Backend runs on: `http://localhost:8080`

### PDF Microservice (FastAPI)

```bash
cd pdf-microservice
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

PDF Service runs on: `http://localhost:8001`

### Frontend (React)

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on: `http://localhost:5173`

## API Setup

### 1. Register Super Admin

```bash
curl -X POST http://localhost:8080/api/v1/auth/register/super-admin \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@syncledger.io",
    "password": "SecurePassword123!",
    "firstName": "Admin",
    "lastName": "User"
  }'
```

Response will include `accessToken` and `refreshToken`.

### 2. Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@syncledger.io",
    "password": "SecurePassword123!"
  }'
```

### 3. Create Organization

```bash
curl -X POST http://localhost:8080/api/v1/super-admin/organizations \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corp",
    "slug": "acme-corp",
    "emailAddress": "invoices@acme.com",
    "sageApiKey": "your-sage-key",
    "sageApiEndpoint": "https://api.sage.com"
  }'
```

### 4. Configure Email Polling

Update organization with Outlook email:

```bash
curl -X PUT http://localhost:8080/api/v1/super-admin/organizations/{organizationId} \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "emailAddress": "org-invoices@outlook.com",
    "status": "ACTIVE"
  }'
```

Enable email polling in `.env`:

```env
EMAIL_POLLING_ENABLED=true
EMAIL_POLLING_INTERVAL_MS=300000
```

## Email Integration (Microsoft Graph)

### Azure AD App Registration

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to: **Azure Active Directory** → **App registrations** → **New registration**
3. **Name**: `SyncLedger`
4. **Supported account types**: Single tenant
5. Click **Register**

### Grant Permissions

1. Go to **API permissions**
2. Add permission: **Microsoft Graph** → **Application permissions**
3. Search for and add:
   - `Mail.Read` - Read mail in all mailboxes
   - `Mail.ReadWrite` - Optional, for marking as read
4. Click **Grant admin consent**

### Create Client Secret

1. Go to **Certificates & secrets**
2. Click **New client secret**
3. Description: `SyncLedger Key`
4. Copy the secret immediately (shown only once!)
5. Add to `.env`:
   ```env
   AZURE_CLIENT_ID=<Application ID from Overview>
   AZURE_CLIENT_SECRET=<Secret value you just copied>
   AZURE_TENANT_ID=<Directory ID from Overview>
   ```

## Database Schema

Migrations are automatically applied via Flyway:

- **V1__Initial_schema.sql** - Core tables (User, Invoice, InvoiceLineItem, etc.)
- **V2__Add_approvals.sql** - Approval workflow tables
- **V3__Add_multi_tenant_support.sql** - Organizations and tenant isolation

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Frontend (React)                   │
│              @localhost:3000 / :5173                │
└────────────────────┬────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────┐
│           Spring Boot Backend API                   │
│         @localhost:8080 - Port 8080                 │
│  (JWT Auth, Multi-tenant, Role-based Access)       │
└────────────────────┬────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        ↓            ↓            ↓
    ┌────────┐  ┌────────┐  ┌────────┐
    │   PDF  │  │PostgreSQL │AWS S3 │
    │Service │  │Database   │ SQS   │
    │:8001   │  │:5432      │:4566* │
    └────────┘  └────────┘  └────────┘

* LocalStack for local AWS mocking
```

## Troubleshooting

### PostgreSQL Connection Error

```bash
# Check if postgres is running
docker ps | grep postgres

# Verify connection
psql -h localhost -U syncledger -d syncledger
# Password: syncledger123
```

### Maven Build Fails

```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Rebuild with full logging
./mvnw.cmd clean package -X
```

### Docker Build Timeout

```bash
# Increase Docker memory
# Docker Desktop Settings → Resources → Memory: 4GB+

# Or rebuild without cache
docker-compose build --no-cache
```

### Port Already in Use

```powershell
# Find process using port
Get-NetTCPConnection -LocalPort 5432 | Select-Object OwningProcess
taskkill /PID <pid> /F
```

## Testing Multi-Tenant Features

### 1. Create Second Organization

```bash
curl -X POST http://localhost:8080/api/v1/super-admin/organizations \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "TechCorp",
    "slug": "techcorp",
    "emailAddress": "invoices@techcorp.com"
  }'
```

### 2. Add Users to Organizations

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@acme.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "APPROVER",
    "organizationId": 1
  }'
```

### 3. Test Data Isolation

- Login as user from Org 1
- Verify they only see Org 1 invoices
- Login as user from Org 2
- Verify they only see Org 2 invoices

## API Documentation

Once the backend is running, access the interactive API documentation:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## Support & Documentation

See [Invoice_Processing_Portal_Documentation.md](docs/Invoice_Processing_Portal_Documentation.md) for detailed architecture and feature documentation.

---

**Created**: February 2026  
**By**: vedvix  
**Status**: Multi-tenant SaaS with Email Integration
