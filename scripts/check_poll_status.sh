#!/bin/bash
echo "=== RECENT EMAIL/POLL LOGS ==="
docker logs syncledger-backend --since 5m 2>&1 | grep -i -E "poll|email|PATCH|error|exception" | grep -v "DEBUG" | grep -v "u1_0\." | grep -v "failed_login" | tail -20
echo ""
echo "=== CONTAINER STATUS ==="
docker ps --format "table {{.Names}}\t{{.Status}}" 2>&1
