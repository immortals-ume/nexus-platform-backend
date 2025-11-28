#!/bin/bash

################################################################################
# Docker Image Build Script for Infrastructure Services
################################################################################
# 
# @description  Builds and optionally pushes Docker images for all three
#               infrastructure services (Config, Discovery, Gateway)
# @author       Platform Team
# @version      1.0.0
# @since        2024-11-28
#
# @usage
#   Local build only:
#     ./build-docker-images.sh --local-only
#
#   Build and tag for registry:
#     ./build-docker-images.sh --registry docker.io/myorg --tag v1.0.0
#
#   Build, tag, and push to registry:
#     ./build-docker-images.sh --registry docker.io/myorg --tag v1.0.0 --push
#
# @options
#   --registry <url>    Docker registry URL (e.g., docker.io/username, ghcr.io/org)
#   --tag <version>     Image tag/version (default: latest)
#   --push              Push images to registry after building
#   --local-only        Build images for local use only (default)
#   --help              Display this help message
#
# @examples
#   # Build for local development
#   ./build-docker-images.sh --local-only
#
#   # Build and push to Docker Hub
#   ./build-docker-images.sh --registry docker.io/mycompany --tag v1.2.3 --push
#
#   # Build and push to GitHub Container Registry
#   ./build-docker-images.sh --registry ghcr.io/myorg --tag latest --push
#
# @prerequisites
#   - Docker installed and running
#   - Maven 3.9+ installed
#   - Java 17+ installed
#   - Docker registry credentials configured (if pushing)
#
# @notes
#   - JARs are built first, then Docker images
#   - Images are multi-stage builds for optimal size
#   - Health checks are included in all images
#   - Non-root user is used for security
#
################################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Script metadata
SCRIPT_VERSION="1.0.0"
SCRIPT_NAME="build-docker-images.sh"

# Default values
REGISTRY=""
TAG="latest"
PUSH=false
LOCAL_ONLY=true

################################################################################
# @function     show_help
# @description  Displays help information and usage examples
# @usage        show_help
################################################################################
show_help() {
    cat << EOF
${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
${BLUE}Infrastructure Services - Docker Image Builder${NC}
${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

${YELLOW}DESCRIPTION:${NC}
    Builds and optionally pushes Docker images for infrastructure services:
    - Config Service (Port 8888)
    - Discovery Service (Port 8761)
    - Gateway Service (Port 8080)

${YELLOW}USAGE:${NC}
    $SCRIPT_NAME [OPTIONS]

${YELLOW}OPTIONS:${NC}
    --registry <url>    Docker registry URL
                        Examples: docker.io/username, ghcr.io/org
    
    --tag <version>     Image tag/version (default: latest)
                        Examples: v1.0.0, 1.2.3, dev, staging
    
    --push              Push images to registry after building
    
    --local-only        Build images for local use only (default)
    
    --help              Display this help message

${YELLOW}EXAMPLES:${NC}
    ${GREEN}# Build for local development${NC}
    ./$SCRIPT_NAME --local-only

    ${GREEN}# Build and tag for Docker Hub${NC}
    ./$SCRIPT_NAME --registry docker.io/mycompany --tag v1.2.3

    ${GREEN}# Build and push to GitHub Container Registry${NC}
    ./$SCRIPT_NAME --registry ghcr.io/myorg --tag latest --push

    ${GREEN}# Build specific version and push${NC}
    ./$SCRIPT_NAME --registry myregistry.azurecr.io --tag 2024.11.28 --push

${YELLOW}PREREQUISITES:${NC}
    ✓ Docker installed and running
    ✓ Maven 3.9+ installed
    ✓ Java 17+ installed
    ✓ Docker registry credentials (if pushing)

${YELLOW}REGISTRY LOGIN:${NC}
    ${GREEN}# Docker Hub${NC}
    docker login

    ${GREEN}# GitHub Container Registry${NC}
    echo \$GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin

    ${GREEN}# Azure Container Registry${NC}
    az acr login --name myregistry

${YELLOW}VERSION:${NC}
    $SCRIPT_VERSION

${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
EOF
}

################################################################################
# @function     print_info
# @description  Prints an informational message in blue
# @param        $1 Message to print
################################################################################
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

################################################################################
# @function     print_success
# @description  Prints a success message in green
# @param        $1 Message to print
################################################################################
print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

################################################################################
# @function     print_warning
# @description  Prints a warning message in yellow
# @param        $1 Message to print
################################################################################
print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

################################################################################
# @function     print_error
# @description  Prints an error message in red
# @param        $1 Message to print
################################################################################
print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

################################################################################
# @function     build_jar
# @description  Builds the JAR file for a service using Maven
# @param        $1 Service name (for display)
# @param        $2 Service directory path
# @return       0 on success, 1 on failure
################################################################################
build_jar() {
    local service_name=$1
    local service_dir=$2
    
    print_info "Building JAR for $service_name..."
    cd "$service_dir"
    
    if mvn clean package -DskipTests; then
        print_success "JAR built successfully for $service_name"
        cd - > /dev/null
        return 0
    else
        print_error "Failed to build JAR for $service_name"
        cd - > /dev/null
        return 1
    fi
}

################################################################################
# @function     build_image
# @description  Builds a Docker image for a service
# @param        $1 Service name (for display)
# @param        $2 Service directory path
# @param        $3 Image name
# @return       0 on success, 1 on failure
################################################################################
build_image() {
    local service_name=$1
    local service_dir=$2
    local image_name=$3
    
    print_info "Building Docker image for $service_name..."
    
    if [ "$LOCAL_ONLY" = true ]; then
        # Build for local use
        if docker build -t "$image_name:$TAG" "$service_dir"; then
            print_success "Docker image built: $image_name:$TAG"
            return 0
        else
            print_error "Failed to build Docker image for $service_name"
            return 1
        fi
    else
        # Build with registry prefix
        local full_image_name="${REGISTRY}/${image_name}:${TAG}"
        if docker build -t "$full_image_name" "$service_dir"; then
            print_success "Docker image built: $full_image_name"
            
            # Also tag as latest if not already
            if [ "$TAG" != "latest" ]; then
                docker tag "$full_image_name" "${REGISTRY}/${image_name}:latest"
                print_info "Also tagged as: ${REGISTRY}/${image_name}:latest"
            fi
            
            return 0
        else
            print_error "Failed to build Docker image for $service_name"
            return 1
        fi
    fi
}

################################################################################
# @function     push_image
# @description  Pushes a Docker image to the registry
# @param        $1 Image name
# @return       0 on success, 1 on failure
################################################################################
push_image() {
    local image_name=$1
    local full_image_name="${REGISTRY}/${image_name}:${TAG}"
    
    print_info "Pushing Docker image: $full_image_name..."
    
    if docker push "$full_image_name"; then
        print_success "Successfully pushed: $full_image_name"
        
        # Push latest tag if different
        if [ "$TAG" != "latest" ]; then
            docker push "${REGISTRY}/${image_name}:latest"
            print_success "Successfully pushed: ${REGISTRY}/${image_name}:latest"
        fi
        
        return 0
    else
        print_error "Failed to push: $full_image_name"
        return 1
    fi
}

################################################################################
# MAIN EXECUTION
################################################################################

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --registry)
      REGISTRY="$2"
      LOCAL_ONLY=false
      shift 2
      ;;
    --tag)
      TAG="$2"
      shift 2
      ;;
    --push)
      PUSH=true
      LOCAL_ONLY=false
      shift
      ;;
    --local-only)
      LOCAL_ONLY=true
      PUSH=false
      shift
      ;;
    --help|-h)
      show_help
      exit 0
      ;;
    *)
      print_error "Unknown option: $1"
      echo "Use --help for usage information"
      exit 1
      ;;
  esac
