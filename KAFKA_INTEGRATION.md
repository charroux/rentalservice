# Intégration Kafka dans le Projet Car Rental

## Architecture Événementielle

Ce projet intègre **Apache Kafka en mode KRaft** (sans ZooKeeper) pour gérer les événements de location de voitures.

### 🎯 Flux de Données

```
Angular Frontend → REST API → Kafka → Delivery Service
     (UI)       (carRental)  (Events) (Consumer/Logger)
```

## 🚀 Composants

### 1. **Kafka (Mode KRaft)**
- **1 broker Kafka** en StatefulSet (Kubernetes) ou conteneur Docker
- **Mode KRaft** : pas besoin de ZooKeeper (architecture moderne)
- **Topic** : `car-rented-events`
- **Port** : 9092

### 2. **Service carRental (Producer)**
- **Endpoint REST** : `POST /rental/confirm`
- Publie des événements `CarRentedEvent` vers Kafka
- Contient : informations client, voiture, prix, remise

### 3. **Service Delivery (Consumer)**
- **Écoute** le topic `car-rented-events`
- **Traite** les événements de location confirmée
- **Logge** les informations (pour l'instant)
- **Port** : 8081

## 📋 Configuration

### Docker Compose (Développement Local)

```bash
# Démarrer tous les services (dont Kafka)
docker-compose -f docker-compose.dev.yml up -d

# Voir les logs de Kafka
docker-compose -f docker-compose.dev.yml logs -f kafka

# Voir les logs du service Delivery
docker-compose -f docker-compose.dev.yml logs -f delivery-service
```

### Kubernetes (Kind/Minikube)

```bash
# Construire les images
docker build -f carRental/Dockerfile -t carrental:latest .
docker build -f deliveryService/Dockerfile -t delivery-service:latest .

# Charger dans Kind
kind load docker-image carrental:latest --name rental-service-cluster
kind load docker-image delivery-service:latest --name rental-service-cluster

# Déployer
kubectl apply -k k8s/overlays/kind

# Vérifier les pods
kubectl get pods -n rental-service

# Voir les logs du service Delivery
kubectl logs -n rental-service -l app=delivery-service -f
```

## 🔧 Structure des Événements

### CarRentedEvent

```json
{
  "plateNumber": "AB-123-CD",
  "brand": "Toyota",
  "model": "Corolla",
  "carModelId": 1,
  "finalPrice": 45.0,
  "originalPrice": 50.0,
  "discountAmount": 5.0,
  "discountApplied": true,
  "customerFirstName": "John",
  "customerLastName": "Doe",
  "customerEmail": "john.doe@example.com",
  "rentalConfirmedAt": "2025-12-12T10:30:00"
}
```

## 🧪 Test du Flux Complet

### 1. Démarrer l'environnement

```bash
# Option A : Docker Compose
docker-compose -f docker-compose.dev.yml up -d

# Option B : Kubernetes
kubectl apply -k k8s/overlays/kind
```

### 2. Accéder à l'application

- **Frontend** : http://localhost:4200 (ou http://car-rental.local pour K8s)
- **API carRental** : http://localhost:8080
- **API Delivery** : http://localhost:8081

### 3. Processus de Location

1. **Sélectionner une voiture** sur le frontend
2. **Participer à l'enchère** → obtenir un prix final
3. **Confirmer la location** → déclenche l'événement Kafka
4. **Vérifier les logs** du service Delivery

```bash
# Docker Compose
docker-compose -f docker-compose.dev.yml logs -f delivery-service

# Kubernetes
kubectl logs -n rental-service -l app=delivery-service -f
```

### 4. Exemple de log attendu

```
========================================
Received CarRentedEvent from Kafka:
  Car: Toyota Corolla (Plate: AB-123-CD)
  Customer: John Doe (john.doe@example.com)
  Pricing: Original=50.0€, Final=45.0€, Discount=5.0€, Applied=true
  Rental confirmed at: 2025-12-12T10:30:00
========================================
Delivery process initiated for car AB-123-CD
```

## 📁 Fichiers Créés/Modifiés

### Backend (carRental)
- `carRental/build.gradle` - Ajout dépendances Kafka
- `carRental/.../event/CarRentedEvent.java` - Classe événement
- `carRental/.../config/KafkaProducerConfig.java` - Configuration producer
- `carRental/.../service/EventPublisher.java` - Service publication
- `carRental/.../dto/RentalConfirmationDTO.java` - DTO requête
- `carRental/.../web/CarRentalRestService.java` - Endpoint `/rental/confirm`

### Microservice Delivery
- `deliveryService/build.gradle` - Configuration Gradle
- `deliveryService/Dockerfile` - Image Docker
- `deliveryService/.../DeliveryServiceApplication.java` - Application Spring Boot
- `deliveryService/.../event/CarRentedEvent.java` - Classe événement
- `deliveryService/.../config/KafkaConsumerConfig.java` - Configuration consumer
- `deliveryService/.../consumer/CarRentedEventConsumer.java` - Consumer Kafka
- `deliveryService/.../resources/application.properties` - Configuration

### Frontend (Angular)
- `car-rental-angular/.../rental.service.ts` - Méthode `confirmRental()`
- `car-rental-angular/.../validate-rental.component.ts` - Appel API activé

### Infrastructure
- `k8s/base/kafka-configmap.yaml` - Configuration Kafka
- `k8s/base/kafka-service.yaml` - Services Kubernetes
- `k8s/base/kafka-statefulset.yaml` - Déploiement Kafka
- `k8s/base/delivery-deployment.yaml` - Déploiement Delivery
- `k8s/base/carrental-deployment.yaml` - Ajout variable Kafka
- `k8s/base/kustomization.yaml` - Ajout ressources Kafka/Delivery
- `docker-compose.dev.yml` - Services Kafka et Delivery
- `settings.gradle` - Inclusion du module deliveryService

## 🔍 Variables d'Environnement

### carRental
```env
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092  # ou kafka-service:9092 dans K8s
```

### deliveryService
```env
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092  # ou kafka-service:9092 dans K8s
```

## 🛠️ Commandes Utiles

### Vérifier Kafka

```bash
# Docker Compose - Vérifier que Kafka est prêt
docker-compose -f docker-compose.dev.yml exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Kubernetes - Vérifier les topics
kubectl exec -n rental-service kafka-0 -- kafka-topics --list --bootstrap-server localhost:9092

# Consommer les événements manuellement
kubectl exec -n rental-service kafka-0 -- kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic car-rented-events \
  --from-beginning
```

## 📊 Monitoring

### Health Checks

```bash
# carRental
curl http://localhost:8080/actuator/health

# deliveryService
curl http://localhost:8081/actuator/health
```

## 🎓 Évolutions Futures

Le service Delivery peut être étendu pour :
- ✉️ Envoyer des emails de confirmation
- 📅 Planifier la préparation du véhicule
- 📦 Gérer le tracking de livraison
- 💳 Intégrer le paiement
- 📱 Envoyer des notifications push
- 🔄 Publier de nouveaux événements (DeliveryScheduled, DeliveryCompleted, etc.)

## 🐛 Dépannage

### Kafka ne démarre pas
```bash
# Vérifier les logs
docker-compose -f docker-compose.dev.yml logs kafka

# Supprimer le volume et redémarrer
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up -d
```

### Événements non reçus
```bash
# Vérifier que le topic existe
docker-compose -f docker-compose.dev.yml exec kafka \
  kafka-topics --describe --topic car-rented-events --bootstrap-server localhost:9092

# Vérifier les consumer groups
docker-compose -f docker-compose.dev.yml exec kafka \
  kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

## 📚 Ressources

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Confluent Platform](https://docs.confluent.io/)
