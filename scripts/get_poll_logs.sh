#!/bin/bash
docker logs syncledger-backend 2>&1 | grep -B1 -A15 "Manual email poll" | tail -80
