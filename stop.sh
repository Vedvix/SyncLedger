#!/bin/bash
# =============================================================================
# SYNCLEDGER - STOP ALL SERVICES
# Stops PostgreSQL, backend, and PDF microservice
# =============================================================================

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

get_pids_by_port() {
    local port="$1"

    if command -v lsof &> /dev/null; then
        lsof -ti :"$port" 2>/dev/null || true
    elif command -v netstat &> /dev/null; then
        netstat -ano 2>/dev/null | grep -E "[:.]${port}[[:space:]]" | grep -i LISTENING | awk '{print $NF}' | sort -u
    fi
}

kill_pid_safe() {
    local pid="$1"
    if [ -z "$pid" ]; then
        return 0
    fi

    kill "$pid" 2>/dev/null || taskkill //PID "$pid" //F > /dev/null 2>&1 || true
}

print_header "SyncLedger - Stopping All Services"

# ---- Kill application processes ----
echo -e "${YELLOW}Stopping application processes...${NC}"

# Kill by known ports
for PORT in 8080 8001; do
    PIDS=$(get_pids_by_port "$PORT")
    if [ -n "$PIDS" ]; then
        for PID in $PIDS; do
            echo "  Killing process on port $PORT (PID: $PID)"
            kill_pid_safe "$PID"
        done
    fi
done

# Kill from saved PIDs
if [ -d .pids ]; then
    for PIDFILE in .pids/*.pid; do
        if [ -f "$PIDFILE" ]; then
            PID=$(cat "$PIDFILE")
            kill_pid_safe "$PID"
        fi
    done
    rm -rf .pids
fi

# Kill any remaining Java/Node/Python processes for this project
pkill -f "syncledger-backend" 2>/dev/null || true
pkill -f "spring-boot:run" 2>/dev/null || true
pkill -f "pdf-microservice/main.py" 2>/dev/null || true

echo -e "${GREEN}  Application processes stopped${NC}"

# ---- Stop Docker containers ----
echo ""
read -p "Stop PostgreSQL container too? (y/n, default: y): " STOP_PG
STOP_PG=${STOP_PG:-y}

if [[ $STOP_PG =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Stopping Docker containers...${NC}"

    read -p "Remove database volumes (deletes all data)? (y/n, default: n): " REMOVE_VOLS
    REMOVE_VOLS=${REMOVE_VOLS:-n}

    if [[ $REMOVE_VOLS =~ ^[Yy]$ ]]; then
        docker compose down -v
        echo -e "${RED}  Containers and volumes removed${NC}"
    else
        docker compose down
        echo -e "${GREEN}  Containers stopped, data preserved${NC}"
    fi
else
    echo -e "${YELLOW}  PostgreSQL container left running${NC}"
fi

# ---- Done ----
print_header "All Services Stopped"
echo -e "  ${YELLOW}To start again:${NC} ./start.sh"
echo ""
