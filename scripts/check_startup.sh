#!/bin/bash
docker logs syncledger-backend --since 3m 2>&1 | grep -i -E "error|exception|caused|failed|started" | grep -v "DEBUG" | tail -30
