@echo off
REM =============================================================================
REM SYNCLEDGER - START ALL SERVICES (Windows)
REM by vedvix
REM =============================================================================
REM Usage: start-all.bat         (Docker mode - recommended for production)
REM        start-all.bat dev     (Development mode - opens separate terminals)
REM =============================================================================

setlocal enabledelayedexpansion

echo.
echo ===========================================================================
echo                        SYNCLEDGER - by vedvix
echo ===========================================================================
echo.

REM Check for dev mode argument
if "%1"=="dev" goto :dev_mode

REM =============================================================================
REM DOCKER MODE (Default) - One-click deployment
REM =============================================================================
:docker_mode
echo Starting in DOCKER MODE (use 'start-all.bat dev' for development mode)
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo [1/4] Checking prerequisites...
echo       Docker is running.

echo [2/4] Creating required directories...
if not exist "uploads" mkdir uploads
if not exist "logs" mkdir logs

echo [3/4] Building and starting all services...
docker-compose --profile all up -d --build

echo [4/4] Waiting for services to be healthy...
timeout /t 10 /nobreak >nul

echo.
echo ===========================================================================
echo               SYNCLEDGER IS NOW RUNNING!
echo ===========================================================================
echo.
echo   Frontend:   http://localhost:3000
echo   Backend:    http://localhost:8080/api
echo   Swagger:    http://localhost:8080/api/swagger-ui.html
echo   PDF API:    http://localhost:8001
echo.
echo   Default Super Admin Login:
echo     Email:    superadmin@syncledger.com
echo     Password: SuperAdmin123!
echo.
echo   Useful Commands:
echo     View logs:  docker-compose logs -f
echo     Stop all:   stop-all.bat (or docker-compose down)
echo     Restart:    docker-compose restart
echo.
echo ===========================================================================
echo Container Status:
docker-compose ps
echo.
pause
goto :eof

REM =============================================================================
REM DEVELOPMENT MODE - Opens separate terminal windows
REM =============================================================================
:dev_mode
echo Starting in DEVELOPMENT MODE (services in separate windows)
echo.

REM Start PostgreSQL if not running
echo [1/4] Ensuring PostgreSQL is running...
docker-compose up -d postgres

REM Start Backend in new window
echo [2/4] Starting Backend (Spring Boot)...
start "SyncLedger Backend" cmd /k "cd syncledger-backend && mvn spring-boot:run -Dspring-boot.run.profiles=local"

REM Wait a moment for backend to initialize
timeout /t 5 /nobreak >nul

REM Start PDF Microservice in new window
echo [3/4] Starting PDF Microservice (Python)...
start "SyncLedger PDF Service" cmd /k "cd pdf-microservice && python main.py"

REM Start Frontend in new window
echo [4/4] Starting Frontend (Vite)...
start "SyncLedger Frontend" cmd /k "cd frontend && npm run dev"

echo.
echo ===========================================================================
echo   All services are starting in separate windows!
echo.
echo   Frontend:  http://localhost:5173
echo   Backend:   http://localhost:8080/api
echo   Swagger:   http://localhost:8080/api/swagger-ui.html
echo   PDF API:   http://localhost:8000
echo ===========================================================================
echo.
pause
