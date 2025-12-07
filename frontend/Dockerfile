# ========================================
CMD ["nginx", "-g", "daemon off;"]
# Run nginx in foreground

    CMD curl -f http://localhost:80/ || exit 1
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
# Healthcheck

EXPOSE 80
# Expose port 80

RUN rm -rf /usr/share/nginx/html/*.map 2>/dev/null || true
# Security: Remove unnecessary files

    chmod -R 755 /usr/share/nginx/html
    chown -R nginx-user:nginx-group /usr/share/nginx/html && \
    chown -R nginx-user:nginx-group /var/run && \
    chown -R nginx-user:nginx-group /var/cache/nginx && \
    adduser -S -D -H -u 1001 -h /var/cache/nginx -s /sbin/nologin -G nginx-group nginx-user && \
RUN addgroup -g 1001 -S nginx-group && \
# Create non-root user for security

COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html
# Copy built Angular app from builder stage

COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copy custom nginx configuration

RUN rm /etc/nginx/conf.d/default.conf
# Remove default nginx configuration

RUN apk add --no-cache curl
# Install curl for healthcheck

      org.opencontainers.image.version="1.0.0"
      org.opencontainers.image.description="Angular frontend for Serenia application" \
      org.opencontainers.image.title="Serenia Frontend" \
LABEL maintainer="Serenia Team" \

FROM nginx:alpine AS production
# Stage 2: Production image with Nginx

RUN npm run build -- --configuration=production
# Build Angular application for production

COPY . .
# Copy source code

RUN npm ci --prefer-offline --no-audit
# Install dependencies

COPY package*.json ./
# Copy package files first for better layer caching

WORKDIR /app

FROM node:22-alpine AS builder
# Stage 1: Build Angular application

# ========================================
# SERENIA FRONTEND - Multi-stage Dockerfile