done

# Print header
echo ""
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_info "Building Infrastructure Services Docker Images"
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_info "Mode: $([ "$LOCAL_ONLY" = true ] && echo "Local Only" || echo "Registry: $REGISTRY")"
print_info "Tag: $TAG"
print_info "Push: $([ "$PUSH" = true ] && echo "Yes" || echo "No")"
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Get script directory and navigate to parent
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.."

# Services to build
declare -A SERVICES=(
    ["config-service"]="config-service"
    ["discovery-service"]="discovery-service"
    ["gateway-service"]="gateway-service"
)

# Step 1: Build JARs
print_info "📦 Step 1: Building JARs for all services..."
echo ""

for service_name in "${!SERVICES[@]}"; do
    service_dir="${SERVICES[$service_name]}"
    if ! build_jar "$service_name" "$service_dir"; then
        print_error "Build process failed at JAR build stage for $service_name"
        exit 1
    fi
    echo ""
done

print_success "✓ All JARs built successfully!"
echo ""

# Step 2: Build Docker images
print_info "🐳 Step 2: Building Docker images..."
echo ""

BUILT_IMAGES=()

for service_name in "${!SERVICES[@]}"; do
    service_dir="${SERVICES[$service_name]}"
    image_name="$service_name"
    
    if build_image "$service_name" "$service_dir" "$image_name"; then
        BUILT_IMAGES+=("$image_name")
    else
        print_error "Build process failed at Docker build stage for $service_name"
        exit 1
    fi
    echo ""
done

print_success "✓ All Docker images built successfully!"
echo ""

# Step 3: Push images if requested
if [ "$PUSH" = true ]; then
    if [ -z "$REGISTRY" ]; then
        print_error "Registry not specified. Use --registry option."
        exit 1
    fi
    
    print_info "🚀 Step 3: Pushing images to registry..."
    echo ""
    
    # Check if Docker is running
    print_info "Checking Docker daemon..."
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker daemon is not running"
        exit 1
    fi
    
    for image_name in "${BUILT_IMAGES[@]}"; do
        if ! push_image "$image_name"; then
            print_warning "Failed to push $image_name, continuing with others..."
        fi
        echo ""
    done
    
    print_success "✓ Push process completed!"
fi

# Summary
echo ""
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_info "📊 Build Summary"
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$LOCAL_ONLY" = true ]; then
    print_info "Built images (local):"
    for image_name in "${BUILT_IMAGES[@]}"; do
        echo "  ✓ $image_name:$TAG"
    done
    echo ""
    print_info "🚀 To run the services locally:"
    echo "  docker-compose up -d"
    echo ""
    print_info "📤 To push to a registry later:"
    echo "  ./scripts/build-docker-images.sh --registry <your-registry> --tag $TAG --push"
else
    print_info "Built and $([ "$PUSH" = true ] && echo "pushed" || echo "tagged") images:"
    for image_name in "${BUILT_IMAGES[@]}"; do
        echo "  ✓ ${REGISTRY}/${image_name}:${TAG}"
        [ "$TAG" != "latest" ] && echo "  ✓ ${REGISTRY}/${image_name}:latest"
    done
    
    if [ "$PUSH" = false ]; then
        echo ""
        print_info "📤 To push these images:"
        echo "  ./scripts/build-docker-images.sh --registry $REGISTRY --tag $TAG --push"
    fi
fi

echo ""
print_success "🎉 All done!"
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
