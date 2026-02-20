#!/bin/bash
for i in $(seq 1 12); do
  echo "Check $i..."
  RESULT=$(curl -s http://localhost:8080/api/actuator/health 2>/dev/null)
  if [ -n "$RESULT" ]; then
    echo "$RESULT"
    exit 0
  fi
  sleep 5
done
echo "Backend not ready after 60 seconds"
