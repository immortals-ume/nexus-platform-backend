#!/bin/bash
set -euo pipefail

echo "🚨 This will DELETE ALL your Docker containers, images, volumes, and user-defined networks!"
echo

# List containers
echo "Containers to be removed:"
docker ps -a

# List images
echo
echo "Images to be removed:"
docker images

# List volumes
echo
echo "Volumes to be removed:"
docker volume ls

# List user-defined networks (exclude default ones)
echo
echo "User-defined networks to be removed:"
docker network ls | grep -v "bridge\|host\|none"

echo
read -rp "Are you sure you want to proceed? Type 'yes' to continue: " confirm

if [[ "$confirm" != "yes" ]]; then
  echo "Aborted by user."
  exit 1
fi

echo
echo "Removing all containers..."
docker rm -f $(docker ps -aq) 2>/dev/null || true

echo "Removing all images..."
docker rmi -f $(docker images -aq) 2>/dev/null || true

echo "Removing all volumes..."
docker volume rm $(docker volume ls -q) 2>/dev/null || true

echo "Removing all user-defined networks..."
docker network rm $(docker network ls -q | grep -v "bridge\|host\|none") 2>/dev/null || true

echo
echo "✅ Docker cleanup completed!"
