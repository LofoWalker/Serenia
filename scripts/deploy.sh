#!/bin/bash
# ========================================
# SERENIA - Deployment Script
# ========================================
# Production deployment script for Serenia application

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
BUILD_MODE="${BUILD_MODE:-native}"
COMPOSE_FILE="docker-compose.yaml"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Set compose file based on build mode
set_compose_file() {
    if [ "$BUILD_MODE" == "jvm" ]; then
        COMPOSE_FILE="docker-compose.jvm.yaml"
        log_info "Using JVM build mode"
    else
        COMPOSE_FILE="docker-compose.yaml"
        log_info "Using Native build mode"
    fi
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

    if ! command -v docker compose &> /dev/null; then
        log_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi

    log_success "Prerequisites check passed."
}

# Check environment file
check_env_file() {
    if [ ! -f ".env" ]; then
        log_warning ".env file not found!"

        if [ -f ".env.example" ]; then
            log_info "Creating .env from .env.example..."
            cp .env.example .env
            log_warning "Please edit .env file with your production values before continuing."
            exit 1
        else
            log_error ".env.example not found. Please create .env file manually."
            exit 1
        fi
    fi

    # Check required variables
    source .env

    local missing_vars=()

    if [ -z "$POSTGRES_PASSWORD" ] || [ "$POSTGRES_PASSWORD" == "your_secure_database_password_here" ]; then
        missing_vars+=("POSTGRES_PASSWORD")
    fi

    if [ -z "$SECURITY_KEY" ] || [ "$SECURITY_KEY" == "your_strong_security_key_here_minimum_32_characters" ]; then
        missing_vars+=("SECURITY_KEY")
    fi

    if [ -z "$OPENAI_API_KEY" ] || [ "$OPENAI_API_KEY" == "sk-your_openai_api_key_here" ]; then
        missing_vars+=("OPENAI_API_KEY")
    fi

    if [ ${#missing_vars[@]} -gt 0 ]; then
        log_error "Missing or invalid required environment variables:"
        for var in "${missing_vars[@]}"; do
            echo "  - $var"
        done
        exit 1
    fi

    log_success "Environment file validated."
}

# Check JWT keys
check_jwt_keys() {
    log_info "Checking JWT keys..."

    if [ ! -d "./keys" ]; then
        mkdir -p ./keys
    fi

    if [ ! -f "./keys/privateKey.pem" ] || [ ! -f "./keys/publicKey.pem" ]; then
        log_warning "JWT keys not found. Generating new keys..."
        ./scripts/generate-keys.sh
    fi

    log_success "JWT keys are ready."
}

# Build images
build_images() {
    set_compose_file
    log_info "Building Docker images..."

    docker compose -f "$COMPOSE_FILE" build --no-cache

    log_success "Docker images built successfully."
}

# Start services
start_services() {
    set_compose_file
    log_info "Starting services..."

    docker compose -f "$COMPOSE_FILE" up -d

    log_success "Services started."
}

# Stop services
stop_services() {
    set_compose_file
    log_info "Stopping services..."

    docker compose -f "$COMPOSE_FILE" down

    log_success "Services stopped."
}

# Show status
show_status() {
    set_compose_file
    log_info "Service status:"
    docker compose -f "$COMPOSE_FILE" ps
}

# Show logs
show_logs() {
    set_compose_file
    local service=${1:-}

    if [ -n "$service" ]; then
        docker compose -f "$COMPOSE_FILE" logs -f "$service"
    else
        docker compose -f "$COMPOSE_FILE" logs -f
    fi
}

# Full deployment
deploy() {
    echo ""
    echo "=========================================="
    echo "  SERENIA - Production Deployment"
    echo "=========================================="
    echo ""

    check_prerequisites
    check_env_file
    check_jwt_keys
    set_compose_file
    build_images
    start_services

    echo ""
    log_success "Deployment complete!"
    echo ""
    log_info "Build mode: $BUILD_MODE"
    log_info "Services:"
    echo "  - Frontend:  http://localhost:${FRONTEND_PORT:-80}"
    echo "  - Mailpit:   http://localhost:${MAILPIT_WEB_PORT:-8025}"
    echo ""
    log_info "Useful commands:"
    echo "  - View logs:    ./scripts/deploy.sh logs"
    echo "  - View status:  ./scripts/deploy.sh status"
    echo "  - Stop:         ./scripts/deploy.sh stop"
    echo "  - Restart:      ./scripts/deploy.sh restart"
    echo "  - JVM mode:     BUILD_MODE=jvm ./scripts/deploy.sh"
    echo ""
}

# Restart services
restart_services() {
    stop_services
    start_services
}

# Main
case "${1:-deploy}" in
    deploy)
        deploy
        ;;
    build)
        check_prerequisites
        build_images
        ;;
    start)
        check_prerequisites
        start_services
        ;;
    stop)
        stop_services
        ;;
    restart)
        restart_services
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs "$2"
        ;;
    *)
        echo "Usage: $0 {deploy|build|start|stop|restart|status|logs [service]}"
        echo ""
        echo "Options:"
        echo "  deploy   - Full deployment (build + start)"
        echo "  build    - Build Docker images only"
        echo "  start    - Start services"
        echo "  stop     - Stop services"
        echo "  restart  - Restart services"
        echo "  status   - Show service status"
        echo "  logs     - View logs (optionally specify service)"
        echo ""
        echo "Environment variables:"
        echo "  BUILD_MODE=jvm    - Use JVM build instead of native"
        echo ""
        echo "Examples:"
        echo "  ./deploy.sh deploy                  # Native build deployment"
        echo "  BUILD_MODE=jvm ./deploy.sh deploy   # JVM build deployment"
        echo "  ./deploy.sh logs backend            # View backend logs"
        exit 1
        ;;
esac

