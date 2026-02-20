#!/bin/bash
# Save full backend logs and extract relevant error info
docker logs syncledger-backend 2>&1 > /tmp/backend_full.log
echo "=== ERRORS AND EXCEPTIONS ==="
grep -i -E "ERROR|Exception|Caused by|500" /tmp/backend_full.log | grep -v "DEBUG" | head -40
echo ""
echo "=== AROUND triggerPoll ==="
grep -n "triggerPoll\|EmailController" /tmp/backend_full.log | head -20
echo ""
echo "=== STACK TRACE AFTER LINE WITH triggerPoll ==="
LINE=$(grep -n "triggerPoll" /tmp/backend_full.log | head -1 | cut -d: -f1)
if [ -n "$LINE" ]; then
  START=$((LINE - 5))
  END=$((LINE + 5))
  sed -n "${START},${END}p" /tmp/backend_full.log
fi
echo ""
echo "=== HTTP 500 RESPONSES ==="
grep -B5 "500\|Internal Server Error" /tmp/backend_full.log | grep -v "DEBUG\|SQL\|Hibernate\|select\|from\|where\|o1_0\|u1_0\|i1_0\|el1_0" | head -20
