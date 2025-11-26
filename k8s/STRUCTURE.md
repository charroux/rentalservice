# Structure du projet k8s/

## 📁 Organisation des fichiers

```
k8s/
├── 📂 base/                              # Ressources communes aux deux environnements
│   ├── auction-deployment.yaml           # Déploiement gRPC auction service
│   ├── carrental-deployment.yaml         # Déploiement REST backend
│   ├── frontend-deployment.yaml          # Déploiement Angular frontend
│   ├── istio-internal-gateway.yaml       # Gateway/VirtualService/DestinationRule Istio
│   ├── kustomization.yaml                # Configuration Kustomize base
│   ├── namespace.yaml                    # Namespace rental-service
│   ├── postgres-secret.yaml              # Secrets PostgreSQL
│   └── postgres-statefulset.yaml         # StatefulSet PostgreSQL
│
├── 📂 overlays/
│   ├── 📂 kind/                          # Configuration spécifique Kind
│   │   ├── ingress-kind.yaml            # 5 Ingress (frontend, api, direct-api, subdomain, grpc)
│   │   ├── kustomization.yaml           # Kustomize overlay Kind
│   │   └── patches/
│   │       └── service-nodeport.yaml    # Patches services (actuellement vide)
│   │
│   └── 📂 minikube/                      # Configuration spécifique Minikube
│       ├── ingress-minikube.yaml        # 3 Ingress (frontend, api, direct-api)
│       └── kustomization.yaml           # Kustomize overlay Minikube
│
├── 📄 deploy.sh                          # Script déploiement auto-détection Kind/Minikube
├── 📄 setup-kind-cluster.sh              # Script setup complet cluster Kind
├── 📄 setup-minikube-cluster.sh          # Script setup complet cluster Minikube
├── 📄 monitor.sh                         # Script monitoring (sans Istio)
├── 📄 monitor-istio.sh                   # Script monitoring avec Istio
│
├── 📄 kind-config.yaml                   # Config Kind cluster (3 nœuds + port mapping)
├── 📄 kind-config-simple.yaml            # Config Kind cluster (simple, 1 nœud)
│
├── 📄 KUSTOMIZE.md                       # Documentation Kustomize
└── 📄 README.md                          # Documentation principale (Istio + Ingress)
```

## 📊 Ressources déployées

### Base (communes aux 2 environnements)
- ✅ 1 Namespace (rental-service)
- ✅ 1 Secret (postgres-credentials)
- ✅ 4 Services (postgres, carrental-service, auction-service, frontend-service)
- ✅ 1 StatefulSet (postgres)
- ✅ 3 Deployments (carrental, frontend-angular, auction-service-server) - **1 replica each**
- ✅ 1 Gateway Istio (rental-internal-gateway)
- ✅ 1 VirtualService Istio (carrental-internal-vs)
- ✅ 1 DestinationRule Istio (carrental-destination)
- ✅ 1 PeerAuthentication Istio (default-mtls PERMISSIVE)

