#!/bin/bash

# AMCP v1.6 Kubernetes Deployment Script
# Demonstrates Quarkus support with Podman and Kubernetes

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="amcp"
IMAGE_TAG="v1.6.0"
NAMESPACE="amcp"
CLUSTER_NAME="amcp-cluster"
REPLICAS=3

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  AMCP v1.6 - Quarkus Kubernetes Deployment                ║${NC}"
echo -e "${BLUE}║  Demonstrating Cloud-Native Agent Mesh Communication      ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Step 1: Build Podman Image
echo -e "${YELLOW}[1/6]${NC} Building Podman image..."
if podman build -t ${IMAGE_NAME}:${IMAGE_TAG} -f Dockerfile . > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Image built successfully: ${IMAGE_NAME}:${IMAGE_TAG}"
else
    echo -e "${RED}✗${NC} Failed to build image"
    exit 1
fi

# Step 2: Check Kubernetes cluster
echo -e "${YELLOW}[2/6]${NC} Checking Kubernetes cluster..."
if kubectl cluster-info > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Kubernetes cluster is running"
    kubectl cluster-info | grep 'Kubernetes master'
else
    echo -e "${RED}✗${NC} Kubernetes cluster not found"
    echo "Please start a cluster: kind create cluster --name ${CLUSTER_NAME}"
    exit 1
fi

# Step 3: Load image into Kubernetes
echo -e "${YELLOW}[3/6]${NC} Loading image into Kubernetes..."
if kind load docker-image ${IMAGE_NAME}:${IMAGE_TAG} --name ${CLUSTER_NAME} 2>/dev/null; then
    echo -e "${GREEN}✓${NC} Image loaded into cluster"
elif minikube image load ${IMAGE_NAME}:${IMAGE_TAG} 2>/dev/null; then
    echo -e "${GREEN}✓${NC} Image loaded into Minikube"
else
    echo -e "${YELLOW}⚠${NC} Could not load image (may already be available)"
fi

# Step 4: Deploy to Kubernetes
echo -e "${YELLOW}[4/6]${NC} Deploying to Kubernetes..."

# Create namespace
kubectl apply -f k8s/namespace.yaml > /dev/null 2>&1
echo -e "${GREEN}✓${NC} Namespace created"

# Create ConfigMap and RBAC
kubectl apply -f k8s/configmap.yaml > /dev/null 2>&1
echo -e "${GREEN}✓${NC} ConfigMap and RBAC created"

# Deploy application
kubectl apply -f k8s/deployment.yaml > /dev/null 2>&1
echo -e "${GREEN}✓${NC} Deployment created"

# Create service
kubectl apply -f k8s/service.yaml > /dev/null 2>&1
echo -e "${GREEN}✓${NC} Service created"

# Step 5: Wait for deployment
echo -e "${YELLOW}[5/6]${NC} Waiting for deployment to be ready..."
if kubectl rollout status deployment/amcp-quarkus -n ${NAMESPACE} --timeout=300s > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Deployment is ready"
else
    echo -e "${RED}✗${NC} Deployment failed to become ready"
    kubectl describe deployment amcp-quarkus -n ${NAMESPACE}
    exit 1
fi

# Step 6: Display deployment info
echo -e "${YELLOW}[6/6]${NC} Deployment information..."
echo ""
echo -e "${BLUE}Pods:${NC}"
kubectl get pods -n ${NAMESPACE} -o wide

echo ""
echo -e "${BLUE}Services:${NC}"
kubectl get svc -n ${NAMESPACE}

echo ""
echo -e "${BLUE}Deployment Status:${NC}"
kubectl get deployment -n ${NAMESPACE}

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  ✓ Deployment Complete!                                   ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

echo -e "${BLUE}Next Steps:${NC}"
echo "1. Port forward the service:"
echo "   ${YELLOW}kubectl port-forward -n ${NAMESPACE} svc/amcp 8080:80${NC}"
echo ""
echo "2. Test the deployment:"
echo "   ${YELLOW}curl http://localhost:8080/q/health${NC}"
echo ""
echo "3. Run end-to-end tests:"
echo "   ${YELLOW}./end-to-end-test.sh${NC}"
echo ""
echo "4. View logs:"
echo "   ${YELLOW}kubectl logs -f deployment/amcp-quarkus -n ${NAMESPACE}${NC}"
echo ""
echo "5. Scale deployment:"
echo "   ${YELLOW}kubectl scale deployment amcp-quarkus -n ${NAMESPACE} --replicas=5${NC}"
echo ""
echo -e "${BLUE}Quarkus Features Demonstrated:${NC}"
echo "✓ Cloud-native containerization with Podman"
echo "✓ Kubernetes orchestration with 3 replicas"
echo "✓ Health checks (liveness, readiness, startup)"
echo "✓ Resource management (CPU, memory limits)"
echo "✓ Pod anti-affinity for distribution"
echo "✓ Kafka-coordinated agent mesh"
echo "✓ Metrics and monitoring"
echo ""
