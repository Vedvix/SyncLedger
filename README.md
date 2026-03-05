# SyncLedger

Multi-tenant Invoice Processing SaaS Platform with AI-powered PDF extraction.

## Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Docker Desktop | Latest | [docker.com](https://www.docker.com/products/docker-desktop/) |
| Java 21 (JDK) | 21+ | [adoptium.net](https://adoptium.net/temurin/releases/) |
| Maven | 3.9+ | [maven.apache.org](https://maven.apache.org/download.cgi) |
| Node.js | 18+ | [nodejs.org](https://nodejs.org/) |
| Python | 3.10+ | [python.org](https://www.python.org/downloads/) |

## Quick Start

### 1. Clone & Configure

```bash
git clone <repo-url>
cd SyncLedger
cp .env.example .env
```

Edit `.env` with your credentials (at minimum set `OPENAI_API_KEY` for AI features).

### 2. Start Everything

**Windows:**
```batch
start.bat
```

**Mac/Linux:**
```bash
chmod +x start.sh
./start.sh
```

This single script will:
1. Start PostgreSQL in Docker
2. Wait for database to be healthy
3. Install frontend dependencies (`npm install`)
4. Install PDF microservice dependencies (`pip install`)
5. Start Backend (Spring Boot) on port 8080
6. Start PDF Microservice (FastAPI) on port 8001
7. Start Frontend (Vite) on port 5173

### 3. Stop Everything

**Windows:**
```batch
stop.bat
```

**Mac/Linux:**
```bash
./stop.sh
```

Stops all application processes and optionally stops PostgreSQL (with option to keep or remove data volumes).

## Service URLs

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| PDF Service | http://localhost:8001 |
| PostgreSQL | localhost:5432 |

## Default Login Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@syncledger.local | Admin@123 |
| Approver | approver@syncledger.local | Admin@123 |
| Viewer | viewer@syncledger.local | Admin@123 |

## Manual Setup (Alternative)

If you prefer to start services individually instead of using the start script:

### Start PostgreSQL
```bash
docker compose up -d postgres
```

### Start Backend
```bash
cd syncledger-backend
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

### Start PDF Microservice
```bash
cd pdf-microservice
python -m venv ../.venv
# Windows: ..\.venv\Scripts\activate
# Mac/Linux: source ../.venv/bin/activate
pip install -r requirements.txt
python main.py
```

### Start Frontend
```bash
cd frontend
npm install
npm run dev
```

## Spring Profiles

| Profile | Database | Usage |
|---------|----------|-------|
| `docker` | PostgreSQL via Docker (default) | `mvn spring-boot:run` |
| `local` | H2 in-memory | `mvn spring-boot:run -Dspring-boot.run.profiles=local` |
| `prod` | External RDS PostgreSQL | Used in deployed environments |

## Project Structure

```
SyncLedger/
├── start.bat / start.sh        # Start all services (one command)
├── stop.bat / stop.sh          # Stop all services
├── docker-compose.yml          # Local dev (PostgreSQL + LocalStack)
├── docker-compose.deploy.yml   # EC2 deployment (connects to RDS)
├── .env.example                # Environment variables template
│
├── syncledger-backend/         # Java 21 / Spring Boot 3.4
│   └── src/main/resources/
│       ├── application.yml           # Main config
│       ├── application-docker.yml    # Docker profile
│       ├── application-local.yml     # H2 profile
│       ├── application-prod.yml      # Production profile
│       └── db/migration/             # Flyway SQL migrations
│
├── frontend/                   # React + TypeScript + Vite + Tailwind
│   └── package.json
│
├── pdf-microservice/           # Python 3.11 / FastAPI
│   └── requirements.txt        # AI extraction: GPT-4o Vision + Text
│
├── infra/                      # Terraform infrastructure
│   ├── main.tf                 # Provider & backend config
│   ├── ec2.tf                  # EC2 + IAM + EIP
│   ├── rds.tf                  # RDS PostgreSQL
│   ├── s3.tf                   # S3 invoice storage
│   ├── vpc.tf                  # VPC + subnets
│   ├── security.tf             # Security groups
│   ├── secrets.tf              # Secrets Manager
│   ├── bootstrap/              # One-time TF state backend setup
│   └── environments/           # Per-env tfvars (dev/staging/prod)
│
├── .github/workflows/          # CI/CD pipelines
│   ├── build.yml               # Build & push Docker images to GHCR
│   ├── deploy.yml              # Terraform + deploy to EC2
│   └── destroy.yml             # Tear down environment
│
└── docs/                       # Documentation
```

## CI/CD & Deployment

Branch-based multi-environment CI/CD. See [DEPLOYMENT.md](DEPLOYMENT.md) for full details.

| Branch | Environment | What happens |
|--------|-------------|--------------|
| `develop` | dev | Build images → Deploy to dev EC2 + RDS |
| `release/*` | staging | Build images → Deploy to staging EC2 + RDS |
| `main` | prod | Build images → Deploy to prod EC2 + RDS |

## Useful Commands

```bash
# Database
docker compose logs -f postgres        # View Postgres logs
docker compose down                     # Stop Postgres (keep data)
docker compose down -v                  # Stop Postgres (delete data)

# Build & Test
cd syncledger-backend && mvn clean test # Run backend tests
cd frontend && npm run build            # Build frontend for production

# Flyway Migrations
cd syncledger-backend
mvn flyway:info -Dflyway.url=jdbc:postgresql://localhost:5432/syncledger \
  -Dflyway.user=syncledger -Dflyway.password=syncledger123
```

## Environment Variables

Copy `.env.example` to `.env`. Key variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_PASSWORD` | PostgreSQL password | `syncledger123` |
| `JWT_SECRET` | JWT signing key (min 256 bits) | dev default |
| `OPENAI_API_KEY` | OpenAI API key (for AI extraction) | — |
| `STORAGE_TYPE` | `local` or `s3` | `local` |
| `EMAIL_POLLING_ENABLED` | Enable Outlook email polling | `false` |
| `AZURE_CLIENT_ID/SECRET/TENANT_ID` | Azure AD (for email) | — |

## Troubleshooting

**Docker not running:**
```
docker info    # Check if Docker daemon is running
```

**Port already in use:**
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <pid> /F

# Mac/Linux
lsof -i :8080
kill <pid>
```

**PostgreSQL connection failed:**
```bash
docker compose logs postgres
docker compose restart postgres
```

**Flyway migration failed:**
```bash
cd syncledger-backend
mvn flyway:repair -Dflyway.url=jdbc:postgresql://localhost:5432/syncledger \
  -Dflyway.user=syncledger -Dflyway.password=syncledger123
```

---

*Built by vedvix*
