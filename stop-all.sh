#!/bin/bash
# =============================================================================
# SYNCLEDGER - STOP ALL SERVICES (Linux/EC2)
# by vedvix
# =============================================================================
# Run with: chmod +x stop-all.sh && ./stop-all.sh
# =============================================================================

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo ""
echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║${NC}                  SYNCLEDGER - STOPPING SERVICES                           ${BLUE}║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check Docker Compose version
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
elif command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
else
    echo -e "${RED}ERROR: Docker Compose is not installed!${NC}"
    exit 1
fi

# Stop all services
echo -e "${YELLOW}Stopping all containers...${NC}"
$DOCKER_COMPOSE --profile all down

echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║${NC}                  ALL SERVICES STOPPED SUCCESSFULLY                        ${GREEN}║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Show final status
echo -e "${BLUE}Remaining containers (should be empty):${NC}"
docker ps --filter "name=syncledger" --format "table {{.Names}}\t{{.Status}}"
