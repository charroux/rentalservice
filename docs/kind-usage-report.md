# 📋 Rapport d'utilisation de Kind (Kubernetes in Docker)

## 🎯 Contexte du projet
- **Projet :** RentalService - Microservices (carRental + auction-service + PostgreSQL + Angular)
- **Objectif :** Migration de Docker Compose vers Kubernetes avec préparation Istio
- **Date :** 9 novembre 2025
- **Utilisateur :** Débutant avec Kind, expérimenté avec Docker/K8s

## 🚀 Installation et Setup

### ✅ Prérequis validés
- Docker Desktop : v24.0.6 ✅
- kubectl : v1.28.2 ✅
- Homebrew : Disponible ✅

### 📦 Installation Kind
```bash
brew install kind
# Résultat: Kind v0.30.0 installé avec succès
```

## 🏗️ Configuration du cluster

### 📄 Configuration utilisée
```yaml
# k8s/kind-config-simple.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: rental-service-cluster
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
  labels:
    app-type: backend
- role: worker  
  labels:
    app-type: frontend
```

### 🎯 Création du cluster
```bash
kind create cluster --config=k8s/kind-config-simple.yaml
```

**Durée :** ~2 minutes  
**Résultat :** ✅ Succès

## 🔍 Analyse du cluster créé

### 🐳 Conteneurs Docker
- **Nombre :** 3 conteneurs
- **Image :** kindest/node:v1.34.0
- **Noms :**
  - `rental-service-cluster-control-plane` (172.18.0.4)
  - `rental-service-cluster-worker` (172.18.0.2)  
  - `rental-service-cluster-worker2` (172.18.0.3)

### 🌐 Réseau
- **Réseau interne :** 172.18.0.0/16
- **API Server :** https://127.0.0.1:59086
- **Ports exposés :** 80, 443 (ingress), 6443 (API)

### 🎛️ Services système déployés automatiquement
| Service | Namespace | Pods | Status | Rôle |
|---------|-----------|------|--------|------|
| coredns | kube-system | 2/2 | Running | DNS interne |
| etcd | kube-system | 1/1 | Running | BDD cluster |
| kube-apiserver | kube-system | 1/1 | Running | API K8s |
| kube-controller-manager | kube-system | 1/1 | Running | Contrôleurs |
| kube-scheduler | kube-system | 1/1 | Running | Ordonnanceur |
| kindnet | kube-system | 3/3 | Running | CNI réseau |
| kube-proxy | kube-system | 3/3 | Running | Proxy réseau |
| local-path-provisioner | local-path-storage | 1/1 | Running | Stockage |

## ✅ Avantages observés de Kind

### 🚀 **Performance**
- Démarrage rapide (~2min)
- Faible consommation mémoire
- Pas de virtualisation lourde

### 🎯 **Fonctionnalités**
- Multi-node natif ✅
- Support ingress prêt ✅
- Compatible Istio ✅
- Isolation réseau propre ✅

### 🛠️ **Facilité d'usage**
- Configuration YAML intuitive ✅
- Intégration kubectl automatique ✅
- Ports mapping simple ✅

## 🔄 Comparaison avec alternatives

| Critère | Kind | Minikube | Docker Desktop K8s |
|---------|------|----------|-------------------|
| Multi-node | ✅ Natif | ❌ Single | ❌ Single |
| Performance | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| Setup | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Istio prep | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |

## 📝 Prochaines étapes identifiées

### Phase 1 : Backend K8s (en cours)
- [ ] Déployer PostgreSQL StatefulSet
- [ ] Déployer carRental service
- [ ] Déployer auction-service
- [ ] Configurer communication gRPC
- [ ] Tester les APIs

### Phase 2 : Istio (à venir)
- [ ] Installation Istio
- [ ] Configuration service mesh
- [ ] Politiques de sécurité mTLS
- [ ] Observabilité (Kiali, Jaeger)

### Phase 3 : Frontend Angular (à venir)
- [ ] Containerisation Angular
- [ ] Déploiement sur K8s
- [ ] Intégration avec Istio Gateway
- [ ] Tests end-to-end

## 🎓 Apprentissages clés

### 💡 **Concepts maîtrisés**
1. **Kind = K8s dans Docker** - Chaque node est un container
2. **Multi-node simulation** - Environnement réaliste sans VMs
3. **Port mapping** - Exposition services vers localhost
4. **Contexte kubectl** - Basculement automatique

### ⚠️ **Points d'attention**
1. Chaque cluster Kind consomme des resources Docker
2. Les données sont éphémères (sauf PV configurés)
3. Les ports host doivent être libres (80, 443)

## 📊 Métriques de performance

### 💾 **Ressources consommées**
- **RAM :** ~800MB pour le cluster complet
- **CPU :** <5% au repos  
- **Disk :** ~2GB images + données
- **Temps démarrage :** 120 secondes

### 🔧 **Commandes utiles découvertes**
```bash
# Gestion cluster
kind get clusters
kind delete cluster --name rental-service-cluster

# Debugging  
kubectl cluster-info
kubectl get nodes -o wide
kubectl get pods -A

# Contexte
kubectl config current-context
kubectl config use-context kind-rental-service-cluster
```

---
*Rapport en cours... Suite après déploiement des services backend*