#!/bin/bash

# AMCP v1.5 - Deployment Preparation Script
# Prepares the framework for production deployment

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "🚀 AMCP v1.5 Deployment Preparation"
echo "===================================="

# Change to project directory
cd "$PROJECT_DIR"

# Configuration  
DEPLOYMENT_TYPE=${1:-"local"}  # local, docker, kubernetes
# macOS compatible version extraction
VERSION=$(grep '<version>' pom.xml | head -1 | cut -d'>' -f2 | cut -d'<' -f1)

echo "📋 Deployment Configuration:"
echo "   • Version: $VERSION"
echo "   • Type: $DEPLOYMENT_TYPE"
echo "   • Target: ${DEPLOYMENT_TARGET:-development}"
echo ""

# Build and test
echo "🔨 Building and Testing..."
./scripts/build-all.sh
./scripts/run-tests.sh

# Package for deployment
echo ""
echo "📦 Packaging for Deployment..."

case $DEPLOYMENT_TYPE in
    "local")
        echo "📁 Local deployment packaging..."
        
        # Create deployment directory
        DEPLOY_DIR="amcp-v1.5-deploy"
        rm -rf "$DEPLOY_DIR"
        mkdir -p "$DEPLOY_DIR"/{bin,lib,conf,logs,examples,docs}
        
        # Copy JARs
        find . -name "*.jar" -not -path "./target/dependency/*" | while read jar; do
            cp "$jar" "$DEPLOY_DIR/lib/"
        done
        
        # Copy scripts
        cp scripts/*.sh "$DEPLOY_DIR/bin/"
        chmod +x "$DEPLOY_DIR/bin/"*.sh
        
        # Copy configuration
        if [ -f "src/main/resources/amcp.properties" ]; then
            cp src/main/resources/amcp.properties "$DEPLOY_DIR/conf/"
        fi
        
        # Copy examples
        cp -r examples/src/main/java/io/amcp/examples/* "$DEPLOY_DIR/examples/" 2>/dev/null || true
        
        # Copy documentation
        cp README.md INSTALLATION_MACOS.md "$DEPLOY_DIR/docs/" 2>/dev/null || true
        
        echo "✅ Local deployment package: $DEPLOY_DIR/"
        ;;
        
    "docker")
        echo "🐳 Docker deployment packaging..."
        
        if [ ! -f "Dockerfile" ]; then
            cat > Dockerfile << 'EOF'
FROM openjdk:8-jre-alpine

# Install required packages
RUN apk add --no-cache bash curl

# Create app directory
WORKDIR /app

# Copy JARs and dependencies
COPY target/classes /app/classes
COPY */target/classes /app/classes/
COPY target/dependency /app/lib

# Copy scripts
COPY scripts/*.sh /app/bin/
RUN chmod +x /app/bin/*.sh

# Expose ports
EXPOSE 8080 8081

# Default command
CMD ["/app/bin/run-greeting-agent.sh"]
EOF
        fi
        
        # Build Docker image
        docker build -t "amcp:$VERSION" .
        echo "✅ Docker image built: amcp:$VERSION"
        ;;
        
    "kubernetes")
        echo "☸️  Kubernetes deployment packaging..."
        
        # Create Kubernetes manifests
        mkdir -p k8s/
        
        cat > k8s/deployment.yaml << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: amcp-v15
  labels:
    app: amcp
    version: v1.5
spec:
  replicas: 3
  selector:
    matchLabels:
      app: amcp
  template:
    metadata:
      labels:
        app: amcp
        version: v1.5
    spec:
      containers:
      - name: amcp
        image: amcp:$VERSION
        ports:
        - containerPort: 8080
        - containerPort: 8081
        env:
        - name: JAVA_OPTS
          value: "-Xmx512m -Xms256m"
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
EOF

        cat > k8s/service.yaml << EOF
apiVersion: v1
kind: Service
metadata:
  name: amcp-service
  labels:
    app: amcp
spec:
  selector:
    app: amcp
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  - name: management
    port: 8081
    targetPort: 8081
  type: LoadBalancer
EOF

        echo "✅ Kubernetes manifests created in k8s/"
        ;;
esac

# Generate deployment documentation
echo ""
echo "📖 Generating Deployment Documentation..."

cat > DEPLOYMENT.md << EOF
# AMCP v1.5 Deployment Guide

## Version Information
- Version: $VERSION
- Build Date: $(date)
- Deployment Type: $DEPLOYMENT_TYPE

## Quick Start

### Local Deployment
\`\`\`bash
# Start greeting agent
./bin/run-greeting-agent.sh

# Start travel planner
./bin/run-travel-planner.sh

# Start weather system
./bin/run-weather-system.sh
\`\`\`

### Configuration
- Configuration files: conf/
- Logs directory: logs/
- Examples: examples/

### Environment Variables
- \`MAPS_API_KEY\`: For travel planning features
- \`WEATHER_API_KEY\`: For weather system integration
- \`JAVA_OPTS\`: JVM options

### Monitoring
- Application logs: logs/amcp.log
- JVM metrics: Available via JMX
- Health check: HTTP GET /health

## Support
- Documentation: docs/README.md
- Installation Guide: docs/INSTALLATION_MACOS.md
- Issue Tracker: GitHub Issues
EOF

echo "✅ Deployment documentation: DEPLOYMENT.md"

# Security check
echo ""
echo "🔒 Security Validation..."

# Check for hardcoded secrets (basic check)
if grep -r "password\|secret\|key" --include="*.java" --include="*.properties" --include="*.yml" src/ | grep -v "API_KEY" | grep -q .; then
    echo "⚠️  Potential hardcoded secrets found - review before deployment"
else
    echo "✅ No obvious hardcoded secrets detected"
fi

# Deployment summary
echo ""
echo "🎉 Deployment Preparation Complete!"
echo ""
echo "📋 Deployment Summary:"
echo "   • Build Status: ✅ Success"
echo "   • Test Status: ✅ All Passed"
echo "   • Package Type: $DEPLOYMENT_TYPE"
echo "   • Security Check: ✅ Completed"
echo ""

case $DEPLOYMENT_TYPE in
    "local")
        echo "🚀 Next Steps for Local Deployment:"
        echo "   1. Review configuration in $DEPLOY_DIR/conf/"
        echo "   2. Set environment variables as needed"
        echo "   3. Run: cd $DEPLOY_DIR && ./bin/run-greeting-agent.sh"
        ;;
    "docker")
        echo "🐳 Next Steps for Docker Deployment:"
        echo "   1. Push image: docker push amcp:$VERSION"
        echo "   2. Deploy: docker run -p 8080:8080 amcp:$VERSION"
        ;;
    "kubernetes")
        echo "☸️  Next Steps for Kubernetes Deployment:"
        echo "   1. Apply manifests: kubectl apply -f k8s/"
        echo "   2. Check status: kubectl get pods -l app=amcp"
        ;;
esac

echo ""