@echo off
REM =============================================================================
REM SYNCLEDGER - STOP ALL SERVICES (Windows)
REM by vedvix
REM =============================================================================

echo.
echo ===========================================================================
echo                SYNCLEDGER - Stopping All Services
echo ===========================================================================
echo.

REM Stop all Docker containers with all profiles
echo Stopping Docker containers...
docker-compose --profile all down

echo.
echo ===========================================================================
echo               ALL SERVICES STOPPED SUCCESSFULLY
echo ===========================================================================
echo.
echo Note: If you started services in development mode (separate windows),
echo       please close those windows manually.
echo.

REM Show remaining containers
echo Remaining containers (should be empty):
docker ps --filter "name=syncledger" --format "table {{.Names}}\t{{.Status}}"
echo.
pause
