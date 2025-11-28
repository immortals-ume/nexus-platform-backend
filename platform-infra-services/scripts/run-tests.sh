#!/bin/bash

################################################################################
# Test Execution Script for Infrastructure Services
################################################################################
# 
# @description  Runs tests and generates coverage reports for all infrastructure
#               services with detailed reporting and analysis
# @author       Platform Team
# @version      1.0.0
# @since        2024-11-28
#
# @usage
#   Run all tests:
#     ./run-tests.sh
#
#   Run tests for specific service:
#     ./run-tests.sh --service config-service
#
#   Run with coverage report:
#     ./run-tests.sh --coverage
#
#   Run only unit tests:
#     ./run-tests.sh --unit-only
#
# @options
#   --service <name>    Run tests for specific service only
#                       Options: config-service, discovery-service, gateway-service
#   --coverage          Generate and display coverage report
#   --unit-only         Run only unit tests (skip integration tests)
#   --integration-only  Run only integration tests
#   --property-only     Run only property-based tests
#   --verbose           Show detailed test output
#   --help              Display this help message
#
# @examples
#   # Run all tests with coverage
#   ./run-tests.sh --coverage
#
#   # Run tests for config service only
#   ./run-tests.sh --service config-service --coverage
#
#   # Run only property-based tests
#   ./run-tests.sh --property-only --verbose
#
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Default values
SERVICE=""
COVERAGE=false
UNIT_ONLY=false
INTEGRATION_ONLY=false
PROPERTY_ONLY=false
VERBOSE=false

# Helper functions
print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

show_help() {
    cat << EOF
${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
${BLUE}Infrastructure Services - Test Runner${NC}
${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

${YELLOW}USAGE:${NC}
    ./run-tests.sh [OPTIONS]

${YELLOW}OPTIONS:${NC}
    --service <name>       Run tests for specific service
    --coverage             Generate coverage report
    --unit-only            Run only unit tests
    --integration-only     Run only integration tests
    --property-only        Run only property-based tests
    --verbose              Show detailed output
    --help                 Display this help

${YELLOW}EXAMPLES:${NC}
    ./run-tests.sh --coverage
    ./run-tests.sh --service config-service --verbose
    ./run-tests.sh --property-only

${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
EOF
}

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --service) SERVICE="$2"; shift 2 ;;
    --coverage) COVERAGE=true; shift ;;
    --unit-only) UNIT_ONLY=true; shift ;;
    --integration-only) INTEGRATION_ONLY=true; shift ;;
    --property-only) PROPERTY_ONLY=true; shift ;;
    --verbose) VERBOSE=true; shift ;;
    --help|-h) show_help; exit 0 ;;
    *) print_error "Unknown option: $1"; exit 1 ;;
  esac
done

# Navigate to project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.."

print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_info "🧪 Running Tests for Infrastructure Services"
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Build Maven command
MVN_CMD="mvn clean test"
[ "$COVERAGE" = true ] && MVN_CMD="$MVN_CMD jacoco:report"
[ "$VERBOSE" = false ] && MVN_CMD="$MVN_CMD -q"

# Add test filters
if [ "$UNIT_ONLY" = true ]; then
    MVN_CMD="$MVN_CMD -Dtest=*Test"
elif [ "$INTEGRATION_ONLY" = true ]; then
    MVN_CMD="$MVN_CMD -Dtest=*IT"
elif [ "$PROPERTY_ONLY" = true ]; then
    MVN_CMD="$MVN_CMD -Dtest=*PropertyTest"
fi

# Run tests
if [ -n "$SERVICE" ]; then
    print_info "Running tests for $SERVICE..."
    cd "$SERVICE"
    eval "$MVN_CMD"
    cd ..
else
    print_info "Running tests for all services..."
    eval "$MVN_CMD"
fi

print_success "✓ All tests completed!"

# Show coverage if requested
if [ "$COVERAGE" = true ]; then
    echo ""
    print_info "📊 Coverage Reports Generated:"
    echo "  Config Service:    file://$(pwd)/config-service/target/site/jacoco/index.html"
    echo "  Discovery Service: file://$(pwd)/discovery-service/target/site/jacoco/index.html"
    echo "  Gateway Service:   file://$(pwd)/gateway-service/target/site/jacoco/index.html"
fi

echo ""
print_success "🎉 Done!"
