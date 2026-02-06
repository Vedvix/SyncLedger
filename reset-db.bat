@echo off
REM =============================================================================
REM SYNCLEDGER - RESET DATABASE (Windows)
REM by vedvix
REM =============================================================================
REM This script resets the database to a clean state
REM WARNING: All data will be lost!
REM =============================================================================

echo.
echo ╔═══════════════════════════════════════════════════════════════════════════╗
echo ║              SYNCLEDGER - Reset Database                                  ║
echo ║                                                                           ║
echo ║  WARNING: This will delete ALL data in the database!                      ║
echo ╚═══════════════════════════════════════════════════════════════════════════╝
echo.

set /p confirm="Are you sure you want to reset the database? (yes/no): "
if /i not "%confirm%"=="yes" (
    echo Operation cancelled.
    exit /b 0
)

echo.
echo [1/4] Stopping containers...
docker-compose down

echo [2/4] Removing database volume...
docker volume rm syncledger_postgres_data 2>nul

echo [3/4] Starting fresh PostgreSQL...
docker-compose up -d postgres

echo [4/4] Waiting for PostgreSQL...
:WAIT_POSTGRES
docker-compose exec -T postgres pg_isready -U syncledger -d syncledger >nul 2>&1
if %ERRORLEVEL% neq 0 (
    timeout /t 2 /nobreak >nul
    goto WAIT_POSTGRES
)

echo.
echo [OK] Database reset complete!
echo.
echo Run 'mvn spring-boot:run' in the backend folder to apply migrations.
echo.
pause
