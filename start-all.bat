@echo off
REM =============================================================================
REM SYNCLEDGER - START ALL SERVICES (Windows)
REM by vedvix
REM =============================================================================

echo.
echo ╔═══════════════════════════════════════════════════════════════════════════╗
echo ║                    SYNCLEDGER - Starting All Services                      ║
echo ╚═══════════════════════════════════════════════════════════════════════════╝
echo.

REM Start PostgreSQL if not running
echo [1/4] Ensuring PostgreSQL is running...
docker-compose up -d postgres

REM Start Backend in new window
echo [2/4] Starting Backend (Spring Boot)...
start "SyncLedger Backend" cmd /k "cd syncledger-backend && mvn spring-boot:run -Dspring-boot.run.profiles=docker"

REM Wait a moment for backend to initialize
timeout /t 5 /nobreak >nul

REM Start PDF Microservice in new window
echo [3/4] Starting PDF Microservice (Python)...
start "SyncLedger PDF Service" cmd /k "cd pdf-microservice && python main.py"

REM Start Frontend in new window
echo [4/4] Starting Frontend (Vite)...
start "SyncLedger Frontend" cmd /k "cd frontend && npm run dev"

echo.
echo ╔═══════════════════════════════════════════════════════════════════════════╗
echo ║  All services are starting in separate windows!                           ║
echo ║                                                                           ║
echo ║  URLs:                                                                   ║
echo ║    Frontend:  http://localhost:5173                                      ║
echo ║    Backend:   http://localhost:8080/api                                  ║
echo ║    Swagger:   http://localhost:8080/api/swagger-ui.html                  ║
echo ║    PDF API:   http://localhost:8000                                      ║
echo ╚═══════════════════════════════════════════════════════════════════════════╝
echo.
pause
