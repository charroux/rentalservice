# Kubernetes Deployment with Istio Service Mesh + NGINX Ingress

This directory contains Kubernetes manifests for the car-rental microservices architecture with **hybrid routing**:
- **NGINX Ingress Controller** for external access (browser → services)
- **Istio Service Mesh** for internal service-to-service communication (Angular → carRental)

## 🏗️ Hybrid Architecture Overview

```
Internet/Browser
       │
   ┌───▼────────────────────────────────────────────────────┐
   │              NGINX Ingress Controller                   │
   │  car-rental.local  │  api.car-rental.local  │  gRPC   │
   └─────────┬──────────┴─────────┬───────────────┴─────────┘
             │                    │
    ┌────────▼──────┐    ┌────────▼────────────────────────┐
    │   Angular     │    │   Istio Gateway (Internal)      │
    │   Frontend    │───→│   VirtualService + mTLS         │
    │   Port: 80    │    │   Advanced routing & policies   │
    │   [+ Envoy]   │    └──────────┬──────────────────────┘
    └───────────────┘               │
                           ┌────────▼─────────────────────────┐
                           │         carRental                │
                           │       (REST API)                 │
                           │       Port: 8080                 │
                           │       [+ Envoy Sidecar]          │
                           └─────────┬───────────┬────────────┘
                                     │           │
                            ┌────────▼──────┐    ▼
                            │  PostgreSQL   │ ┌─────────────────┐
                            │  (Database)   │ │ auctionService  │
                            │  Port: 5432   │ │    (gRPC)       │
                            │  [+ Envoy]    │ │   Port: 9090    │
                            └───────────────┘ │   [+ Envoy]     │
                                              └─────────────────┘
```

### Key Features

✅ **Dual Routing Layer**
- External: NGINX Ingress (proven, lightweight, simple)
- Internal: Istio (advanced features, mTLS, observability)

✅ **Istio Benefits for Internal Traffic**
- Automatic mTLS encryption between services
- Intelligent load balancing (LEAST_REQUEST)
- Circuit breaker and retry policies
- Traffic monitoring and distributed tracing
- Fine-grained traffic control

✅ **NGINX Benefits for External Access**
- Lightweight and fast
- Simple configuration
- Wide adoption and proven stability
- Easy integration with Let's Encrypt for TLS

## 🚀 Quick Start with Kind + Istio

### Prerequisites

- Docker Desktop running
- kubectl installed
- Kind installed: `brew install kind`
- **No need to install istioctl manually** (script downloads it)

### 1. Complete Deployment Script

```bash
# Download this deployment script
curl -O https://raw.githubusercontent.com/charroux/rentalservice/gateway/k8s/deploy-istio.sh
chmod +x deploy-istio.sh

# Run complete deployment (creates cluster, installs Istio, deploys apps)
./deploy-istio.sh
```

**Or step-by-step:**

### 2. Create Kind Cluster

```bash
cat > /tmp/kind-istio-config.yaml << 'EOF'
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    protocol: TCP
  - containerPort: 443
    hostPort: 443
    protocol: TCP
- role: worker
- role: worker
EOF

kind create cluster --name rental-service-cluster --config /tmp/kind-istio-config.yaml
```

### 3. Install Istio

```bash
# Download Istio
curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.23.2 sh -
cd istio-1.23.2
export PATH=$PWD/bin:$PATH

# Install Istio with demo profile (includes ingress gateway)
istioctl install --set profile=demo -y

# Verify installation
kubectl get pods -n istio-system
```

### 4. Build and Load Images

```bash
# Build all images
docker build -f carRental/Dockerfile -t carrental:latest .
docker build -f auctionServiceServer/Dockerfile -t auction-service-server:latest .
docker build -f car-rental-angular/Dockerfile -t car-rental-angular:latest .

# Load into Kind cluster
kind load docker-image carrental:latest auction-service-server:latest car-rental-angular:latest --name rental-service-cluster
```

### 5. Deploy Services

