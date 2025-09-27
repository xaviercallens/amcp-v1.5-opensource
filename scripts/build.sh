#!/bin/bash

# AMCP v1.4 Build Script
# Builds and packages the complete AMCP framework

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
SKIP_TESTS=false
BUILD_DOCKER=false
QUALITY_CHECKS=false
CLEAN_BUILD=false

# Functions
log() {
    echo -e "${BLUE}[AMCP Build]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

usage() {
    cat << EOF
AMCP v1.4 Build Script

Usage: $0 [OPTIONS]

OPTIONS:
  -h, --help           Show this help message
  -t, --skip-tests     Skip running tests
  -d, --docker         Build Docker images after Maven build
  -q, --quality        Run quality checks (SpotBugs, Checkstyle, PMD)
  -c, --clean          Clean build (mvn clean)
  --all                Run complete build with quality checks and Docker

Examples:
  $0                   # Standard build
  $0 -t                # Build without tests
  $0 --all             # Complete build with quality checks and Docker
  $0 -c -q             # Clean build with quality checks

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            exit 0
            ;;
        -t|--skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        -d|--docker)
            BUILD_DOCKER=true
            shift
            ;;
        -q|--quality)
            QUALITY_CHECKS=true
            shift
            ;;
        -c|--clean)
            CLEAN_BUILD=true
            shift
            ;;
        --all)
            QUALITY_CHECKS=true
            BUILD_DOCKER=true
            shift
            ;;
        *)
            error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        error "Java is not installed or not in PATH"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1-2)
    if [[ "$JAVA_VERSION" < "1.8" ]]; then
        error "Java 8 or higher is required, found: $JAVA_VERSION"
        exit 1
    fi
    success "Java $JAVA_VERSION detected"
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        error "Maven is not installed or not in PATH"
        exit 1
    fi
    
    MVN_VERSION=$(mvn -version | head -n1 | cut -d' ' -f3)
    success "Maven $MVN_VERSION detected"
    
    # Check Docker (if needed)
    if [[ "$BUILD_DOCKER" == true ]]; then
        if ! command -v docker &> /dev/null; then
            error "Docker is not installed or not in PATH"
            exit 1
        fi
        success "Docker detected"
    fi
}

# Main build function
build_project() {
    log "Starting AMCP v1.4 build..."
    
    # Move to project root
    cd "$(dirname "$0")/.."
    
    # Clean if requested
    if [[ "$CLEAN_BUILD" == true ]]; then
        log "Cleaning project..."
        mvn clean
        success "Project cleaned"
    fi
    
    # Build Maven command
    MVN_CMD="mvn compile package"
    
    # Add test skipping if requested
    if [[ "$SKIP_TESTS" == true ]]; then
        MVN_CMD="$MVN_CMD -Dmaven.test.skip=true"
        warn "Skipping test compilation and execution"
    else
        log "Including tests in build"
    fi
    
    # Add quality profile if requested
    if [[ "$QUALITY_CHECKS" == true ]]; then
        MVN_CMD="$MVN_CMD -P quality"
        log "Including quality checks (SpotBugs, Checkstyle, PMD)"
    fi
    
    # Execute Maven build
    log "Executing: $MVN_CMD"
    if $MVN_CMD; then
        success "Maven build completed successfully"
    else
        error "Maven build failed"
        exit 1
    fi
    
    # Build Docker images if requested
    if [[ "$BUILD_DOCKER" == true ]]; then
        build_docker_images
    fi
    
    # Print build summary
    print_build_summary
}

build_docker_images() {
    log "Building Docker images..."
    
    if mvn -P docker package; then
        success "Docker images built successfully"
    else
        error "Docker build failed"
        exit 1
    fi
}

print_build_summary() {
    echo
    log "Build Summary"
    echo "=============="
    
    # Find JAR files
    echo
    echo "Built artifacts:"
    find . -name "*.jar" -not -path "./target/maven-*" -not -path "./.m2/*" | while read jar; do
        size=$(du -h "$jar" | cut -f1)
        echo "  $jar ($size)"
    done
    
    # Docker images if built
    if [[ "$BUILD_DOCKER" == true ]]; then
        echo
        echo "Docker images:"
        docker images | grep amcp | head -5
    fi
    
    echo
    success "AMCP v1.4 build completed successfully!"
    echo
    
    # Next steps
    log "Next steps:"
    echo "  1. Run locally: java -jar core/target/amcp-core-1.4.0.jar"
    echo "  2. Run with Docker: cd deploy/docker && docker-compose up"
    echo "  3. Deploy to Kubernetes: kubectl apply -f deploy/k8s/"
    echo "  4. View documentation: open README.md"
}

# Main execution
main() {
    cat << EOF
    
    █████╗ ███╗   ███╗ ██████╗██████╗     ██╗   ██╗ ██╗    ██╗██╗
    ██╔══██╗████╗ ████║██╔════╝██╔══██╗    ██║   ██║███║    ██║██║
    ███████║██╔████╔██║██║     ██████╔╝    ██║   ██║╚██║    ██║██║
    ██╔══██║██║╚██╔╝██║██║     ██╔═══╝     ╚██╗ ██╔╝ ██║    ██║╚═╝
    ██║  ██║██║ ╚═╝ ██║╚██████╗██║          ╚████╔╝ ███████╗██║██╗
    ╚═╝  ╚═╝╚═╝     ╚═╝ ╚═════╝╚═╝           ╚═══╝  ╚══════╝╚═╝╚═╝
    
    Agent Mesh Communication Protocol v1.4 Build System
    
EOF

    check_prerequisites
    build_project
}

# Execute main function
main "$@"