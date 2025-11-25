#!/bin/bash

# Script de setup Minikube pour rentalService
# Usage: ./setup-minikube-cluster.sh

set -e

# Déterminer le répertoire racine du projet
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

CLUSTER_NAME="rental-service"

echo "🚀 Setup Minikube cluster pour rentalService..."
echo "📁 Project root: ${PROJECT_ROOT}"

# 1. Vérifier si Minikube est installé
if ! command -v minikube &> /dev/null; then
    echo "❌ Minikube n'est pas installé. Installation..."
    if command -v brew &> /dev/null; then
        brew install minikube
    else
        echo "Installer Minikube manuellement: https://minikube.sigs.k8s.io/docs/start/"
        exit 1
    fi
fi

# 2. Supprimer le cluster existant si présent
if minikube status -p "${CLUSTER_NAME}" &> /dev/null; then
    echo "⚠️ Cluster ${CLUSTER_NAME} existe déjà. Suppression..."
    minikube delete -p "${CLUSTER_NAME}"
fi

# 3. Créer le cluster Minikube
echo "🏗️ Création cluster Minikube..."
minikube start -p "${CLUSTER_NAME}" \
    --cpus=4 \
    --memory=8192 \
    --disk-size=20g \
    --driver=docker

# 4. Activer les addons
echo "🔌 Activation des addons..."
minikube addons enable ingress -p "${CLUSTER_NAME}"
minikube addons enable metrics-server -p "${CLUSTER_NAME}"
minikube addons enable registry -p "${CLUSTER_NAME}"

# 5. Installer Istio
echo "🔷 Installation Istio..."
ISTIO_DIR="${PROJECT_ROOT}/istio-1.23.2"
if [ ! -d "${ISTIO_DIR}" ]; then
    echo "⚠️ Istio non trouvé. Téléchargement..."
    cd "${PROJECT_ROOT}"
    curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.23.2 sh -
    ISTIO_DIR="${PROJECT_ROOT}/istio-1.23.2"
fi

echo "Installation d'Istio avec istioctl..."
"${ISTIO_DIR}/bin/istioctl" install --set profile=demo -y

echo "✅ Istio installé"

# 6. Récupérer l'IP Minikube
MINIKUBE_IP=$(minikube ip -p "${CLUSTER_NAME}")

echo ""
echo "✅ Cluster Minikube avec Istio prêt!"
echo ""
echo "📋 Informations utiles:"
echo "   - Cluster name: ${CLUSTER_NAME}"
echo "   - Minikube IP: ${MINIKUBE_IP}"
echo "   - Context: ${CLUSTER_NAME}"
echo "   - Istio: version 1.23.2 (profile demo)"
echo ""
echo "⚠️ Configuration requise:"
echo "   1. Ajouter à /etc/hosts:"
echo "      ${MINIKUBE_IP} car-rental.local"
echo ""
echo "   2. Lancer le tunnel dans un autre terminal:"
echo "      minikube tunnel -p ${CLUSTER_NAME}"
echo ""
echo "🚀 Déployer l'application:"
echo "   cd k8s && ./deploy.sh"
echo ""
echo "🔧 Commandes utiles:"
echo "   minikube status -p ${CLUSTER_NAME}"
echo "   minikube dashboard -p ${CLUSTER_NAME}"
echo "   kubectl get pods -n istio-system"
echo "   kubectl get nodes"
echo ""
echo "🐳 Build images avec Minikube:"
echo "   eval \$(minikube docker-env -p ${CLUSTER_NAME})"
echo "   docker build -t rental-service:latest ."
echo ""
echo "🗑️ Pour supprimer le cluster:"
echo "   minikube delete -p ${CLUSTER_NAME}"
