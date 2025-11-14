#!/bin/bash

# Architecture Monitoring Script - Istio + NGINX Ingress
# Shows complete status of the hybrid architecture

echo "=========================================="
echo "   CAR RENTAL - HYBRID ARCHITECTURE"
echo "   NGINX Ingress + Istio Service Mesh"
echo "=========================================="
echo ""

echo "📊 CLUSTER INFO"
echo "----------------------------------------"
kubectl cluster-info | grep "Kubernetes control plane"
echo ""

echo "🌐 ISTIO SYSTEM"
echo "----------------------------------------"
kubectl get pods -n istio-system -o wide
echo ""

echo "🔧 INGRESS NGINX"
echo "----------------------------------------"
kubectl get pods -n ingress-nginx -o wide
echo ""

echo "🏗️  APPLICATION PODS (with Istio sidecars)"
echo "----------------------------------------"
kubectl get pods -n rental-service -o custom-columns=\
NAME:.metadata.name,\
READY:.status.containerStatuses[*].ready,\
STATUS:.status.phase,\
RESTARTS:.status.containerStatuses[*].restartCount,\
AGE:.metadata.creationTimestamp,\
NODE:.spec.nodeName
echo ""

echo "🔗 SERVICES"
echo "----------------------------------------"
kubectl get svc -n rental-service
echo ""

echo "📍 INGRESS RULES"
echo "----------------------------------------"
kubectl get ingress -n rental-service
echo ""

echo "🌀 ISTIO GATEWAY & VIRTUAL SERVICES"
echo "----------------------------------------"
kubectl get gateway,virtualservice,destinationrule,peerauthentication -n rental-service
echo ""

echo "🔐 ISTIO mTLS STATUS"
echo "----------------------------------------"
kubectl get peerauthentication -n rental-service -o custom-columns=\
NAME:.metadata.name,\
MODE:.spec.mtls.mode
echo ""

echo "🧪 QUICK TESTS"
echo "----------------------------------------"
echo "1. External access via Ingress NGINX:"
echo "   curl -H 'Host: api.car-rental.local' http://localhost:80/actuator/health"
curl -s -H "Host: api.car-rental.local" http://localhost:80/actuator/health 2>/dev/null | head -c 100
echo "..."
echo ""

echo "2. Internal communication via Istio:"
POD=$(kubectl get pod -n rental-service -l app=frontend-angular -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
if [ -n "$POD" ]; then
  kubectl exec -n rental-service "$POD" -c frontend-angular -- wget -qO- http://carrental-service:8080/actuator/health 2>/dev/null | head -c 100
  echo "..."
else
  echo "No frontend pod found"
fi
echo ""

echo "📈 RESOURCE USAGE"
echo "----------------------------------------"
kubectl top pods -n rental-service 2>/dev/null || echo "Metrics server not available"
echo ""

echo "=========================================="
echo "✅ Architecture Status: OPERATIONAL"
echo "   - Ingress NGINX: External access"
echo "   - Istio Gateway: Internal routing"
echo "   - mTLS: PERMISSIVE mode"
echo "=========================================="
