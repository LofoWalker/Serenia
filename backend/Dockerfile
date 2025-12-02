# Dockerfile for running a local PostgreSQL instance compatible with Serenity backend
FROM postgres:16-alpine

LABEL maintainer="Serenia" \
      org.opencontainers.image.source="https://github.com/lofo/serenia" \
      org.opencontainers.image.description="PostgreSQL database for Serenity backend"

ENV POSTGRES_DB=serenia \
    POSTGRES_USER=serenia \
    POSTGRES_PASSWORD=serenia \
    PGDATA=/var/lib/postgresql/data/pgdata

# Expose default PostgreSQL port
EXPOSE 5432

# Persist database files
VOLUME ["/var/lib/postgresql/data"]

# Simple healthcheck to ensure PostgreSQL is accepting connections
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=5 \
    CMD pg_isready -d "$POSTGRES_DB" -U "$POSTGRES_USER" || exit 1

# Default entrypoint/cmd provided by postgres image will start the database

