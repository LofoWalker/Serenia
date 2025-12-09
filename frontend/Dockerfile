# ========================================
# SERENIA FRONTEND - Multi-stage Dockerfile
# Node 22 build → Nginx runtime
# ========================================

# ========================================
# Stage 1: Build Angular application
# ========================================
FROM node:22-alpine AS builder
LABEL maintainer="Serenia Team"

# Set working directory
WORKDIR /app

# Copy package files for dependency caching
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci --prefer-offline --no-audit

# Copy source code
COPY . .

# Build Angular application for production
RUN npm run build -- --configuration=production

# Remove source map files for security
RUN find /app/dist -name "*.map" -type f -delete

# ========================================
# Stage 2: Production image with Nginx
# ========================================
FROM nginx:alpine AS production
LABEL maintainer="Serenia Team" \
      org.opencontainers.image.title="Serenia Frontend" \
      org.opencontainers.image.description="Angular frontend for Serenia application" \
      org.opencontainers.image.version="1.0.0"

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user (UID 1001)
RUN addgroup -g 1001 -S serenia && \
    adduser -S -D -H -u 1001 -h /var/cache/nginx -s /sbin/nologin -G serenia serenia

# Remove default nginx configuration
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built Angular app from builder stage
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

# Configure nginx for non-root user and fix pid file location
RUN sed -i 's/listen 80;/listen 8080;/g' /etc/nginx/conf.d/default.conf && \
    sed -i '/user  nginx;/d' /etc/nginx/nginx.conf && \
    sed -i 's|pid\s*/run/nginx.pid;|pid /tmp/nginx.pid;|g' /etc/nginx/nginx.conf

# Set proper ownership for non-root user
RUN chown -R serenia:serenia /usr/share/nginx/html && \
    chown -R serenia:serenia /var/cache/nginx && \
    chown -R serenia:serenia /var/log/nginx && \
    chown -R serenia:serenia /etc/nginx/conf.d && \
    touch /tmp/nginx.pid && \
    chown serenia:serenia /tmp/nginx.pid

# Ensure no .map files remain (double-check)
RUN rm -rf /usr/share/nginx/html/*.map 2>/dev/null || true && \
    find /usr/share/nginx/html -name "*.map" -type f -delete 2>/dev/null || true

# Set permissions
RUN chmod -R 755 /usr/share/nginx/html

# Switch to non-root user
USER 1001

# Expose port 8080 (non-privileged)
EXPOSE 8080

# Run nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
