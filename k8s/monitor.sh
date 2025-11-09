#!/bin/bash
# Monitoring script for rental-service architecture

echo "🏗️  RENTAL SERVICE ARCHITECTURE STATUS"
echo "======================================"
echo

echo "📊 CLUSTER OVERVIEW:"
kubectl get nodes --no-headers | wc -l | xargs echo "  Nodes:"
kubectl get pods -n rental-service --no-headers | wc -l | xargs echo "  Total Pods:"
kubectl get pods -n rental-service --field-selector=status.phase=Running --no-headers | wc -l | xargs echo "  Running Pods:"
echo

echo "🎯 SERVICE STATUS:"
echo "  Database (PostgreSQL):"
kubectl get pods -n rental-service -l app=postgres --no-headers | awk '{print "    " $1 ": " $3}'
echo "  Backend (carRental):"
kubectl get pods -n rental-service -l app=carrental --no-headers | awk '{print "    " $1 ": " $3}'
echo "  Backend (auctionService):"
kubectl get pods -n rental-service -l app=auction-service-server --no-headers | awk '{print "    " $1 ": " $3}'
echo "  Frontend (Angular):"
kubectl get pods -n rental-service -l app=frontend-angular --no-headers | awk '{print "    " $1 ": " $3}'
echo

echo "🌐 INGRESS STATUS:"
kubectl get ingress -n rental-service --no-headers | while read line; do
  name=$(echo $line | awk '{print $1}')
  hosts=$(echo $line | awk '{print $3}')
  echo "  $name: $hosts"
done
echo

echo "🔗 CONNECTIVITY TESTS:"
echo "  Frontend: $(curl -H 'Host: car-rental.local' -s -o /dev/null -w '%{http_code}' http://localhost:80/)"
echo "  API Health: $(curl -H 'Host: api.car-rental.local' -s -o /dev/null -w '%{http_code}' http://localhost:80/actuator/health)"
echo "  Car Models: $(curl -H 'Host: api.car-rental.local' -s http://localhost:80/car-models | jq length 2>/dev/null || echo 'N/A')"
echo

echo "✅ Architecture is ready!"
echo "   Frontend: http://car-rental.local/"
echo "   API: http://api.car-rental.local/"
echo "   Direct API: http://car-rental.local/direct-api/"