#!/bin/bash
# =============================================================================
# SYNCLEDGER - START ALL SERVICES
# Starts PostgreSQL, builds backend JAR, and runs backend + PDF services
# =============================================================================
set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

print_header() {
    echo -e "\n${BLUE}=======================================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}=======================================================${NC}\n"
}

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

get_pid_by_port() {
    local port="$1"
    local pid=""

    if command -v lsof &> /dev/null; then
        pid=$(lsof -ti :"$port" 2>/dev/null | head -n 1)
    elif command -v netstat &> /dev/null; then
        pid=$(netstat -ano 2>/dev/null | grep -E "[:.]${port}[[:space:]]" | grep -i LISTENING | awk '{print $NF}' | head -n 1)
    fi

    echo "$pid"
}

kill_pid_safe() {
    local pid="$1"
    if [ -z "$pid" ]; then
        return 0
    fi

    kill "$pid" 2>/dev/null || taskkill //PID "$pid" //F > /dev/null 2>&1 || true
}

ensure_port_free() {
    local port="$1"
    local service_name="$2"
    local pid

    pid=$(get_pid_by_port "$port")
    if [ -n "$pid" ]; then
        echo -e "${YELLOW}  Port $port already in use (PID: $pid) for $service_name. Stopping it...${NC}"
        kill_pid_safe "$pid"
        sleep 1
        pid=$(get_pid_by_port "$port")
        if [ -n "$pid" ]; then
            echo -e "${RED}  Failed to free port $port. Please stop PID $pid manually and retry.${NC}"
            exit 1
        fi
        echo -e "${GREEN}  Port $port is now free${NC}"
    fi
}

print_header "SyncLedger - Starting All Services"

# ---- Step 1: Check prerequisites ----
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker is not installed. Please install Docker Desktop.${NC}"
    exit 1
fi

if ! docker info &> /dev/null 2>&1; then
    echo -e "${RED}Docker is not running. Please start Docker Desktop.${NC}"
    exit 1
fi

echo -e "${GREEN}  Docker is running${NC}"

# Check Java (optional - needed only for running backend outside Docker)
if command -v java &> /dev/null; then
    JAVA_VER=$(java -version 2>&1 | head -n 1)
    echo -e "${GREEN}  Java: $JAVA_VER${NC}"
fi

# Check Node.js
if command -v node &> /dev/null; then
    echo -e "${GREEN}  Node.js: $(node --version)${NC}"
fi

# ---- Step 2: Create .env if missing ----
if [ ! -f .env ]; then
    echo -e "${YELLOW}Creating .env from .env.example...${NC}"
    cp .env.example .env
    echo -e "${GREEN}  .env created. Edit it with your credentials.${NC}"
fi

# ---- Step 3: Start PostgreSQL ----
print_header "Step 1/4: Starting PostgreSQL"
docker compose up -d postgres
echo -e "${YELLOW}Waiting for PostgreSQL to be healthy...${NC}"

for i in $(seq 1 30); do
    if docker exec syncledger-postgres pg_isready -U syncledger -d syncledger > /dev/null 2>&1; then
        echo -e "${GREEN}  PostgreSQL is ready!${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}  PostgreSQL failed to start. Check: docker compose logs postgres${NC}"
        exit 1
    fi
    echo "  Waiting... ($i/30)"
    sleep 2
done

# ---- Step 4: Install PDF microservice dependencies ----
print_header "Step 2/3: Setting Up PDF Microservice"
if [ -f pdf-microservice/requirements.txt ]; then
    cd pdf-microservice
    if [ ! -d ../.venv ]; then
        echo "Creating virtual environment..."
        python3 -m venv ../.venv
    fi
    echo "Installing Python dependencies..."
    source ../.venv/bin/activate 2>/dev/null || ../.venv/Scripts/activate 2>/dev/null || true
    pip install -r requirements.txt -q
    cd "$ROOT_DIR"
fi

# ---- Step 6: Start services ----
print_header "Step 3/3: Starting Application Services"

echo -e "${YELLOW}Checking and freeing required ports...${NC}"
ensure_port_free 8080 "Backend"
ensure_port_free 8001 "PDF Microservice"

mkdir -p .pids

# Start backend
echo -e "${YELLOW}Starting Backend (Spring Boot)...${NC}"
cd syncledger-backend
if [ -f mvnw ]; then
    chmod +x mvnw
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=docker &
    BACKEND_PID=$!
else
    mvn spring-boot:run -Dspring-boot.run.profiles=docker &
    BACKEND_PID=$!
fi
cd "$ROOT_DIR"

# Start PDF microservice
echo -e "${YELLOW}Starting PDF Microservice (FastAPI)...${NC}"
cd pdf-microservice
source ../.venv/bin/activate 2>/dev/null || ../.venv/Scripts/activate 2>/dev/null || true
python main.py &
PDF_PID=$!
cd "$ROOT_DIR"

# Save PIDs for stop script
echo "$BACKEND_PID" > .pids/backend.pid
echo "$PDF_PID" > .pids/pdf.pid

# ---- Done ----
print_header "All Services Started!"

echo -e "  ${GREEN}Backend API:${NC}  http://localhost:8080/api"
echo -e "  ${GREEN}Swagger UI:${NC}   http://localhost:8080/api/swagger-ui.html"
echo -e "  ${GREEN}PDF Service:${NC}  http://localhost:8001"
echo -e "  ${GREEN}PostgreSQL:${NC}   localhost:5432"
echo ""
echo -e "  ${YELLOW}Frontend runs separately from syncledger-frontend repo.${NC}"
echo -e "  ${YELLOW}To stop all services:${NC} ./stop.sh"
echo -e "  ${YELLOW}Logs:${NC} docker compose logs -f postgres"
echo ""

# Wait for any process to exit
wait
