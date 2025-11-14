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
- ✅ 3 Deployments (carrental, frontend-angular, auction-service-server)
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

## 🚀 Commandes de déploiement

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
| **Accès** | localhost:80 via port mapping | minikube ip via tunnel |
| **NGINX** | Installation manuelle | Addon intégré |
| **Hosts** | 127.0.0.1 car-rental.local | $(minikube ip) car-rental.local |

## ✅ Validation

Structure validée avec succès :
- ✅ Kustomize base contient 8 ressources
- ✅ Overlay Kind génère 19 ressources totales
- ✅ Overlay Minikube génère 17 ressources totales
- ✅ Pas de fichiers en double
- ✅ Tous les fichiers obsolètes supprimés