```bash
# Create namespace with Istio injection enabled
kubectl apply -f k8s/namespace.yaml
kubectl label namespace rental-service istio-injection=enabled

# Deploy secrets and database
kubectl apply -f k8s/postgres-secret.yaml
kubectl apply -f k8s/postgres-statefulset.yaml

# Wait for PostgreSQL
kubectl wait --for=condition=ready pod/postgres-0 -n rental-service --timeout=120s

# Deploy application services (Istio sidecars auto-injected)
kubectl apply -f k8s/carrental-deployment.yaml
kubectl apply -f k8s/auction-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml

# Deploy Istio Gateway and routing rules
kubectl apply -f k8s/istio-internal-gateway.yaml

# Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=90s

# Deploy Ingress rules
kubectl apply -f k8s/ingress.yaml
```

### 6. Verify Deployment

```bash
# Check all pods (should show 2/2 for app pods = app + Envoy sidecar)
kubectl get pods -n rental-service

# Check Istio configuration
kubectl get gateway,virtualservice,destinationrule,peerauthentication -n rental-service

# Run monitoring script
chmod +x k8s/monitor-istio.sh
./k8s/monitor-istio.sh
```

## 📁 File Structure

```
k8s/
├── README-ISTIO.md                  # This file (Istio architecture)
├── README.md                        # Original README (Ingress-only)
├── namespace.yaml                   # rental-service namespace
├── postgres-secret.yaml             # DB credentials & Spring config
├── postgres-statefulset.yaml        # PostgreSQL with 2Gi PVC
├── carrental-deployment.yaml        # carRental REST API service
├── auction-deployment.yaml          # auctionService gRPC server
├── frontend-deployment.yaml         # Angular frontend with nginx
├── ingress.yaml                     # NGINX Ingress rules (external)
├── istio-internal-gateway.yaml      # Istio Gateway (internal) - NEW
├── istio-gateway.yaml               # Legacy Istio config (for reference)
├── monitor-istio.sh                 # Hybrid architecture monitoring - NEW
└── monitor.sh                       # Original monitoring script
```

## 🔧 Istio Configuration Details

### Gateway Configuration (`istio-internal-gateway.yaml`)

#### 1. Gateway Resource
- Routes internal traffic through Istio Ingress Gateway
- Listens on port 80 for internal service communication
- Handles `carrental-service.rental-service.svc.cluster.local`

#### 2. VirtualService
- **Health checks**: 5s timeout, no retries
- **API endpoints**: 30s timeout, 3 retry attempts on failures
- **Retry policy**: Retries on 5xx, connection failures, resets

#### 3. DestinationRule
- **Load Balancing**: LEAST_REQUEST algorithm (intelligent)
- **Connection Pool**: Max 100 TCP connections, 50 pending HTTP requests
- **Circuit Breaker**: 
  - Ejects unhealthy hosts after 3 consecutive errors
  - 30s ejection time
  - Max 50% of instances can be ejected

#### 4. PeerAuthentication (mTLS)
- **Mode**: PERMISSIVE
  - Accepts both mTLS (Istio-to-Istio) and plaintext (Ingress-to-Service)
  - Allows NGINX Ingress to communicate with services
  - Services with Istio sidecars automatically use mTLS between themselves

## 🛠️ Development Workflow

### Rebuild and Redeploy with Istio

```bash
# Rebuild carRental
docker build -f carRental/Dockerfile -t carrental:latest .
kind load docker-image carrental:latest --name rental-service-cluster
kubectl rollout restart deployment/carrental -n rental-service

# Watch pod recreation with new sidecars
kubectl get pods -n rental-service -w
```

### Testing & Debugging

#### 1. Via NGINX Ingress (External Access)
```bash
# Test frontend
curl -H "Host: car-rental.local" http://localhost:80/

# Test API via subdomain
curl -H "Host: api.car-rental.local" http://localhost:80/actuator/health
curl -H "Host: api.car-rental.local" http://localhost:80/car-models
```

