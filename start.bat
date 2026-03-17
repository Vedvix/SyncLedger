@echo off
REM =============================================================================
REM SYNCLEDGER - START ALL SERVICES (Windows)
REM Starts PostgreSQL, Backend, and PDF Service
REM =============================================================================
setlocal enabledelayedexpansion

echo.
echo =======================================================
echo   SyncLedger - Starting All Services
echo =======================================================
echo.

REM ---- Check Docker ----
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running. Please start Docker Desktop.
    pause
    exit /b 1
)
echo [OK] Docker is running

REM ---- Create .env if missing ----
if not exist .env (
    echo [INFO] Creating .env from .env.example...
    copy .env.example .env >nul
    echo [OK] .env created. Edit it with your credentials before continuing.
)

REM ---- Start PostgreSQL ----
echo.
echo [1/4] Starting PostgreSQL...
docker compose up -d postgres
echo Waiting for PostgreSQL to be healthy...

set /a RETRIES=0
:wait_pg
set /a RETRIES+=1
if %RETRIES% gtr 30 (
    echo [ERROR] PostgreSQL failed to start. Run: docker compose logs postgres
    pause
    exit /b 1
)
docker exec syncledger-postgres pg_isready -U syncledger -d syncledger >nul 2>&1
if errorlevel 1 (
    echo   Waiting... (%RETRIES%/30)
    timeout /t 2 /nobreak >nul
    goto wait_pg
)
echo [OK] PostgreSQL is ready!

REM ---- Install PDF Microservice Dependencies ----
echo.
echo [2/3] Setting Up PDF Microservice...
if not exist .venv (
    echo Creating virtual environment...
    python -m venv .venv
)
call .venv\Scripts\activate.bat
cd pdf-microservice
pip install -r requirements.txt -q
cd ..

REM ---- Start Services in Separate Windows ----
echo.
echo [3/3] Starting Application Services...

echo Starting Backend (Spring Boot)...
start "SyncLedger - Backend" cmd /k "cd syncledger-backend && mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=docker"

echo Starting PDF Microservice (FastAPI)...
start "SyncLedger - PDF Service" cmd /k "call .venv\Scripts\activate.bat && cd pdf-microservice && python main.py"

REM ---- Done ----
echo.
echo =======================================================
echo   All Services Started!
echo =======================================================
echo.
echo   Backend API:  http://localhost:8080/api
echo   Swagger UI:   http://localhost:8080/api/swagger-ui.html
echo   PDF Service:  http://localhost:8001
echo   PostgreSQL:   localhost:5432
echo.
echo   Frontend runs separately from syncledger-frontend repo.
echo   To stop all services: stop.bat
echo.
pause
