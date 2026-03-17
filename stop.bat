@echo off
REM =============================================================================
REM SYNCLEDGER - STOP ALL SERVICES (Windows)
REM Stops all running services and optionally removes Docker volumes
REM =============================================================================

echo.
echo =======================================================
echo   SyncLedger - Stopping All Services
echo =======================================================
echo.

REM ---- Kill application processes by port ----
echo Stopping application processes...

REM Kill Backend (port 8080)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
    echo   Killing Backend (PID: %%a)
    taskkill /PID %%a /F >nul 2>&1
)

REM Kill PDF Service (port 8001)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8001 ^| findstr LISTENING 2^>nul') do (
    echo   Killing PDF Service (PID: %%a)
    taskkill /PID %%a /F >nul 2>&1
)

echo [OK] Application processes stopped

REM ---- Close service terminal windows ----
taskkill /FI "WINDOWTITLE eq SyncLedger - Backend*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq SyncLedger - PDF Service*" /F >nul 2>&1

REM ---- Stop Docker ----
echo.
set /p STOP_PG="Stop PostgreSQL container too? (y/n, default: y): "
if "%STOP_PG%"=="" set STOP_PG=y

if /i "%STOP_PG%"=="y" (
    set /p REMOVE_VOLS="Remove database volumes - deletes all data? (y/n, default: n): "
    if /i "!REMOVE_VOLS!"=="y" (
        docker compose down -v
        echo [WARNING] Containers and volumes removed
    ) else (
        docker compose down
        echo [OK] Containers stopped, data preserved
    )
) else (
    echo [INFO] PostgreSQL container left running
)

echo.
echo =======================================================
echo   All Services Stopped
echo =======================================================
echo.
echo   To start again: start.bat
echo.
pause
