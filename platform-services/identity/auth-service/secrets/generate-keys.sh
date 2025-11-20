#!/bin/bash

set -euo pipefail

# === CONFIGURATION ===
KEY_DIR="./keys"
DOMAIN="yourdomain.com"          # Change as needed
COUNTRY="US"
STATE="California"
LOCALITY="San Francisco"
ORG="YourCompany"
OU="IT Department"
EMAIL="admin@yourdomain.com"
VALID_DAYS=365

# === CREATE KEY DIR ===
mkdir -p "$KEY_DIR"
chmod 700 "$KEY_DIR"

# === 1. SSH KEY PAIR ===
echo "🔐 Generating SSH key pair..."
ssh-keygen -t rsa -b 4096 -f "$KEY_DIR/ssh_key" -N "" -C "$EMAIL"
chmod 600 "$KEY_DIR/ssh_key"
chmod 644 "$KEY_DIR/ssh_key.pub"

# === 2. TLS PRIVATE KEY ===
echo "🔒 Generating TLS private key..."
openssl genrsa -out "$KEY_DIR/tls_private_key.pem" 4096
chmod 600 "$KEY_DIR/tls_private_key.pem"

# === 3. TLS CSR (Certificate Signing Request) ===
echo "📝 Generating Certificate Signing Request (CSR)..."
openssl req -new -key "$KEY_DIR/tls_private_key.pem" -out "$KEY_DIR/tls_request.csr" -subj "/C=$COUNTRY/ST=$STATE/L=$LOCALITY/O=$ORG/OU=$OU/CN=$DOMAIN/emailAddress=$EMAIL"

# === 4. Self-Signed TLS Certificate ===
echo "📄 Generating self-signed TLS certificate..."
openssl x509 -req -in "$KEY_DIR/tls_request.csr" -signkey "$KEY_DIR/tls_private_key.pem" -out "$KEY_DIR/tls_cert.pem" -days "$VALID_DAYS"
chmod 644 "$KEY_DIR/tls_cert.pem"

# === 5. Extract TLS Public Key ===
echo "🔓 Extracting public key from TLS private key..."
openssl rsa -in "$KEY_DIR/tls_private_key.pem" -pubout -out "$KEY_DIR/tls_public_key.pem"
chmod 644 "$KEY_DIR/tls_public_key.pem"

# === CLEANUP (optional) ===
rm -f "$KEY_DIR/tls_request.csr"

# === OUTPUT SUMMARY ===
echo -e "\n✅ Keys generated in: $KEY_DIR"
ls -l "$KEY_DIR"
