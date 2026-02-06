@echo off
REM =============================================================================
REM SYNCLEDGER - ONE-CLICK SETUP SCRIPT (Windows)
REM by vedvix
REM =============================================================================
REM This script sets up the entire SyncLedger development environment
REM Prerequisites: Docker Desktop, Java 21, Maven, Node.js
REM =============================================================================

echo.
echo ╔═══════════════════════════════════════════════════════════════════════════╗
echo ║                     SYNCLEDGER - Development Setup                         ║
echo ║                              by vedvix                                     ║
echo ╚═══════════════════════════════════════════════════════════════════════════╝
echo.

REM Check if Docker is running
docker info >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Docker is not running. Please start Docker Desktop and try again.
    exit /b 1
)
echo [OK] Docker is running

REM Start PostgreSQL container
echo.
echo [STEP 1/5] Starting PostgreSQL database...
docker-compose up -d postgres
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to start PostgreSQL container
    exit /b 1
)
echo [OK] PostgreSQL container started

REM Wait for PostgreSQL to be ready
echo.
echo [STEP 2/5] Waiting for PostgreSQL to be ready...
:WAIT_POSTGRES
docker-compose exec -T postgres pg_isready -U syncledger -d syncledger >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Waiting for PostgreSQL...
    timeout /t 2 /nobreak >nul
    goto WAIT_POSTGRES
)
echo [OK] PostgreSQL is ready

REM Install backend dependencies and run migrations
echo.
echo [STEP 3/5] Building backend and running Flyway migrations...
cd syncledger-backend
call mvn clean compile flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/syncledger -Dflyway.user=syncledger -Dflyway.password=syncledger123
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Backend build failed
    cd ..
    exit /b 1
)
cd ..
echo [OK] Backend built and migrations complete

REM Install frontend dependencies
echo.
echo [STEP 4/5] Installing frontend dependencies...
cd frontend
call npm install
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Frontend npm install failed
    cd ..
    exit /b 1
)
cd ..
echo [OK] Frontend dependencies installed

REM Install Python dependencies for PDF microservice
echo.
echo [STEP 5/5] Installing PDF microservice dependencies...
cd pdf-microservice
pip install -r requirements.txt
if %ERRORLEVEL% neq 0 (
    echo [WARNING] PDF microservice dependencies failed. You may need to install them manually.
)
cd ..
echo [OK] PDF microservice dependencies installed

echo.
echo ╔═══════════════════════════════════════════════════════════════════════════╗
echo ║                         SETUP COMPLETE!                                   ║
echo ╠═══════════════════════════════════════════════════════════════════════════╣
echo ║                                                                           ║
echo ║  To start the services, run:                                             ║
echo ║                                                                           ║
echo ║    Backend:   cd syncledger-backend ^&^& mvn spring-boot:run              ║
echo ║    Frontend:  cd frontend ^&^& npm run dev                                ║
echo ║    PDF Service: cd pdf-microservice ^&^& python main.py                   ║
echo ║                                                                           ║
echo ║  Or use: start-all.bat                                                   ║
echo ║                                                                           ║
echo ║  Default Login:                                                          ║
echo ║    Email: admin@syncledger.local                                         ║
echo ║    Password: Admin@123                                                   ║
echo ║                                                                           ║
echo ║  URLs:                                                                   ║
echo ║    Frontend:  http://localhost:5173                                      ║
echo ║    Backend:   http://localhost:8080/api                                  ║
echo ║    Swagger:   http://localhost:8080/api/swagger-ui.html                  ║
echo ║    pgAdmin:   docker-compose --profile tools up -d (http://localhost:5050)║
echo ║                                                                           ║
echo ╚═══════════════════════════════════════════════════════════════════════════╝
echo.
