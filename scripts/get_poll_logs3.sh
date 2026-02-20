#!/bin/bash
docker logs syncledger-backend 2>&1 | grep -i -E "error|exception|500|poll|WARN" | grep -v "DEBUG" | tail -80 > /tmp/poll_errors.txt
echo "=== FULL ERROR BLOCK ===" >> /tmp/poll_errors.txt
docker logs syncledger-backend 2>&1 | grep -A50 "Manual email poll triggered" | grep -v "DEBUG" | grep -v "Hibernate:" | grep -v "o1_0\." | grep -v "select$" | grep -v "from$" | grep -v "where$" | grep -v "and " > /tmp/poll_context.txt
cat /tmp/poll_errors.txt
echo "============================"
cat /tmp/poll_context.txt