#### 2. Via Istio (Internal Communication)
```bash
# Test from Angular pod to carRental (goes through Istio mesh)
kubectl exec -n rental-service deploy/frontend-angular -c frontend-angular -- \
  wget -qO- http://carrental-service:8080/actuator/health

# Check Envoy sidecar logs
kubectl logs -n rental-service deploy/carrental -c istio-proxy

# View Istio configuration for a pod
istioctl proxy-config routes deploy/carrental -n rental-service
```

#### 3. Istio Observability

```bash
# Check mTLS status
kubectl get peerauthentication -n rental-service

# View traffic policies
kubectl get destinationrule carrental-destination -n rental-service -o yaml

# Analyze configuration
istioctl analyze -n rental-service

# Check proxy status
istioctl proxy-status
```

#### 4. Port Forward (Direct Access)
```bash
# Forward carRental (bypass both Ingress and Istio)
kubectl port-forward service/carrental-service 8081:8080 -n rental-service
curl http://localhost:8081/actuator/health
```

### Monitoring Architecture

```bash
# Complete status overview
./k8s/monitor-istio.sh

# Watch pod status in real-time
watch -n 2 'kubectl get pods -n rental-service'

# Check Istio system health
kubectl get pods -n istio-system

# View Ingress status
kubectl get ingress -n rental-service
```

## 🌐 Application Access

### Add to /etc/hosts
```bash
echo "127.0.0.1 car-rental.local api.car-rental.local grpc.car-rental.local" | sudo tee -a /etc/hosts
```

### Available Endpoints

| URL | Purpose | Routing | Target Service |
|-----|---------|---------|----------------|
| `http://car-rental.local/` | 🎨 Main Application | NGINX Ingress | Angular Frontend |
| `http://car-rental.local/direct-api/` | 🔧 Direct API | NGINX Ingress | carRental Backend |
| `http://api.car-rental.local/` | 🌐 API Subdomain | NGINX Ingress | carRental Backend |
| Internal: `carrental-service:8080` | ⚡ Internal API | **Istio Gateway** | carRental Backend |

### Sample API Calls

```bash
# External access via Ingress NGINX
curl -H "Host: api.car-rental.local" http://localhost:80/car-models
curl -H "Host: api.car-rental.local" http://localhost:80/actuator/health

# Internal access via Istio (from within cluster)
kubectl run test --rm -it --image=curlimages/curl --restart=Never -n rental-service -- \
  curl carrental-service:8080/actuator/health
```

## 🔐 Security & mTLS

### Current Configuration: PERMISSIVE mTLS

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default-mtls
  namespace: rental-service
spec:
  mtls:
    mode: PERMISSIVE  # Allows both mTLS and plaintext
```

**Why PERMISSIVE?**
- NGINX Ingress → Services: Uses plaintext (no Istio sidecar in Ingress)
- Frontend → carRental: Uses mTLS (both have Istio sidecars)
- carRental → auctionService: Uses mTLS (both have Istio sidecars)
- carRental → PostgreSQL: Uses mTLS (both have Istio sidecars)

### Upgrade to STRICT mTLS (Optional)

For full security, migrate NGINX Ingress to Istio Gateway:

```bash
# 1. Update to STRICT mode
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default-mtls
  namespace: rental-service
spec:
  mtls:
    mode: STRICT
EOF

# 2. Use Istio Gateway instead of NGINX Ingress
# See k8s/istio-gateway.yaml for full example
```

## 📊 Architecture Comparison

### Before Istio (Ingress-only)
```
Browser → NGINX Ingress → Frontend → carRental → PostgreSQL
                      ↘ carRental → auctionService
```
- Simple routing
- No service mesh features
- No automatic mTLS
- Basic load balancing

### After Istio (Hybrid)
```
Browser → NGINX Ingress → Frontend ─┐
                                    ↓
                            Istio Gateway → carRental → PostgreSQL
                                          ↘ auctionService
