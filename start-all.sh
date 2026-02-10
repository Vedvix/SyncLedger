#!/bin/bash
# =============================================================================
# SYNCLEDGER - START ALL SERVICES (Linux/EC2)
# by vedvix
# =============================================================================
# Run with: chmod +x start-all.sh && ./start-all.sh
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo ""
echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║${NC}                                                                           ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}   ███████╗██╗   ██╗███╗   ██╗ ██████╗██╗     ███████╗██████╗  ██████╗   ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}   ██╔════╝╚██╗ ██╔╝████╗  ██║██╔════╝██║     ██╔════╝██╔══██╗██╔════╝   ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}   ███████╗ ╚████╔╝ ██╔██╗ ██║██║     ██║     █████╗  ██║  ██║██║  ███╗  ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}   ╚════██║  ╚██╔╝  ██║╚██╗██║██║     ██║     ██╔══╝  ██║  ██║██║   ██║  ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}   ███████║   ██║   ██║ ╚████║╚██████╗███████╗███████╗██████╔╝╚██████╔╝  ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}   ╚══════╝   ╚═╝   ╚═╝  ╚═══╝ ╚═════╝╚══════╝╚══════╝╚═════╝  ╚═════╝   ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}                                                                           ${BLUE}║${NC}"
echo -e "${BLUE}║${NC}                          by vedvix                                        ${BLUE}║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# =============================================================================
# Check prerequisites
# =============================================================================
echo -e "${YELLOW}[1/6]${NC} Checking prerequisites..."

# Check Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}ERROR: Docker is not installed!${NC}"
    echo "Install Docker: https://docs.docker.com/engine/install/"
    exit 1
fi

# Check Docker Compose (v2 or standalone)
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
elif command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
else
    echo -e "${RED}ERROR: Docker Compose is not installed!${NC}"
    echo "Install Docker Compose: https://docs.docker.com/compose/install/"
    exit 1
fi

echo -e "${GREEN}✓ Docker and Docker Compose are installed${NC}"

# Check if Docker daemon is running
if ! docker info &> /dev/null; then
    echo -e "${RED}ERROR: Docker daemon is not running!${NC}"
    echo "Start Docker: sudo systemctl start docker"
    exit 1
fi

echo -e "${GREEN}✓ Docker daemon is running${NC}"

# =============================================================================
# Check environment file
# =============================================================================
echo -e "${YELLOW}[2/6]${NC} Checking environment configuration..."

if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        echo -e "${YELLOW}Creating .env from .env.example...${NC}"
        cp .env.example .env
        echo -e "${YELLOW}⚠ Please configure .env file with your credentials${NC}"
    else
        echo -e "${YELLOW}⚠ No .env file found. Using default values.${NC}"
    fi
else
    echo -e "${GREEN}✓ .env file exists${NC}"
fi

# =============================================================================
# Create required directories
# =============================================================================
echo -e "${YELLOW}[3/6]${NC} Creating required directories..."

mkdir -p uploads
mkdir -p logs

echo -e "${GREEN}✓ Directories created${NC}"

# =============================================================================
# Pull/Build Docker images
# =============================================================================
echo -e "${YELLOW}[4/6]${NC} Building Docker images (this may take a few minutes)..."

$DOCKER_COMPOSE build --parallel

echo -e "${GREEN}✓ Docker images built${NC}"

# =============================================================================
# Start services
# =============================================================================
echo -e "${YELLOW}[5/6]${NC} Starting all services..."

# Start with 'all' profile to include all services
$DOCKER_COMPOSE --profile all up -d

echo -e "${GREEN}✓ Services started${NC}"

# =============================================================================
# Wait for services to be healthy
# =============================================================================
echo -e "${YELLOW}[6/6]${NC} Waiting for services to be healthy..."

# Wait for PostgreSQL
echo -n "  Waiting for PostgreSQL..."
until docker exec syncledger-postgres pg_isready -U syncledger &> /dev/null; do
    echo -n "."
    sleep 2
done
echo -e " ${GREEN}✓${NC}"

# Wait for Backend
echo -n "  Waiting for Backend..."
max_attempts=60
attempt=0
until curl -s http://localhost:8080/api/actuator/health > /dev/null 2>&1; do
    echo -n "."
    sleep 2
    attempt=$((attempt + 1))
    if [ $attempt -ge $max_attempts ]; then
        echo -e " ${YELLOW}(timeout - backend may still be starting)${NC}"
        break
    fi
done
if [ $attempt -lt $max_attempts ]; then
    echo -e " ${GREEN}✓${NC}"
fi

# Wait for Frontend
echo -n "  Waiting for Frontend..."
attempt=0
until curl -s http://localhost:3000 > /dev/null 2>&1; do
    echo -n "."
    sleep 2
    attempt=$((attempt + 1))
    if [ $attempt -ge 30 ]; then
        echo -e " ${YELLOW}(timeout - frontend may still be starting)${NC}"
        break
    fi
done
if [ $attempt -lt 30 ]; then
    echo -e " ${GREEN}✓${NC}"
fi

# =============================================================================
# Show status
# =============================================================================
echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                     SYNCLEDGER IS NOW RUNNING!                            ║${NC}"
echo -e "${GREEN}╠═══════════════════════════════════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║${NC}                                                                           ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  ${BLUE}Frontend:${NC}   http://localhost:3000                                       ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  ${BLUE}Backend:${NC}    http://localhost:8080/api                                   ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  ${BLUE}Swagger:${NC}    http://localhost:8080/api/swagger-ui.html                   ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  ${BLUE}PDF API:${NC}    http://localhost:8001                                       ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}                                                                           ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  ${YELLOW}Default Super Admin Login:${NC}                                             ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}    Email:    superadmin@syncledger.com                                    ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}    Password: SuperAdmin123!                                               ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}                                                                           ${GREEN}║${NC}"
echo -e "${GREEN}╠═══════════════════════════════════════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║${NC}  ${YELLOW}Useful Commands:${NC}                                                        ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}    View logs:     docker compose logs -f                                  ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}    Stop all:      ./stop-all.sh (or docker compose down)                  ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}    Restart:       docker compose restart                                  ${GREEN}║${NC}"
echo -e "${GREEN}║${NC}                                                                           ${GREEN}║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Show container status
echo -e "${BLUE}Container Status:${NC}"
$DOCKER_COMPOSE ps
