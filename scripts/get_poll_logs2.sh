#!/bin/bash
docker logs syncledger-backend 2>&1 | grep -A30 "Manual email poll triggered" | tail -100
