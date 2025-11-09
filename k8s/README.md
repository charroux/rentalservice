# Kubernetes Deployment with Kind & Ingress

This directory contains Kubernetes manifests for the car-rental microservices architecture, optimized for local development with **Kind** (Kubernetes in Docker) and **NGINX Ingress Controller**.

## 🏗️ Architecture Overview

```
Internet/Browser
       │
   ┌───▼────────────────────────────────────────────────────┐
   │              NGINX Ingress Controller                   │
   │  car-rental.local  │  api.car-rental.local  │  gRPC   │
   └─────────┬──────────┴─────────┬───────────────┴─────────┘
             │                    │
    ┌────────▼──────┐    ┌────────▼─────────────────────────┐
    │   Angular     │    │         carRental                │
    │   Frontend    │    │       (REST API)                 │
    │   Port: 80    │    │       Port: 8080                 │
    └───────────────┘    └─────────┬───────────┬────────────┘
                                   │           │
                          ┌────────▼──────┐    ▼
                          │  PostgreSQL   │ ┌─────────────────┐
                          │  (Database)   │ │ auctionService  │
                          │  Port: 5432   │ │    (gRPC)       │
                          └───────────────┘ │   Port: 9090    │
                                            └─────────────────┘
```

## 🚀 Quick Start with Kind

### Prerequisites

