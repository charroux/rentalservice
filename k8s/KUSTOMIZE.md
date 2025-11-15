# Kustomize Structure

This project uses Kustomize to manage Kubernetes configurations for multiple environments.

## Directory Structure

```
k8s/
├── base/                           # Common resources (shared)
│   ├── namespace.yaml
│   ├── postgres-secret.yaml
│   ├── postgres-statefulset.yaml
│   ├── carrental-deployment.yaml
│   ├── frontend-deployment.yaml
│   ├── auction-deployment.yaml
│   ├── istio-internal-gateway.yaml
│   └── kustomization.yaml
│
├── overlays/
│   ├── kind/                       # Kind-specific configuration
│   │   ├── ingress-kind.yaml
│   │   └── kustomization.yaml
│   │
│   └── minikube/                   # Minikube-specific configuration
│       ├── ingress-minikube.yaml
│       └── kustomization.yaml
│
└── deploy.sh                       # Automated deployment script
```

## Usage

### Automated Deployment (Recommended)

The `deploy.sh` script automatically detects your environment (Kind or Minikube) and deploys accordingly:

```bash
cd k8s
./deploy.sh
```

### Manual Deployment

#### Deploy to Kind

```bash
kubectl apply -k overlays/kind
```

#### Deploy to Minikube

```bash
kubectl apply -k overlays/minikube
```

## Prerequisites

### For Kind

1. Create Kind cluster with port mapping:
```bash
kind create cluster --config kind-config.yaml --name rental-service-cluster
```

2. Install Istio:
```bash
istioctl install --set profile=demo -y
```

3. **Create namespace and enable Istio sidecar injection:**
```bash
kubectl create namespace rental-service
kubectl label namespace rental-service istio-injection=enabled
```

4. Install NGINX Ingress:
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
```

5. Add to `/etc/hosts`:
```
127.0.0.1 car-rental.local
```

### For Minikube

1. Start Minikube:
```bash
minikube start --cpus=4 --memory=8192
```

2. Enable addons:
```bash
minikube addons enable ingress
minikube addons enable metrics-server
```

3. Install Istio:
```bash
istioctl install --set profile=demo -y
```

4. **Create namespace and enable Istio sidecar injection:**
```bash
kubectl create namespace rental-service
kubectl label namespace rental-service istio-injection=enabled
```

5. **Build Docker images in Minikube's Docker daemon:**
```bash
# Configure your shell to use Minikube's Docker
eval $(minikube docker-env)

# Build all images
docker build -f carRental/Dockerfile -t carrental:latest .
docker build -f auctionServiceServer/Dockerfile -t auction-service-server:latest .
docker build -f car-rental-angular/Dockerfile -t car-rental-angular:latest .
```

6. **Configure Ingress as LoadBalancer for tunnel:**
```bash
# Patch the Ingress controller to use LoadBalancer type (required for minikube tunnel)
kubectl patch svc ingress-nginx-controller -n ingress-nginx -p '{"spec":{"type":"LoadBalancer"}}'
```

7. Start tunnel (in separate terminal):
```bash
minikube tunnel
```

8. Add to `/etc/hosts`:
```
127.0.0.1 car-rental.local
```

**Note:** With `minikube tunnel` and LoadBalancer type, the Ingress gets `127.0.0.1` as EXTERNAL-IP, making it accessible via localhost.

### Alternative: NodePort Access (No Tunnel, No /etc/hosts)

If you don't want to use `minikube tunnel` or modify `/etc/hosts`, you can access services via NodePort:

```bash
# Get the NodePort for Ingress
kubectl get svc -n ingress-nginx ingress-nginx-controller

# Access with Minikube IP and NodePort (example: port 31076)
MINIKUBE_IP=$(minikube ip)
NODE_PORT=$(kubectl get svc -n ingress-nginx ingress-nginx-controller -o jsonpath='{.spec.ports[0].nodePort}')

# Test API
curl -H "Host: car-rental.local" http://${MINIKUBE_IP}:${NODE_PORT}/api/offers

# Access frontend in browser
echo "Open: http://${MINIKUBE_IP}:${NODE_PORT}/ (with Host header extension or configure app)"
```

**Note:** For browser access, you'll need to either:
- Use a browser extension to set the `Host: car-rental.local` header
- Or modify the Angular app to work without virtual host

### Alternative: Port Forwarding (Simplest for Development)

Access services directly without Ingress:

```bash
# Forward carrental service
kubectl port-forward -n rental-service svc/carrental-service 8080:8080

# Forward frontend service
kubectl port-forward -n rental-service svc/frontend-service 4200:80

