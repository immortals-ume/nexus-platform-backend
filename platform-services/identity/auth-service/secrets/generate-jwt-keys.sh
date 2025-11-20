#!/bin/bash

set -euo pipefail

# === CONFIGURATION ===
KEY_DIR="./jwt-keys"
PRIVATE_KEY_FILE="$KEY_DIR/private_key.pem"
PUBLIC_KEY_FILE="$KEY_DIR/public_key.pem"
JWK_FILE="$KEY_DIR/public_jwk.json" # Optional (requires 'jose' CLI tool)

# === CREATE OUTPUT DIR ===
mkdir -p "$KEY_DIR"
chmod 700 "$KEY_DIR"

echo "🔐 Generating 2048-bit RSA private key..."
openssl genpkey -algorithm RSA -out "$PRIVATE_KEY_FILE" -pkeyopt rsa_keygen_bits:2048

echo "🔓 Extracting public key from private key..."
openssl rsa -in "$PRIVATE_KEY_FILE" -pubout -out "$PUBLIC_KEY_FILE"

# === SET PERMISSIONS ===
chmod 600 "$PRIVATE_KEY_FILE"
chmod 644 "$PUBLIC_KEY_FILE"

# === OPTIONAL: Generate JWK (requires `jose` CLI tool) ===
if command -v jose >/dev/null 2>&1; then
  echo "🧬 Generating JWK from public key..."
  jose jwk pub --input="$PUBLIC_KEY_FILE" --output="$JWK_FILE"
  echo "✅ JWK written to $JWK_FILE"
else
  echo "ℹ️  'jose' CLI tool not found — skipping JWK export."
fi

echo -e "\n✅ JWT RSA key pair generated:"
ls -l "$KEY_DIR"
