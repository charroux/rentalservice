#!/bin/bash

# Script de setup Kind pour rentalService
# Usage: ./setup-kind-cluster.sh

set -e

CLUSTER_NAME="rental-service-cluster"
REGISTRY_NAME="kind-registry"
REGISTRY_PORT="5001"

echo "🚀 Setup Kind cluster pour rentalService..."

# 1. Vérifier si Kind est installé
if ! command -v kind &> /dev/null; then
    echo "❌ Kind n'est pas installé. Installation..."
    if command -v brew &> /dev/null; then
        brew install kind
    else
        echo "Installer Kind manuellement: https://kind.sigs.k8s.io/docs/user/quick-start/"
        exit 1
    fi
fi

# 2. Créer registry local pour les images Docker
echo "📦 Création registry local..."
if [ "$(docker inspect -f '{{.State.Running}}' "${REGISTRY_NAME}" 2>/dev/null || true)" != 'true' ]; then
    docker run -d --restart=always -p "127.0.0.1:${REGISTRY_PORT}:5000" --name "${REGISTRY_NAME}" registry:2
fi

# 3. Créer le cluster Kind
echo "🏗️ Création cluster Kind..."
if kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
    echo "⚠️ Cluster ${CLUSTER_NAME} existe déjà. Suppression..."
    kind delete cluster --name "${CLUSTER_NAME}"
fi

kind create cluster --name "${CLUSTER_NAME}" --config=k8s/kind-config.yaml

# 4. Connecter le registry au cluster
echo "🔗 Connection registry au cluster..."
if [ "$(docker inspect -f='{{json .NetworkSettings.Networks.kind}}' "${REGISTRY_NAME}")" = 'null' ]; then
    docker network connect "kind" "${REGISTRY_NAME}"
fi

# 5. Documenter le registry dans le cluster
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: local-registry-hosting
  namespace: kube-public
data:
  localRegistryHosting.v1: |
    host: "localhost:${REGISTRY_PORT}"
    help: "https://kind.sigs.k8s.io/docs/user/local-registry/"
EOF

# 6. Installer MetalLB pour LoadBalancer support
echo "⚖️ Installation MetalLB..."
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.13.7/config/manifests/metallb-native.yaml
kubectl wait --namespace metallb-system \
                --for=condition=ready pod \
                --selector=app=metallb \
                --timeout=90s

# Configuration MetalLB
kubectl apply -f - <<EOF
apiVersion: metallb.io/v1beta1
kind: IPAddressPool
metadata:
  name: example
  namespace: metallb-system
spec:
  addresses:
  - 172.19.255.200-172.19.255.250
---
apiVersion: metallb.io/v1beta1
kind: L2Advertisement
metadata:
  name: empty
  namespace: metallb-system
EOF

# 7. Installer NGINX Ingress Controller
echo "🌐 Installation NGINX Ingress..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s

# 8. Installer Istio
echo "🔷 Installation Istio..."
ISTIO_DIR="../istio-1.23.2"
if [ ! -d "${ISTIO_DIR}" ]; then
    echo "⚠️ Istio non trouvé. Téléchargement..."
    cd ..
    curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.23.2 sh -
    cd k8s
    ISTIO_DIR="../istio-1.23.2"
fi

echo "Installation d'Istio avec istioctl..."
"${ISTIO_DIR}/bin/istioctl" install --set profile=demo -y

echo "✅ Istio installé"

echo "✅ Cluster Kind avec Istio prêt!"
echo ""
echo "📋 Informations utiles:"
echo "   - Cluster name: ${CLUSTER_NAME}"
echo "   - Registry: localhost:${REGISTRY_PORT}"
echo "   - Context: kind-${CLUSTER_NAME}"
echo "   - Istio: version 1.23.2 (profile demo)"
echo ""
echo "🚀 Déployer l'application:"
echo "   cd k8s && ./deploy.sh"
echo ""
echo "🔧 Commandes utiles:"
echo "   kubectl cluster-info --context kind-${CLUSTER_NAME}"
echo "   kubectl get pods -n istio-system"
echo "   kubectl get nodes"
echo ""
echo "🐳 Build & Push images vers le registry local:"
echo "   docker build -t localhost:${REGISTRY_PORT}/rental-service:latest ."
echo "   docker push localhost:${REGISTRY_PORT}/rental-service:latest"
echo ""
echo "🗑️ Pour supprimer le cluster:"
echo "   kind delete cluster --name ${CLUSTER_NAME}"
echo "   docker rm -f ${REGISTRY_NAME}"