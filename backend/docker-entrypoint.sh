#!/bin/sh
set -eu

read_secret() {
  name="$1"
  path="/run/secrets/$name"
  if [ -f "$path" ]; then
    # trim trailing newline(s)
    tr -d '\n' < "$path"
  fi
}

# Map Docker secrets -> env vars expected by application.properties
DB_PASSWORD="$(read_secret db_password || true)"
OPENAI_API_KEY_VAL="$(read_secret openai_api_key || true)"
STRIPE_SECRET_KEY_VAL="$(read_secret stripe_secret_key || true)"
STRIPE_WEBHOOK_SECRET_VAL="$(read_secret stripe_webhook_secret || true)"
SMTP_PASSWORD_VAL="$(read_secret smtp_password || true)"
SECURITY_KEY_VAL="$(read_secret security_key || true)"

# Export only if not already provided (allow override for debug)
: "${QUARKUS_DATASOURCE_PASSWORD:=${DB_PASSWORD}}"
: "${OPENAI_API_KEY:=${OPENAI_API_KEY_VAL}}"
: "${STRIPE_SECRET_KEY:=${STRIPE_SECRET_KEY_VAL}}"
: "${STRIPE_WEBHOOK_SECRET:=${STRIPE_WEBHOOK_SECRET_VAL}}"
: "${QUARKUS_MAILER_PASSWORD:=${SMTP_PASSWORD_VAL}}"
: "${SERENIA_SECURITY_KEY:=${SECURITY_KEY_VAL}}"

export QUARKUS_DATASOURCE_PASSWORD OPENAI_API_KEY STRIPE_SECRET_KEY STRIPE_WEBHOOK_SECRET QUARKUS_MAILER_PASSWORD SERENIA_SECURITY_KEY

# Default to listening on all interfaces
exec "$@"

