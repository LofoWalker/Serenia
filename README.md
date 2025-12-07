# Serenia

A mental health and well-being AI assistant application.

## 🏗️ Architecture

- **Frontend**: Angular 21 with TailwindCSS
- **Backend**: Quarkus 3.29 (Native build support)
- **Database**: PostgreSQL 16
- **Email**: Mailpit (SMTP server)

## 🚀 Quick Start

### Docker Deployment (Recommended)

```bash
# 1. Copy and configure environment
cp .env.example .env
# Edit .env with your values (POSTGRES_PASSWORD, SECURITY_KEY, OPENAI_API_KEY)

# 2. Generate JWT keys
./scripts/generate-keys.sh

# 3. Deploy
./scripts/deploy.sh
```

Access the application at http://localhost:80

📖 For detailed deployment instructions, see [DOCKER.md](DOCKER.md)

### Local Development

```bash
# Start database and mail server
docker compose -f docker-compose.dev.yaml up -d

# Run backend
cd backend && ./mvnw quarkus:dev

# Run frontend
cd frontend && npm install && npm start
```

## 📁 Project Structure

```
Serenia/
├── frontend/          # Angular application
├── backend/           # Quarkus API
├── docs/              # Documentation
├── keys/              # JWT keys (git ignored)
├── scripts/           # Deployment scripts
├── docker-compose.yaml      # Production deployment
└── docker-compose.dev.yaml  # Development services
```

## 📚 Documentation

- [Docker Deployment Guide](DOCKER.md)
- [Backend API Contract](docs/BACKEND_API_CONTRACT.md)
- [Backend Security](docs/BACKEND_SECURITY.md)
- [Backend Workflow](docs/BACKEND_WORKFLOW.md)
