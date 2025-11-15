#!/bin/bash

# Deployment script for rental-service
# Automatically detects Kind or Minikube and deploys accordingly

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Rental Service Deployment Script${NC}"
echo "===================================="

# Detect current Kubernetes context
CURRENT_CONTEXT=$(kubectl config current-context)
echo -e "${BLUE}Current context:${NC} $CURRENT_CONTEXT"

# Determine environment
if [[ "$CURRENT_CONTEXT" == *"kind"* ]]; then
    ENV="kind"
    OVERLAY="overlays/kind"
    echo -e "${GREEN}✓ Detected Kind cluster${NC}"
elif [[ "$CURRENT_CONTEXT" == *"minikube"* ]]; then
    ENV="minikube"
    OVERLAY="overlays/minikube"
    echo -e "${GREEN}✓ Detected Minikube cluster${NC}"
else
    echo -e "${YELLOW}⚠ Unknown context: $CURRENT_CONTEXT${NC}"
    echo "Please specify environment: kind or minikube"
    read -p "Enter environment (kind/minikube): " ENV
    OVERLAY="overlays/$ENV"
fi

echo ""
echo -e "${BLUE}📦 Deploying to ${ENV}...${NC}"
echo ""

# Apply Kustomize configuration
echo -e "${BLUE}Applying Kustomize configuration from ${OVERLAY}${NC}"
kubectl apply -k "$OVERLAY"

echo ""
echo -e "${GREEN}✓ Base resources deployed${NC}"

# Deploy Istio resources (common to both environments)
echo ""
echo -e "${BLUE}🔧 Checking Istio installation...${NC}"

if kubectl get namespace istio-system &> /dev/null; then
    echo -e "${GREEN}✓ Istio is installed${NC}"
    
    # Label namespace for Istio injection if not already labeled
    if ! kubectl get namespace rental-service -o jsonpath='{.metadata.labels.istio-injection}' | grep -q "enabled"; then
        echo -e "${BLUE}Enabling Istio sidecar injection...${NC}"
        kubectl label namespace rental-service istio-injection=enabled --overwrite
        echo -e "${GREEN}✓ Istio injection enabled${NC}"
    else
        echo -e "${GREEN}✓ Istio injection already enabled${NC}"
    fi
    
    # Restart deployments to inject sidecars
    echo -e "${BLUE}Restarting deployments to inject Istio sidecars...${NC}"
    kubectl rollout restart deployment -n rental-service
    kubectl delete pod postgres-0 -n rental-service --ignore-not-found=true
    
    echo -e "${GREEN}✓ Deployments restarted${NC}"
else
    echo -e "${RED}✗ Istio is not installed${NC}"
    echo -e "${YELLOW}Please install Istio first:${NC}"
    echo "  istioctl install --set profile=demo -y"
    exit 1
fi

# Wait for rollout
echo ""
echo -e "${BLUE}⏳ Waiting for deployments to be ready...${NC}"
kubectl wait --for=condition=ready pod --all -n rental-service --timeout=120s

echo ""
echo -e "${GREEN}✅ Deployment complete!${NC}"
echo ""

# Environment-specific instructions
if [ "$ENV" == "kind" ]; then
    echo -e "${BLUE}📝 Access your application:${NC}"
    echo "  Frontend: http://car-rental.local"
    echo "  API:      http://car-rental.local/api/"
    echo "  Direct:   http://car-rental.local/direct-api/"
    echo ""
    echo -e "${YELLOW}Note:${NC} Make sure you have added '127.0.0.1 car-rental.local' to /etc/hosts"
elif [ "$ENV" == "minikube" ]; then
    MINIKUBE_IP=$(minikube ip)
    echo -e "${BLUE}📝 Access your application:${NC}"
    echo "  1. Start minikube tunnel in another terminal:"
    echo "     minikube tunnel"
    echo ""
    echo "  2. Add to /etc/hosts:"
    echo "     $MINIKUBE_IP car-rental.local"
    echo ""
    echo "  3. Access:"
    echo "     Frontend: http://car-rental.local"
    echo "     API:      http://car-rental.local/api/"
fi

echo ""
echo -e "${BLUE}🔍 Check deployment status:${NC}"
echo "  kubectl get pods -n rental-service"
echo "  kubectl get ingress -n rental-service"
echo "  kubectl get gateway,virtualservice -n rental-service"
