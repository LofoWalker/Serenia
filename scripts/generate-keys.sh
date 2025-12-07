#!/bin/bash
# ========================================
# SERENIA - JWT Keys Generation Script
# ========================================
# Run this script to generate new RSA key pairs for JWT authentication

set -e

KEYS_DIR="./keys"

echo "=========================================="
echo "  SERENIA - JWT Keys Generation"
echo "=========================================="

# Create keys directory if it doesn't exist
mkdir -p "$KEYS_DIR"

# Check if keys already exist
if [ -f "$KEYS_DIR/privateKey.pem" ] || [ -f "$KEYS_DIR/publicKey.pem" ]; then
    read -p "Keys already exist. Overwrite? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 0
    fi
fi

echo ""
echo "Generating RSA key pair (4096 bits)..."

# Generate private key (PKCS#8 format)
openssl genpkey -algorithm RSA -out "$KEYS_DIR/privateKey.pem" -pkeyopt rsa_keygen_bits:4096

# Extract public key
openssl rsa -in "$KEYS_DIR/privateKey.pem" -pubout -out "$KEYS_DIR/publicKey.pem"

# Also generate RSA private key in traditional format (for compatibility)
openssl rsa -in "$KEYS_DIR/privateKey.pem" -out "$KEYS_DIR/rsaPrivateKey.pem"

# Set secure permissions
chmod 600 "$KEYS_DIR/privateKey.pem"
chmod 600 "$KEYS_DIR/rsaPrivateKey.pem"
chmod 644 "$KEYS_DIR/publicKey.pem"

echo ""
echo "=========================================="
echo "  Keys generated successfully!"
echo "=========================================="
echo ""
echo "Files created:"
echo "  - $KEYS_DIR/privateKey.pem  (PKCS#8 private key)"
echo "  - $KEYS_DIR/publicKey.pem   (Public key)"
echo "  - $KEYS_DIR/rsaPrivateKey.pem (Traditional RSA private key)"
echo ""
echo "IMPORTANT: Keep private keys secure and NEVER commit them to Git!"
echo ""

