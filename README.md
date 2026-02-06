# SyncLedger - Quick Start Guide 🚀

## Prerequisites

Before starting, ensure you have the following installed:

- **Docker Desktop** - [Download](https://www.docker.com/products/docker-desktop/)
- **Java 21** - [Download](https://adoptium.net/temurin/releases/)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **Node.js 18+** - [Download](https://nodejs.org/)
- **Python 3.10+** - [Download](https://www.python.org/downloads/)

## One-Click Setup (Recommended)

**Windows:**
```batch
# From the project root directory
setup.bat
```

This script will:
1. ✅ Start PostgreSQL in Docker
2. ✅ Run Flyway database migrations
3. ✅ Install backend dependencies
4. ✅ Install frontend dependencies
5. ✅ Install PDF microservice dependencies

## Start All Services

**Windows:**
```batch
start-all.bat
```

This opens 3 terminal windows:
- Backend (Spring Boot) on http://localhost:8080/api
- Frontend (Vite) on http://localhost:5173
- PDF Microservice (FastAPI) on http://localhost:8000

## Manual Setup

If you prefer to set up manually:

### 1. Start PostgreSQL
```bash
docker-compose up -d postgres
```

### 2. Start Backend
```bash
cd syncledger-backend
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

### 3. Start Frontend
```bash
cd frontend
npm install
npm run dev
```

### 4. Start PDF Microservice
```bash
cd pdf-microservice
pip install -r requirements.txt
python main.py
```

## Default Login Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@syncledger.local | Admin@123 |
| Approver | approver@syncledger.local | Admin@123 |
| Viewer | viewer@syncledger.local | Admin@123 |

## URLs

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| PDF Service | http://localhost:8000 |
| pgAdmin (optional) | http://localhost:5050 |

## Useful Commands

### Database Management

```bash
# Reset database (WARNING: deletes all data)
reset-db.bat

# Stop all Docker services
docker-compose down

# View PostgreSQL logs
docker-compose logs -f postgres

# Start pgAdmin (database GUI)
docker-compose --profile tools up -d pgadmin
```

### Spring Profiles

| Profile | Description | Command |
|---------|-------------|---------|
| `docker` | PostgreSQL via Docker (default) | `mvn spring-boot:run` |
| `local` | H2 in-memory (quick testing) | `mvn spring-boot:run -Dspring-boot.run.profiles=local` |
| `prod` | Production settings | `mvn spring-boot:run -Dspring-boot.run.profiles=prod` |

### Flyway Commands

```bash
# Run migrations manually
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/syncledger -Dflyway.user=syncledger -Dflyway.password=syncledger123

# Check migration status
mvn flyway:info -Dflyway.url=jdbc:postgresql://localhost:5432/syncledger -Dflyway.user=syncledger -Dflyway.password=syncledger123

# Clean database (WARNING: drops all objects)
mvn flyway:clean -Dflyway.url=jdbc:postgresql://localhost:5432/syncledger -Dflyway.user=syncledger -Dflyway.password=syncledger123
```

## Project Structure

```
SyncLedger/
├── docker-compose.yml          # Docker services configuration
├── setup.bat                   # One-click setup script
├── start-all.bat               # Start all services
├── stop-all.bat                # Stop all services
├── reset-db.bat                # Reset database
│
├── syncledger-backend/         # Java Spring Boot Backend
│   ├── src/main/resources/
│   │   ├── application.yml           # Main config
│   │   ├── application-docker.yml    # Docker profile
│   │   ├── application-local.yml     # H2 profile
│   │   ├── application-prod.yml      # Production profile
│   │   └── db/migration/             # Flyway migrations
│   │       ├── V1__Initial_schema.sql
│   │       └── V2__Seed_data.sql
│   └── pom.xml
│
├── frontend/                   # React + Vite Frontend
│   └── package.json
│
├── pdf-microservice/           # Python FastAPI Service
│   └── requirements.txt
│
└── docs/                       # Documentation
```

## Troubleshooting

### PostgreSQL Connection Failed
```
# Check if container is running
docker ps

# Check container logs
docker-compose logs postgres

# Restart container
docker-compose restart postgres
```

### Flyway Migration Failed
```
# Check migration status
mvn flyway:info

# If needed, repair the schema history
mvn flyway:repair
```

### Port Already in Use
```
# Find process using the port (e.g., 8080)
netstat -ano | findstr :8080

# Kill the process
taskkill /PID <pid> /F
```

## Environment Variables (Production)

For production deployment, set these environment variables:

```bash
# Database
DATABASE_URL=jdbc:postgresql://your-host:5432/syncledger
DATABASE_USERNAME=your_user
DATABASE_PASSWORD=your_secure_password

# JWT
JWT_SECRET=your-256-bit-secret-key

# AWS (if using)
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your_key
AWS_SECRET_ACCESS_KEY=your_secret
S3_BUCKET_NAME=your-bucket

# Azure (for email)
AZURE_CLIENT_ID=your_client_id
AZURE_CLIENT_SECRET=your_secret
AZURE_TENANT_ID=your_tenant_id

# Active Profile
SPRING_PROFILES_ACTIVE=prod
```

---

*Documentation by vedvix*