# Access in browser
# API: http://localhost:8080/offers
# Frontend: http://localhost:4200
```

**Pros:** No tunnel, no /etc/hosts, simple
**Cons:** Bypasses Ingress and Istio Gateway (but services still communicate via Istio internally)

## Differences Between Environments

### Kind
- Uses extraPortMappings to expose ports 80/443 to localhost
- NGINX Ingress installed manually
- Access via `localhost` with host header
- **Images:** `imagePullPolicy: Never` - images must be loaded into Kind with `kind load docker-image`
- **Replicas:** 1 per deployment (suitable for local development)

### Minikube
- Uses `minikube tunnel` for LoadBalancer services
- NGINX Ingress via addon (default: NodePort, must be patched to LoadBalancer)
- Access via `127.0.0.1` (with tunnel and LoadBalancer patch)
- **Images:** `imagePullPolicy: IfNotPresent` - images must be built in Minikube's Docker daemon with `eval $(minikube docker-env)`
- **Replicas:** 1 per deployment (optimized for limited Minikube resources)
- **Important:** Ingress controller must be changed to LoadBalancer type for tunnel to work

## Istio Configuration

Both environments share the same Istio configuration:
- Gateway for internal routing
- VirtualService with retry and timeout policies
- DestinationRule with load balancing and circuit breaker
- PeerAuthentication with PERMISSIVE mTLS
- **Automatic sidecar injection** enabled via namespace label `istio-injection=enabled`

All application pods run with 2 containers:
- Main application container
- Istio Envoy sidecar proxy

## Access URLs

After deployment, access your application at:
- Frontend: http://car-rental.local
- API: http://car-rental.local/api/
- Direct API: http://car-rental.local/direct-api/

## Verification

Check deployment status:
```bash
kubectl get pods -n rental-service
kubectl get ingress -n rental-service
kubectl get gateway,virtualservice,destinationrule -n rental-service
```

Check Istio sidecar injection:
```bash
kubectl get pods -n rental-service -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[*].name}{"\n"}{end}'
```

You should see 2 containers per pod: `<app-name>` and `istio-proxy`.

Expected output:
```
auction-service-server-xxx    auction-service-server istio-proxy
carrental-xxx                 carrental istio-proxy
frontend-angular-xxx          frontend-angular istio-proxy
postgres-0                    postgres
```

## Troubleshooting

### Pods not ready (0/2 or 1/2)

If pods show READY 1/2, the Istio sidecar might not be injected:

1. Check namespace label:
```bash
kubectl get namespace rental-service --show-labels
```

2. Add label if missing:
```bash
kubectl label namespace rental-service istio-injection=enabled --overwrite
```

3. Restart deployments:
```bash
kubectl rollout restart deployment -n rental-service
```

### Pods have wrong labels (app=rental-service instead of specific app names)

If services don't route correctly, pods might have generic labels instead of specific ones:

1. Check pod labels:
```bash
kubectl get pods -n rental-service --show-labels
```

2. If pods have `app=rental-service` instead of `app=carrental`, `app=frontend-angular`, etc., delete and recreate:
```bash
# Delete deployments with wrong labels
kubectl delete deployment carrental auction-service-server frontend-angular -n rental-service

# Recreate with correct labels from base manifests
kubectl apply -f k8s/base/carrental-deployment.yaml
kubectl apply -f k8s/base/auction-deployment.yaml
kubectl apply -f k8s/base/frontend-deployment.yaml
```

### Ingress controller not accessible with minikube tunnel

If you can't access services via `127.0.0.1` even with tunnel running:

1. Check Ingress controller type:
```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller
```

2. If TYPE is NodePort instead of LoadBalancer, patch it:
```bash
kubectl patch svc ingress-nginx-controller -n ingress-nginx -p '{"spec":{"type":"LoadBalancer"}}'
```

3. Restart tunnel:
```bash
pkill -f "minikube tunnel"
sudo minikube tunnel
```

4. Verify EXTERNAL-IP is `127.0.0.1`:
```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller
```

### Ingress paths with regex not working

If you get errors like "path /api(/|$)(.*) cannot be used with pathType Prefix":

- Change `pathType: Prefix` to `pathType: ImplementationSpecific` for paths with regex patterns
- This is required for paths like `/api(/|$)(.*)` with rewrite-target annotation

### Java apps take long to start

The deployment probes are configured with extended delays for Java applications with Istio:
- Liveness probe: 120s initial delay
- Readiness probe: 90s initial delay
- Failure threshold: 6 attempts

This is normal for Spring Boot applications with Istio sidecars and PostgreSQL initialization.

## Cluster Management

### Stop Minikube

To stop the Minikube cluster without deleting it:
```bash
minikube stop
```

To start it again later:
```bash
minikube start
```

### Delete Minikube

To completely delete the Minikube cluster:
```bash
minikube delete
```

### Stop Kind

To stop the Kind cluster, you need to delete it (Kind doesn't have a stop/start mechanism):
```bash
kind delete cluster --name rental-service-cluster
```

To recreate it, use the setup script:
```bash
cd k8s
./setup-kind-cluster.sh
```
