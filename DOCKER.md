# 🐳 Serenia - Docker Deployment Guide

This guide explains how to deploy the Serenia application using Docker containers.

## 📋 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        serenia-external                          │
│                         (public network)                         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    ┌────────┴────────┐
                    │    Frontend     │
                    │  (Nginx + SPA)  │
                    │    Port: 80     │
                    └────────┬────────┘
                             │
┌────────────────────────────┼────────────────────────────────────┐
│                   serenia-internal                               │
│                   (private network)                              │
│                            │                                     │
│    ┌───────────────────────┼───────────────────────┐            │
│    │                       │                       │            │
│    ▼                       ▼                       ▼            │
│ ┌──────────┐        ┌──────────────┐        ┌──────────┐       │
│ │ Postgres │◄───────│   Backend    │───────►│ Mailpit  │       │
│ │   :5432  │        │ (Quarkus)    │        │  :1025   │       │
│ └──────────┘        │    :8080     │        │  :8025   │       │
│                     └──────────────┘        └──────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### 1. Clone and Configure

```bash
# Navigate to project root
cd /path/to/Serenia

# Copy environment template
cp .env.example .env

# Edit with your production values
nano .env  # or your preferred editor
```

### 2. Generate JWT Keys

```bash
chmod +x scripts/generate-keys.sh
./scripts/generate-keys.sh
```

### 3. Deploy

```bash
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

## 📁 File Structure

```
Serenia/
├── docker-compose.yaml      # Production compose file
├── docker-compose.dev.yaml  # Development compose file
├── .env.example             # Environment template
├── .env                     # Your production config (git ignored)
├── keys/                    # JWT keys directory (git ignored)
│   ├── privateKey.pem
│   ├── publicKey.pem
│   └── rsaPrivateKey.pem
├── scripts/
│   ├── deploy.sh           # Deployment script
│   └── generate-keys.sh    # Key generation script
├── frontend/
│   ├── Dockerfile          # Angular + Nginx build
│   ├── nginx.conf          # Nginx configuration
│   └── .dockerignore
└── backend/
    ├── Dockerfile.native   # Quarkus native build
    ├── Dockerfile.jvm      # Quarkus JVM build (alternative)
    └── .dockerignore
```

## ⚙️ Configuration

### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `POSTGRES_PASSWORD` | Database password | `secure_password_123` |
| `SECURITY_KEY` | Encryption key (min 32 chars) | `your_32_char_secret_key_here!!` |
| `OPENAI_API_KEY` | OpenAI API key | `sk-...` |

### Optional Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_VERSION` | `latest` | Docker image tag |
| `POSTGRES_DB` | `serenia` | Database name |
| `POSTGRES_USER` | `serenia` | Database user |
| `JWT_ISSUER` | `serenia` | JWT issuer claim |
| `AUTH_EXPIRATION_TIME` | `3600` | Token expiration (seconds) |
| `OPENAI_MODEL` | `gpt-4o-mini` | OpenAI model |
| `FRONTEND_PORT` | `80` | Frontend exposed port |
| `MAILPIT_WEB_PORT` | `8025` | Mailpit UI port |
| `LOG_LEVEL` | `INFO` | Logging level |

## 🛠️ Commands

### Deployment Script

```bash
# Full deployment (build + start)
./scripts/deploy.sh

# Build images only
./scripts/deploy.sh build

# Start services
./scripts/deploy.sh start

# Stop services
./scripts/deploy.sh stop

# Restart services
./scripts/deploy.sh restart

# View status
./scripts/deploy.sh status

# View logs (all services)
./scripts/deploy.sh logs

# View logs (specific service)
./scripts/deploy.sh logs backend
./scripts/deploy.sh logs frontend
./scripts/deploy.sh logs postgres
```

### Docker Compose Commands

```bash
# Production
docker compose up -d
docker compose down
docker compose logs -f

# Development (local services only)
docker compose -f docker-compose.dev.yaml up -d
```

## 🔒 Security Best Practices

1. **Never commit `.env` to version control**
2. **Use strong passwords** (min 16 characters, mixed case, numbers, symbols)
3. **Rotate JWT keys periodically**
4. **Keep Docker images updated**
5. **Database is not exposed externally** (internal network only)
6. **Non-root users** in all containers
7. **Health checks** for all services

## 🐛 Troubleshooting

### Backend won't start

```bash
# Check logs
docker compose logs backend

# Verify database is ready
docker compose exec postgres pg_isready -U serenia

# Check environment variables
docker compose exec backend env | grep -E "(QUARKUS|SERENIA)"
```

### Native build fails

If the Quarkus native build fails (common on low-memory systems), use the JVM version:

```bash
# Option 1: Using deploy script
BUILD_MODE=jvm ./scripts/deploy.sh

# Option 2: Direct docker compose
docker compose -f docker-compose.jvm.yaml up -d
```

The JVM build is faster to compile but uses slightly more memory at runtime (~512MB vs ~128MB for native).

### Database connection issues

```bash
# Check PostgreSQL logs
docker compose logs postgres

# Verify connection
docker compose exec backend curl -v postgres:5432
```

### Email not sending

```bash
# Check Mailpit logs
docker compose logs mailpit

# Verify SMTP connection
docker compose exec backend curl -v mailpit:1025

# View emails in Mailpit UI
# http://localhost:8025
```

## 📊 Monitoring

### Health Endpoints

- **Backend**: `http://localhost:8080/q/health`
- **Frontend**: `http://localhost/health`

### Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f backend
```

### Resource Usage

```bash
docker stats
```

## 🔄 Updates

### Update Images

```bash
# Pull latest base images
docker compose pull

# Rebuild with no cache
docker compose build --no-cache

# Restart
docker compose up -d
```

### Database Migrations

Liquibase migrations run automatically on backend startup.

## 📝 Development Mode

For local development, use the dev compose file which only starts database and mail services:

```bash
# Start dev services
docker compose -f docker-compose.dev.yaml up -d

# Run frontend locally
cd frontend && npm start

# Run backend locally
cd backend && ./mvnw quarkus:dev
```

