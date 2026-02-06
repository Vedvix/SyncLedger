@echo off
REM =============================================================================
REM SYNCLEDGER - STOP ALL SERVICES (Windows)
REM by vedvix
REM =============================================================================

echo.
echo ╔═══════════════════════════════════════════════════════════════════════════╗
echo ║                    SYNCLEDGER - Stopping All Services                      ║
echo ╚═══════════════════════════════════════════════════════════════════════════╝
echo.

REM Stop Docker containers
echo [1/2] Stopping Docker containers...
docker-compose down

REM Kill any running Java/Node processes (optional - be careful with this)
echo [2/2] Services stopped.
echo.
echo Note: If you started services in separate terminal windows,
echo       please close those windows manually.
echo.
pause