### Overlay Kind
- ✅ 5 Ingress NGINX
  - frontend-ingress (/)
  - backend-api-ingress (/api/*)
  - backend-direct-api-ingress (/direct-api/*)
  - api-subdomain-ingress (api.car-rental.local)
  - grpc-ingress (grpc.car-rental.local)

### Overlay Minikube
- ✅ 3 Ingress NGINX
  - frontend-ingress (/)
  - backend-api-ingress (/api/*)
  - backend-direct-api-ingress (/direct-api/*)

## 🚀 Setup complet (depuis zéro)

### Option 1: Setup automatique Kind
```bash
cd k8s
./setup-kind-cluster.sh    # Crée cluster + Istio + NGINX + MetalLB
./deploy.sh                 # Déploie l'application
```

### Option 2: Setup automatique Minikube
```bash
cd k8s
./setup-minikube-cluster.sh  # Crée cluster + Istio + addons

# Build des images dans le Docker de Minikube
eval $(minikube docker-env)
docker build -f carRental/Dockerfile -t carrental:latest .
docker build -f auctionServiceServer/Dockerfile -t auction-service-server:latest .
docker build -f car-rental-angular/Dockerfile -t car-rental-angular:latest .

# Patcher Ingress en LoadBalancer pour le tunnel
kubectl patch svc ingress-nginx-controller -n ingress-nginx -p '{"spec":{"type":"LoadBalancer"}}'

minikube tunnel              # Dans un autre terminal
./deploy.sh                  # Déploie l'application
```

## 🚀 Commandes de déploiement (cluster déjà existant)

```bash
# Déploiement automatique (détecte l'environnement)
./deploy.sh

# Déploiement manuel Kind
kubectl apply -k overlays/kind

# Déploiement manuel Minikube
kubectl apply -k overlays/minikube

# Tester la génération sans appliquer
kubectl kustomize overlays/kind
kubectl kustomize overlays/minikube
```

## 🔍 Différences Kind vs Minikube

| Aspect | Kind | Minikube |
|--------|------|----------|
| **Ingress** | 5 Ingress (tous les chemins) | 3 Ingress (essentiels) |
| **Accès** | localhost:80 via port mapping | 127.0.0.1:80 via tunnel + LoadBalancer |
| **NGINX** | Installation manuelle + patch control-plane | Addon intégré (NodePort par défaut) |
| **Control-Plane** | NGINX doit tourner sur control-plane pour accéder au port mapping | N/A |
| **Hosts** | 127.0.0.1 car-rental.local | 127.0.0.1 car-rental.local |
| **Images** | imagePullPolicy: Never | imagePullPolicy: IfNotPresent |
| **Build** | kind load docker-image | eval $(minikube docker-env) |
| **Replicas** | 1 per deployment | 1 per deployment |
| **Probes** | initialDelaySeconds: 120s (liveness), 90s (readiness) | Same |
| **LoadBalancer** | Not needed (port mapping) | Required patch for Ingress controller |

## 🔧 Configuration Istio

Tous les pods d'application (sauf postgres) exécutent **2 containers** :
- Container principal de l'application
- Sidecar Istio Envoy (istio-proxy)

**Postgres est exclu** de l'injection Istio via l'annotation `sidecar.istio.io/inject: "false"` et ne contient qu'**1 container**.

Configuration partagée :
- ✅ Gateway Istio pour le routage interne
- ✅ VirtualService avec retry (3 tentatives, 10s) et timeout (30s API, 5s health)
- ✅ DestinationRule avec LEAST_REQUEST load balancing et circuit breaker
- ✅ PeerAuthentication en mode PERMISSIVE (mTLS optionnel)
- ✅ Label `istio-injection=enabled` sur namespace rental-service
- ✅ Annotation `sidecar.istio.io/inject: "false"` sur postgres StatefulSet

**Counts de containers attendus:**
- carRental: 2/2 (carrental + istio-proxy)
- frontend: 2/2 (frontend-angular + istio-proxy)
- auction: 2/2 (auction-service-server + istio-proxy)
- postgres: 1/1 (postgres uniquement, pas de sidecar)

## 🏷️ Architecture des Labels

### Kustomize commonLabels et Réécriture

Les fichiers `kustomization.yaml` définissent `commonLabels: app=rental-service` qui est appliqué à **toutes les ressources**. Cependant, Kustomize réécrit les labels des pod templates, ce qui cause des problèmes avec les sélecteurs de services.

**Problème:** Si un deployment définit `app: carrental` dans son pod template, Kustomize le change en `app: rental-service`.

**Solution:** Utiliser des labels dédiés qui ne seront pas réécrits :

```yaml
# Pod template d'un deployment
metadata:
  labels:
    app: rental-service          # Réécrit par commonLabels (attendu)
    service-name: carrental       # NON réécrit - utilisé par le sélecteur de service
    component: backend            # NON réécrit - utilisé pour kubectl wait
```

### Stratégie de Sélection des Services

Les services utilisent le label `service-name` pour sélectionner leurs pods :

```yaml
# carrental-service.yaml
spec:
  selector:
    service-name: carrental    # Match le label service-name dans le pod template
```

**Valeurs service-name disponibles:**
- `carrental` - Backend carRental
- `frontend` - Frontend Angular
- `auction` - Service auction
- `postgres` - Base de données PostgreSQL

### Sélecteurs Basés sur component pour les Opérations

Pour les commandes `kubectl wait` et les tâches opérationnelles, utiliser le label `component` :

```bash
# Attendre les pods backend
kubectl wait --for=condition=ready pod -l component=backend -n rental-service --timeout=600s

# Attendre les pods frontend
kubectl wait --for=condition=ready pod -l component=frontend -n rental-service --timeout=300s

# Attendre la base de données
kubectl wait --for=condition=ready pod -l component=database -n rental-service --timeout=300s
```

**Valeurs component disponibles:**
- `backend` - Services carRental et auction
- `frontend` - Frontend Angular
- `database` - PostgreSQL

## 🔄 Pipeline CI/CD

Le projet inclut un pipeline CI/CD complet GitHub Actions (`.github/workflows/ci.yml`) :

### Étapes du Pipeline

1. **Build**
   - JDK 17 + Node.js 20
   - Gradle: `:carRental:bootJar`, `:auctionServiceServer:bootJar`
   - npm: build Angular frontend

2. **Docker**
   - Build images: carrental, auction-service-server, car-rental-angular
   - Contexte root avec flag `-f` pour les services Java
   - Tag `latest`

3. **Setup Cluster**
   - Kind cluster (nom: `kind`)
   - Istio installation
   - NGINX Ingress + patch control-plane
   - MetalLB pour LoadBalancer

4. **Déploiement**
   - `kind load docker-image` pour charger les images
   - Exécution de `deploy.sh` (même workflow que local)
   - Utilisation de `kubectl rollout status`

5. **Vérification**
   - Injection sidecars Istio avec labels `service-name`
   - Counts containers: 2/2 pour apps, 1/1 pour postgres
   - Test API: `curl -H "Host: car-rental.local" http://localhost:80/api/offers`

6. **Cleanup**
   - Capture des logs
   - Suppression du cluster Kind

### Configurations Clés CI

```bash
# Nom du cluster
kind create cluster  # Utilise "kind" par défaut

# Chargement des images
kind load docker-image carrental:latest auction-service-server:latest car-rental-angular:latest

# Vérification avec service-name labels
kubectl get pods -n rental-service -l service-name=carrental

# Attente avec component labels
kubectl wait --for=condition=ready pod -l component=backend -n rental-service --timeout=600s
```

### NGINX Control-Plane Patch (Critique pour Kind)

```bash
# Après installation NGINX, patch pour tourner sur control-plane
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
```

**Raison:** Le port mapping Kind (80:80, 443:443) est configuré sur le nœud control-plane. NGINX doit y tourner pour accéder à localhost.

## ✅ Validation
- ✅ Kustomize base contient 8 ressources
- ✅ Overlay Kind génère 19 ressources totales
- ✅ Overlay Minikube génère 17 ressources totales
- ✅ Pas de fichiers en double
- ✅ Tous les fichiers obsolètes supprimés