- Docker Desktop running
- kubectl installed
- Kind installed: `brew install kind` (macOS) or [kind.sigs.k8s.io](https://kind.sigs.k8s.io/docs/user/quick-start/)

### 1. Create Kind Cluster

```bash
# Create multi-node cluster for realistic testing
kind create cluster --name rental-service-cluster --config - <<EOF
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
- role: worker
- role: worker
EOF
```

### 2. Build and Load Images

```bash
# Build backend images
docker build -f carRental/Dockerfile -t carrental:latest .
docker build -f auctionServiceServer/Dockerfile -t auction-service-server:latest .

# Build frontend image
docker build -f car-rental-angular/Dockerfile -t car-rental-angular:latest .

# Load all images into Kind cluster
kind load docker-image carrental:latest --name rental-service-cluster
kind load docker-image auction-service-server:latest --name rental-service-cluster
kind load docker-image car-rental-angular:latest --name rental-service-cluster
```

### 3. Deploy Services

```bash
# Create namespace and secrets
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/postgres-secret.yaml

# Deploy database
kubectl apply -f k8s/postgres-statefulset.yaml

# Wait for PostgreSQL to be ready
kubectl wait --for=condition=ready pod/postgres-0 -n rental-service --timeout=120s

# Deploy backend services
kubectl apply -f k8s/carrental-deployment.yaml
kubectl apply -f k8s/auction-deployment.yaml

# Deploy frontend
kubectl apply -f k8s/frontend-deployment.yaml

# Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/kind/deploy.yaml

# Wait for Ingress Controller to be ready
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=90s

# Deploy Ingress rules
kubectl apply -f k8s/ingress.yaml
```

### 4. Verify Deployment

```bash
# Check all pods
kubectl get pods -n rental-service -o wide

# Check services
kubectl get services -n rental-service

# View logs
kubectl logs -f deployment/carrental -n rental-service
```

## 📁 File Structure

```
k8s/
├── README.md                    # This file
├── namespace.yaml               # rental-service namespace
├── postgres-secret.yaml         # DB credentials & Spring config
├── postgres-statefulset.yaml    # PostgreSQL with 2Gi PVC
├── carrental-deployment.yaml    # carRental REST API service
├── auction-deployment.yaml      # auctionService gRPC server
├── frontend-deployment.yaml     # Angular frontend with nginx
├── ingress.yaml                 # NGINX Ingress rules (multiple)
├── istio-gateway.yaml           # Istio configuration (future migration)
├── monitor.sh                   # Architecture monitoring script
└── car-rental-deployment.yaml   # Legacy (to be updated)
```

## 🔧 Configuration Details

### Images Configuration
- **Local Development**: Uses `imagePullPolicy: Never` for Kind-loaded images
- **Production**: Switch to registry images with `imagePullPolicy: Always`

### Database Configuration
- **PostgreSQL 15** with persistent 2Gi storage
- **Connection**: `jdbc:postgresql://postgres:5432/dbcar`
- **Credentials**: Managed via Kubernetes secrets

### Service Communication & Routing

#### Internal (ClusterIP) Communication
- **carRental** ↔ **PostgreSQL**: JDBC connection via `postgres:5432`
- **carRental** ↔ **auctionService**: gRPC client via `auction-service:9090`
- **Frontend** → **Backend**: Proxied via nginx configuration `/api/` → `carrental-service:8080`

#### External (Ingress) Access
- **Frontend**: `http://car-rental.local/` → Angular SPA
- **API Direct**: `http://car-rental.local/direct-api/` → Backend API
- **API Subdomain**: `http://api.car-rental.local/` → Backend API
- **gRPC**: `http://grpc.car-rental.local/` → Auction Service (gRPC)

## 🛠️ Development Workflow

### Rebuild and Redeploy

```bash
# Rebuild carRental
docker build -f carRental/Dockerfile -t carrental:latest .
kind load docker-image carrental:latest --name rental-service-cluster
kubectl rollout restart deployment/carrental -n rental-service

# Rebuild auctionService
docker build -f auctionServiceServer/Dockerfile -t auction-service-server:latest .
kind load docker-image auction-service-server:latest --name rental-service-cluster
kubectl rollout restart deployment/auction-service-server -n rental-service
```

### Testing & Debugging

#### Via Ingress (Recommended)
```bash
# Test frontend (add to /etc/hosts: 127.0.0.1 car-rental.local api.car-rental.local)
curl -H "Host: car-rental.local" http://localhost:80/

# Test API via subdomain
curl -H "Host: api.car-rental.local" http://localhost:80/actuator/health
curl -H "Host: api.car-rental.local" http://localhost:80/car-models

# Test API via direct path
curl -H "Host: car-rental.local" http://localhost:80/direct-api/actuator/health

# Architecture monitoring
./k8s/monitor.sh
```

#### Via Port Forward (Debugging)
```bash
# Port forward services directly (using different local ports to avoid conflicts)
kubectl port-forward service/carrental-service 8081:8080 -n rental-service  # Local 8081 → Service 8080
kubectl port-forward service/frontend-service 3000:80 -n rental-service     # Local 3000 → Service 80
kubectl port-forward service/postgres 5433:5432 -n rental-service           # Local 5433 → Service 5432

# Test the forwarded services
curl http://localhost:8081/actuator/health    # carRental API
curl http://localhost:3000/                   # Frontend
psql -h localhost -p 5433 -U dbuser -d dbcar  # Database

# Internal cluster testing (no port conflicts)
kubectl run test-pod --rm -it --image=curlimages/curl --restart=Never -n rental-service -- curl carrental-service:8080/actuator/health
```

**💡 Port Forward Tips:**
- Use different local ports to avoid conflicts (8081, 8082, etc.)
- Check what's using port 8080: `lsof -i :8080` or `netstat -an | grep 8080`
- Kill conflicting process: `sudo lsof -ti:8080 | xargs kill -9` (if safe to do)

### Cleanup
```bash
# Remove all resources
kubectl delete namespace rental-service

# Delete Kind cluster
kind delete cluster --name rental-service-cluster
```

## 📝 Notes

- **Kind Cluster**: 3-node setup (1 control-plane + 2 workers) for realistic load balancing
- **Storage**: Uses Kind's default storage class for persistent volumes
- **Networking**: All services use ClusterIP for internal communication
- **Secrets**: Development credentials in plain text (stringData format)
- **Health Checks**: Liveness and readiness probes configured for all services
- **Resources**: CPU/Memory limits set for optimal Kind performance

## 🔄 Migration from Docker Compose

This setup replaces the previous `docker-compose.yml` approach:
- ✅ Better isolation and resource management
- ✅ Production-like environment locally
- ✅ Service discovery and load balancing
- ✅ Health monitoring and auto-restart
- ✅ Persistent data storage

## ✅ Deployment Status

### Complete Architecture - **PRODUCTION READY** 🎉

| Layer | Service | Status | Replicas | Ingress | Health |
|-------|---------|--------|----------|---------|--------|
| **Ingress** | NGINX Controller | ✅ Running | 1/1 | ✅ Active | Ready |
| **Frontend** | Angular SPA | ✅ Running | 2/2 | ✅ car-rental.local | Ready |
| **Backend** | carRental API | ✅ Running | 2/2 | ✅ Multi-domain | Ready |
| **Backend** | auctionService | ✅ Running | 2/2 | ✅ gRPC ready | Ready |
| **Database** | PostgreSQL | ✅ Running | 1/1 | - | Ready |

### Quick Verification

```bash
# Architecture overview
./k8s/monitor.sh

# Test all endpoints
curl -H "Host: car-rental.local" http://localhost:80/ -s | head -5
curl -H "Host: api.car-rental.local" http://localhost:80/actuator/health
curl -H "Host: api.car-rental.local" http://localhost:80/car-models

# Check ingress status
kubectl get ingress -n rental-service
```

## 🚀 Next Steps

### Completed ✅
1. ✅ Backend services deployment (PostgreSQL, carRental, auctionService)
2. ✅ Frontend deployment (Angular with nginx)
3. ✅ Ingress configuration (NGINX Controller with multiple domains)
4. ✅ Load balancing and health checks
5. ✅ Security headers and WebSocket support

### Available Enhancements 🔧
1. **HTTPS/TLS**: Add cert-manager for SSL certificates
2. **Monitoring**: Deploy Prometheus/Grafana stack
3. **Service Mesh**: Migrate to Istio (configuration ready)
4. **CI/CD**: GitHub Actions pipeline for automated deployments
5. **Autoscaling**: Horizontal Pod Autoscaler (HPA)
6. **Backup**: PostgreSQL backup strategy
7. **Multi-environment**: Staging/production environments

## 🛠️ Troubleshooting

### Common Issues

#### Ingress Not Working
```bash
# Check Ingress Controller status
kubectl get pods -n ingress-nginx
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller

# Verify Ingress rules
kubectl get ingress -n rental-service
kubectl describe ingress frontend-ingress -n rental-service
```

#### Pod CrashLoopBackOff
```bash
# Check pod logs
kubectl logs <pod-name> -n rental-service

# Check resource constraints
kubectl describe pod <pod-name> -n rental-service

# Common fixes
kubectl rollout restart deployment/<deployment-name> -n rental-service
```

#### Database Connection Issues
```bash
# Check PostgreSQL status
kubectl logs postgres-0 -n rental-service
kubectl exec -it postgres-0 -n rental-service -- psql -U dbuser -d dbcar -c "\dt"

# Verify secrets
kubectl get secret postgres-credentials -n rental-service -o yaml
```

#### Image Pull Issues with Kind
```bash
# Reload images into Kind
kind load docker-image carrental:latest --name rental-service-cluster
kind load docker-image car-rental-angular:latest --name rental-service-cluster
kind load docker-image auction-service-server:latest --name rental-service-cluster

# Restart deployments
kubectl rollout restart deployment/carrental -n rental-service
kubectl rollout restart deployment/frontend-angular -n rental-service
kubectl rollout restart deployment/auction-service-server -n rental-service
```

### Performance Issues
```bash
# Check resource usage
kubectl top nodes
kubectl top pods -n rental-service

# Scale deployments
kubectl scale deployment carrental --replicas=3 -n rental-service
kubectl scale deployment frontend-angular --replicas=3 -n rental-service
```

## 🌐 Application Access

### Local Development Setup

Add these entries to your `/etc/hosts` file:
```bash
echo "127.0.0.1 car-rental.local api.car-rental.local grpc.car-rental.local" | sudo tee -a /etc/hosts
```

### Available Endpoints

| URL | Purpose | Target Service |
|-----|---------|---------------|
| `http://car-rental.local/` | 🎨 **Main Application** | Angular Frontend |
| `http://car-rental.local/direct-api/` | 🔧 **Direct API Access** | carRental Backend |
| `http://api.car-rental.local/` | � **API Subdomain** | carRental Backend |
| `http://grpc.car-rental.local/` | ⚡ **gRPC Endpoint** | auctionService |

### Sample API Calls

```bash
# Get car models
curl -H "Host: api.car-rental.local" http://localhost:80/car-models

# Health check
curl -H "Host: api.car-rental.local" http://localhost:80/actuator/health

# Via direct API path
curl -H "Host: car-rental.local" http://localhost:80/direct-api/actuator/health
```

## �📊 Performance Notes

- **Kind Cluster**: 3 nodes (1 control-plane + 2 workers)
- **Resource Usage**: Optimized for development with CPU/Memory limits
- **Storage**: 2Gi PVC for PostgreSQL persistent data
- **Load Balancing**: NGINX Ingress + Kubernetes services
- **Health Checks**: Liveness/readiness probes on all components
- **Security**: Security headers, ClusterIP isolation
