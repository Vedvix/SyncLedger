# =============================================================================
# SYNCLEDGER FRONTEND - DOCKERFILE
# Multi-stage build for React/Vite application
# =============================================================================

# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /app

# Accept build args
ARG VITE_API_URL=http://localhost:8080/api

# Set environment for build
ENV VITE_API_URL=$VITE_API_URL

# Copy package files first for caching
COPY package.json package-lock.json* ./
RUN npm install --legacy-peer-deps

# Copy source and build
COPY . .
RUN npm run build

# Stage 2: Production with nginx
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static files
RUN rm -rf ./*

# Copy built assets from builder
COPY --from=builder /app/dist .

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Create non-root user
RUN addgroup -g 1001 syncledger && \
    adduser -D -u 1001 -G syncledger syncledger && \
    chown -R syncledger:syncledger /var/cache/nginx /var/log/nginx /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown -R syncledger:syncledger /var/run/nginx.pid

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD wget -q --spider http://localhost:80/ || exit 1

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