```
- Advanced routing (retry, circuit breaker, timeout)
- Automatic mTLS between services
- Intelligent load balancing
- Traffic metrics and distributed tracing
- Canary deployments ready

## 🚀 Next Steps

### Current Status ✅
1. ✅ Istio Service Mesh installed
2. ✅ All services with Envoy sidecars (2/2 containers)
3. ✅ PERMISSIVE mTLS (hybrid compatibility)
4. ✅ NGINX Ingress for external access
5. ✅ Istio Gateway for internal routing

### Available Enhancements 🔧
1. **Observability Stack**
   ```bash
   # Install Kiali, Prometheus, Grafana
   kubectl apply -f istio-1.23.2/samples/addons
   istioctl dashboard kiali
   ```

2. **Strict mTLS** (Full security)
   - Migrate external access to Istio Gateway
   - Enable STRICT mTLS mode

3. **Traffic Management**
   - Canary deployments (90/10 traffic split)
   - A/B testing
   - Fault injection for chaos engineering

4. **Circuit Breaker Testing**
   ```bash
   # Generate load to test circuit breaker
   kubectl run load-test --rm -it --image=fortio/fortio --restart=Never -n rental-service -- \
     load -c 50 -qps 0 -n 1000 http://carrental-service:8080/actuator/health
   ```

5. **Distributed Tracing**
   - Deploy Jaeger
   - View request traces across services

## 🛠️ Troubleshooting

### Istio Sidecar Not Injected
```bash
# Verify namespace label
kubectl get namespace rental-service --show-labels

# Should show: istio-injection=enabled
# If not:
kubectl label namespace rental-service istio-injection=enabled

# Restart deployments
kubectl rollout restart deployment -n rental-service
```

### mTLS Connection Issues
```bash
# Check mTLS status
kubectl get peerauthentication -n rental-service

# Test connection
kubectl exec -n rental-service deploy/frontend-angular -c frontend-angular -- \
  wget -qO- http://carrental-service:8080/actuator/health

# View Envoy proxy logs
kubectl logs -n rental-service deploy/carrental -c istio-proxy --tail=50
```

### Ingress 502 Bad Gateway
```bash
# Check if mTLS is STRICT (should be PERMISSIVE for hybrid)
kubectl get peerauthentication default-mtls -n rental-service -o yaml

# If STRICT, change to PERMISSIVE
kubectl patch peerauthentication default-mtls -n rental-service \
  --type='json' -p='[{"op": "replace", "path": "/spec/mtls/mode", "value":"PERMISSIVE"}]'
```

### Pod CrashLoopBackOff
```bash
# Check both containers (app + istio-proxy)
kubectl logs <pod-name> -n rental-service -c <container-name>
kubectl logs <pod-name> -n rental-service -c istio-proxy

# Describe pod for events
kubectl describe pod <pod-name> -n rental-service
```

### Istio Configuration Errors
```bash
# Analyze configuration
istioctl analyze -n rental-service

# Validate specific resource
istioctl validate -f k8s/istio-internal-gateway.yaml
```

## 🧹 Cleanup

```bash
# Remove application resources
kubectl delete namespace rental-service

# Uninstall Istio
istioctl uninstall --purge -y

# Delete Kind cluster
kind delete cluster --name rental-service-cluster
```

## 📚 Additional Resources

- [Istio Documentation](https://istio.io/latest/docs/)
- [Istio Traffic Management](https://istio.io/latest/docs/tasks/traffic-management/)
- [Istio Security](https://istio.io/latest/docs/tasks/security/)
- [Istio Observability](https://istio.io/latest/docs/tasks/observability/)
- [NGINX Ingress + Istio](https://istio.io/latest/docs/tasks/traffic-management/ingress/ingress-control/)

---

**Architecture Status**: ✅ **PRODUCTION-READY**
- Hybrid routing (NGINX Ingress + Istio Gateway)
- Automatic mTLS between services
- Advanced traffic policies
- Full observability ready
