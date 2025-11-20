#!/bin/bash
set -euo pipefail

# Configurable parameters
CPU=2                # Limit CPU cores
MEMORY=4             # Limit memory in GB
DISK=30              # Disk size in GB
DOCKER_CONTEXT=colima-secure

echo "🛠️  Setting up Colima with secure defaults..."

# Stop existing colima if running
if colima status | grep -q "Running"; then
  echo "⏹️  Stopping existing Colima instance..."
  colima stop
fi

echo "🔧 Starting Colima with resource limits: CPU=$CPU, Memory=${MEMORY}GB, Disk=${DISK}GB"

colima start \
  --cpu $CPU \
  --memory ${MEMORY} \
  --disk $DISK \
  --mount-type 9p \
  --kubernetes=false \
  --vm-type qemu

echo "✅ Colima started with secure resource limits."

# Switch docker context to Colima
docker context use $DOCKER_CONTEXT || {
  echo "ℹ️ Docker context '$DOCKER_CONTEXT' does not exist, creating..."
  docker context create $DOCKER_CONTEXT --docker "host=unix://$HOME/.colima/docker.sock"
  docker context use $DOCKER_CONTEXT
}

echo "🔒 Setting permissions on Colima Docker socket..."
chmod 660 "$HOME/.colima/docker.sock"

# Firewall / network hardening inside Lima VM
echo "🔐 Applying network security hardening inside Colima VM..."

colima nerdctl exec -- sh -c "
  apk add --no-cache iptables &&
  iptables -P INPUT DROP &&
  iptables -P FORWARD DROP &&
  iptables -P OUTPUT ACCEPT &&
  iptables -A INPUT -i lo -j ACCEPT &&
  iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
"

echo "⚠️ Note: Modify firewall rules as per your network requirements."

echo "🎉 Colima secure setup complete!"