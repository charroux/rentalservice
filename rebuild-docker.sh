#!/bin/bash

echo "=== Nettoyage des images Docker existantes ==="

# Arrêter tous les services
docker-compose -f docker-compose.dev.yml down

# Supprimer les images existantes
docker rmi charroux/car-rental:dev 2>/dev/null || echo "Image car-rental:dev n'existe pas"
docker rmi charroux/auction-service:dev 2>/dev/null || echo "Image auction-service:dev n'existe pas"

# Nettoyer les images dangling
docker image prune -f

echo "=== Reconstruction des images Docker ==="

# Reconstruire les images sans cache
docker-compose -f docker-compose.dev.yml build --no-cache

echo "=== Démarrage des services ==="

# Redémarrer les services
docker-compose -f docker-compose.dev.yml up -d

echo "=== Vérification des logs ==="
echo "Vous pouvez vérifier les logs avec:"
echo "docker-compose -f docker-compose.dev.yml logs -f car-rental"
echo "docker-compose -f docker-compose.dev.yml logs -f auction-service"