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

4. Install NGINX Ingress and patch to run on control-plane:
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=90s

# Patch NGINX controller to run on control-plane (where Kind port mapping is configured)
kubectl patch deployment ingress-nginx-controller -n ingress-nginx -p '{
  "spec": {
    "template": {
      "spec": {
        "nodeSelector": {"ingress-ready": "true"},
        "tolerations": [
          {"key": "node-role.kubernetes.io/control-plane", "operator": "Equal", "effect": "NoSchedule"},
          {"key": "node-role.kubernetes.io/master", "operator": "Equal", "effect": "NoSchedule"}
        ]
      }
    }
  }
}'
kubectl rollout status deployment ingress-nginx-controller -n ingress-nginx --timeout=90s
```

**Why?** Kind's port mapping (80:80, 443:443) is configured on the control-plane node. NGINX must run there to access localhost ports.

5. Add to `/etc/hosts`:
```
127.0.0.1 car-rental.local
```

### For Minikube

1. Start Minikube:
```bash
minikube start --cpus=4 --memory=7800
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

## Label Architecture

### Kustomize commonLabels and Label Overwrites

The `kustomization.yaml` files define `commonLabels: app=rental-service` which is applied to **all resources**. However, Kustomize overwrites pod template labels, causing issues with service selectors that rely on specific `app` labels.

**Problem:** If a deployment defines `app: carrental` in its pod template, Kustomize changes it to `app: rental-service`.

**Solution:** Use dedicated labels that won't be overwritten:

```yaml
# Deployment pod template
metadata:
  labels:
    app: rental-service          # Overwritten by commonLabels (expected)
    service-name: carrental       # NOT overwritten - used for service selector
    component: backend            # NOT overwritten - used for kubectl wait
```

### Service Selector Strategy

Services use `service-name` label to select their pods:

```yaml
# carrental-service.yaml
spec:
  selector:
    service-name: carrental    # Matches service-name label in pod template
```

**Available service-name values:**
- `carrental` - carRental backend
- `frontend` - Angular frontend
- `auction` - auction service
- `postgres` - PostgreSQL database

### Component-Based Selectors for Operations

For kubectl wait commands and operational tasks, use `component` label:

```bash
# Wait for backend pods
kubectl wait --for=condition=ready pod -l component=backend -n rental-service --timeout=600s

# Wait for frontend pods
kubectl wait --for=condition=ready pod -l component=frontend -n rental-service --timeout=300s

# Wait for database
kubectl wait --for=condition=ready pod -l component=database -n rental-service --timeout=300s
```

**Available component values:**
- `backend` - carRental and auction services
- `frontend` - Angular frontend
- `database` - PostgreSQL

## Istio Configuration

Both environments share the same Istio configuration:
- Gateway for internal routing
- VirtualService with retry and timeout policies
- DestinationRule with load balancing and circuit breaker
- PeerAuthentication with PERMISSIVE mTLS
- **Automatic sidecar injection** enabled via namespace label `istio-injection=enabled`
- **Postgres excluded from Istio** via annotation `sidecar.istio.io/inject: "false"`

Application pods run with 2 containers (app + istio-proxy), postgres runs with 1 container:
- **carRental**: 2/2 (carrental + istio-proxy)
- **frontend**: 2/2 (frontend-angular + istio-proxy)
- **auction**: 2/2 (auction-service-server + istio-proxy)
- **postgres**: 1/1 (postgres only, no sidecar)

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

You should see 2 containers per application pod (app + istio-proxy), but only 1 for postgres:

Expected output:
```
auction-service-server-xxx    auction-service-server istio-proxy
carrental-xxx                 carrental istio-proxy
frontend-angular-xxx          frontend-angular istio-proxy
postgres-0                    postgres
```

Verify labels:
```bash
kubectl get pods -n rental-service --show-labels
```

You should see labels like:
- `app=rental-service` (common to all)
- `service-name=carrental|frontend|auction|postgres` (specific)
- `component=backend|frontend|database` (operational)

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

### Services not routing correctly

If services can't find their pods, check that pods have the correct `service-name` labels:

1. Check pod labels:
```bash
kubectl get pods -n rental-service --show-labels
```

2. Verify each pod has its specific `service-name` label:
- carRental pods: `service-name=carrental`
- Frontend pods: `service-name=frontend`
- Auction pods: `service-name=auction`
- Postgres pods: `service-name=postgres`

3. If labels are missing, redeploy:
```bash
kubectl apply -k overlays/kind  # or overlays/minikube
```

**Note:** The `app=rental-service` label is normal and expected (from Kustomize commonLabels). Services use `service-name` for pod selection.

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

## CI/CD Pipeline

This project includes a GitHub Actions CI/CD pipeline (`.github/workflows/ci.yml`) that automatically:

1. **Builds** Java and Node.js applications
2. **Creates** Docker images for all services
3. **Sets up** Kind cluster with Istio and NGINX Ingress
4. **Deploys** application using `deploy.sh` script
5. **Verifies** Istio sidecar injection and rollout status
6. **Tests** API endpoints via Ingress

The pipeline uses the same deployment workflow as local development, ensuring consistency between CI and manual deployments.

**Key CI configurations:**
- Uses Kind cluster (name: `kind`)
- Loads Docker images with `kind load docker-image`
- Verifies sidecars using `service-name` labels
- Tests endpoints via `curl -H "Host: car-rental.local" http://localhost:80/api/offers`

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
