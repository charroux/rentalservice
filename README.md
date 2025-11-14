# Cloud native app

```
Voilà la logique du projet : le fichier aunctionService.proto définit un CarModel comme ayant un lowestPrice et un highestPrice. La classe JPA CarModelJPA (projet carRental) reflète bien cela. Cependant, ce n'est pas un CarModelJPA que les utilisateurs finaux vont louer mais un Car (voir la classe Car du projet carRental). Car correspond à une instance réel d'un CarModel ayant une plaque d'immatriculation et un prix de location qui peut être différent des lowestPrice et highestPrice. Pour éviter toute confusion je suggère de renommer price de la classe Car en rentalPrice. Côté front angular, l'utilisateur ne doit voir que ce rentalPrice. Je suggère donc de supprimer côté front les champs lowestPrice et highestPrice au profit du seul champ rentalPrice. Mais attention ! Au démarrage du projet seuls les CarModel sont initialisés (voir le constructeur de la classe RentalServiceImpl), et donc seuls les lowestPrice et highestPrice sont connus. Ainsi le champs rentalPrice côté front ne peut être initialisé qu'avec un de ces deux champs. Je propose que ce soit avec highestPrice. Nous réaliseront ensemle mais plus tard la logique de sélection d'une Car particulière via une vente aux enchères parmi un ensemble de voitures issues du même CzrModel, et finalement c'est cette voiture qui sera proposées à la location. Peux-tu me dire si c'est bien clair pour toi avant de procéder aux changements ?
```

## Build Instructions avec Gradle

Ce projet utilise Gradle comme système de build. Voici les commandes principales pour compiler les différents modules :

### Build complet du projet

```bash
# Builder tous les modules du projet
./gradlew build

# Clean et rebuild complet (recommandé après modifications importantes)
./gradlew clean build
```

### Build par module individuel

```bash
# Module auctionService (définitions protobuf + client gRPC)
./gradlew :auctionService:build

# Module auctionServiceServer (serveur gRPC)
./gradlew :auctionServiceServer:build

# Module carRental (service principal REST + JPA)
./gradlew :carRental:build
```

### Commandes spécifiques pour les fichiers Protobuf/gRPC

Après modification des fichiers `.proto`, régénérer les classes Java :

```bash
# Régénération pour le module auctionService
./gradlew :auctionService:clean :auctionService:generateProto :auctionService:compileJava

# Régénération complète (recommandée après modification de .proto)
./gradlew clean build

# Génération protobuf uniquement (sans compilation)
./gradlew :auctionService:generateProto
```

### Autres commandes utiles

```bash
# Tests uniquement
./gradlew test

# Tests pour un module spécifique
./gradlew :carRental:test

# Build sans tests (plus rapide)
./gradlew build -x test

# Nettoyage des builds
./gradlew clean

# Vérifier les dépendances
./gradlew dependencies
```

### Structure des modules

- **auctionService** : Contient les définitions protobuf (`.proto`) et génère les classes Java client
- **auctionServiceServer** : Implémente le serveur gRPC à partir des définitions protobuf
- **carRental** : Service principal avec API REST, JPA, et client gRPC
- **car-rental-angular** : Frontend Angular (build séparé avec npm/ng)

⚠️ **Important** : Après modification d'un fichier `.proto`, vous devez rebuilder TOUS les modules qui l'utilisent pour éviter les erreurs de compilation.

## Test des Enchères de Voitures

Le système d'enchères permet aux carRentalCompany de participer à des enchères en temps réel pour obtenir des voitures à louer. Voici comment tester cette fonctionnalité :

### Démarrage des services

```bash
# Méthode 1: Docker Compose (recommandée pour les tests)
docker compose -f docker-compose.dev.yml up

# Méthode 2: Gradle (pour le développement)
# Terminal 1 - Serveur gRPC d'enchères
./gradlew :auctionServiceServer:bootRun

# Terminal 2 - Service principal
./gradlew :carRental:bootRun

# Terminal 3 - Frontend Angular (optionnel)
cd car-rental-angular && npm start
```

### Commandes curl pour tester les enchères

#### 1. **Lister les modèles de voitures disponibles**
```bash
curl -X GET http://localhost:8080/cars
```

#### 2. **Participer à une enchère Ferrari F8**
```bash
# Enchère avec companyId par défaut
curl -X POST http://localhost:8080/auction/Ferrari/F8

# Enchère avec une entreprise spécifique
curl -X POST http://localhost:8080/auction/Ferrari/F8?companyId=HERTZ_COMPANY
```

#### 3. **Enchères pour différents modèles**
```bash
# Porsche 911 (600€-900€)
curl -X POST http://localhost:8080/auction/Porsche/911?companyId=AVIS_RENTAL

# Tesla Model S (300€-500€) - Attention à l'encodage URL
curl -X POST "http://localhost:8080/auction/Tesla/Model%20S?companyId=ENTERPRISE"
```

#### 4. **Tests d'enchères simultanées**
Pour tester la logique de concurrence, lancez plusieurs enchères en parallèle :

```bash
# Terminal 1
curl -X POST http://localhost:8080/auction/Ferrari/F8?companyId=COMPANY_A &

# Terminal 2
curl -X POST http://localhost:8080/auction/Ferrari/F8?companyId=COMPANY_B &

# Terminal 3
curl -X POST http://localhost:8080/auction/Ferrari/F8?companyId=COMPANY_C &
```

#### 5. **Vérifier les résultats d'enchère**

Après une enchère réussie, vous obtenez une plaque d'immatriculation. Vous pouvez alors :

```bash
# Voir les détails de la voiture (pour l'utilisateur final)
curl -X GET http://localhost:8080/cars/FE-001-F8

# Voir la marge de la carRentalCompany
curl -X GET http://localhost:8080/cars/FE-001-F8/margin
```

### Réponses attendues

#### Enchère réussie :
```json
{
  "plateNumber": "FE-001-F8",
  "brand": "Ferrari",
  "model": "F8", 
  "rentalPrice": 1200,
  "photo": "default_photo_url"
}
```

#### Informations sur la marge :
```
Voiture FE-001-F8 - Prix d'acquisition: 800€, Prix de location: 1200€, Marge: 400€ (50.0%)
```

### Logique d'enchères

- **Durée** : 5 secondes (configuré pour les tests, 30s en production)
- **Prix initial** : `lowestPrice` du modèle (ex: 800€ pour Ferrari F8)
- **Prix utilisateur final** : Toujours `highestPrice` (ex: 1200€ pour Ferrari F8)
- **Marge carRentalCompany** : Différence entre `rentalPrice` et `finalPrice`
- **Incrément minimal** : 10€ entre les enchères
- **Limitation** : Une participation par entreprise et par modèle

### Cas d'usage typique

1. **Utilisateur frontend** sélectionne "Ferrari F8"
2. **carRentalCompany** participe automatiquement à l'enchère (800€)
3. **Enchère de 5s** - autres entreprises peuvent surenchérir
4. **Résultat** : Voiture attribuée avec plaque d'immatriculation
5. **Utilisateur final** voit le prix fixe de 1200€/jour
6. **carRentalCompany** connaît sa marge (ex: 400€ de bénéfice)

### Debug et logs

Pour voir les logs détaillés des enchères :
```bash
# Logs du service principal
docker compose -f docker-compose.dev.yml logs -f car-rental

# Logs du serveur d'enchères
docker compose -f docker-compose.dev.yml logs -f auction-service
```

### Générer le code protobuf / gRPC

Le projet utilise protobuf pour définir les contracts gRPC. Le plugin Gradle génère les sources Java à partir des fichiers `.proto`.

#### Commandes de régénération après modification d'un fichier .proto

Lorsque vous modifiez un fichier `.proto` (comme `/auctionService/src/main/proto/auctionService.proto`), vous devez régénérer les classes Java correspondantes :

**1. Régénération pour un module spécifique :**
```bash
# Pour le module auctionService (client)
./gradlew :auctionService:clean :auctionService:generateProto :auctionService:compileJava

# Pour le module auctionServiceServer (serveur)
./gradlew :auctionServiceServer:clean :auctionServiceServer:compileJava
```

**2. Régénération complète (recommandée) :**
```bash
# Nettoyer, régénérer et compiler tous les modules
./gradlew clean build
```

**3. Régénération depuis le dossier racine multi-module :**
```bash
# Si vous êtes dans le dossier racine du projet
./gradlew auctionService:clean auctionService:build
./gradlew auctionServiceServer:clean auctionServiceServer:build
./gradlew carRental:clean carRental:build
```

#### Autres commandes utiles

- Générer les sources protobuf pour le module `auctionService` :
```bash
./gradlew :auctionService:generateProto
```

- Générer puis compiler le module serveur (génère les sources et compile) :
```bash
./gradlew :auctionService:generateProto :auctionServiceServer:compileJava
```

#### Où trouver les fichiers générés

- Par défaut dans ce projet les fichiers générés sont placés sous `auctionService/src/generated` (configuré dans `auctionService/build.gradle`).
- Les classes générées incluent les messages (ex: `CarModel.java`), les services gRPC (`AuctionServiceGrpc.java`), etc.

#### Conseils IDE / CI

- Après génération, rafraîchissez le projet Gradle dans votre IDE (IntelliJ/VS Code) pour que le répertoire `src/generated` soit reconnu comme source Java.
- Le plugin télécharge automatiquement `protoc` et le plugin gRPC si nécessaire ; pas besoin d'installer `protoc` manuellement pour la compilation Gradle.
- **Important :** Si vous modifiez les fichiers `.proto`, regénérez les sources puis reconstruisez TOUS les modules consommateurs (ex. `auctionServiceServer`, `carRental`) pour éviter les erreurs de compilation.

#### Workflow typique après modification d'un .proto

1. Modifiez le fichier `.proto` (ex: renommer `Car` en `CarModel`)
2. Mettez à jour le code Java qui utilise les anciennes classes générées
3. Régénérez avec `./gradlew clean build`
4. Vérifiez que tous les tests passent


## Build the Docker images (optional)

Docker images for the services can be built locally. New Dockerfiles have been added to the repository:

- `carRental/Dockerfile` — multi-stage builder (Gradle + JDK 21) and runtime (Temurin JRE).
- `agreementService/Dockerfile` — builds the agreement service artifacts and packages them.
- `agreementServiceServer/Dockerfile` — builds the gRPC server service.
- `Dockerfile.multi` — multi-target Dockerfile at repository root with targets `carRental` and `postgres`.

You can also find some images published to Docker Hub under the `charroux` namespace; if you prefer to use those, update the manifests in `k8s/` accordingly.

Examples — build and push to your registry (replace `charroux` with your registry/namespace):

Build the car-rental image (single-service Dockerfile):
```bash
docker build -f carRental/Dockerfile -t charroux/car-rental:latest .
docker push charroux/car-rental:latest
```

Build the agreement-service image:
```bash
docker build -f agreementService/Dockerfile -t charroux/agreement-service:latest .
docker push charroux/agreement-service:latest
```

Build the agreement-service-server image:
```bash
docker build -f agreementServiceServer/Dockerfile -t charroux/agreement-service-server:latest .
docker push charroux/agreement-service-server:latest
```

Build the car-rental image using the multi-target root Dockerfile (alternate):
```bash
# build only the carRental target from Dockerfile.multi
docker build -f Dockerfile.multi --target carRental -t charroux/car-rental:latest .
docker push charroux/car-rental:latest
```

Build a custom Postgres image (optional, includes init scripts if present):
```bash
docker build -f Dockerfile.multi --target postgres -t charroux/postgres:15 .
docker push charroux/postgres:15
```

Quick dev using Docker Compose
---------------------------------
A ready-to-use Docker Compose file for development has been added: `docker-compose.dev.yml` and an environment file `.env.dev`.

Basic workflow
1. Copy or edit the example environment file (optional):

```bash
cp .env.dev .env
# edit .env to change POSTGRES_USER/POSTGRES_PASSWORD/POSTGRES_DB if needed
```

2. Start the stack (build images and run all services):

```bash
docker compose -f docker-compose.dev.yml --env-file .env.dev up
```

This command:
- Uses development environment variables from `.env.dev`
- Starts PostgreSQL database
- Starts car-rental service (REST API)
- Starts agreement-service (gRPC)

This will:
- Build and start the Postgres database
- Build and start the car-rental service on port 8080
- Build and start the agreement-service (gRPC) on port 9090

3. Run in detached mode:

```bash
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d
```
4. View logs:

```bash
docker compose -f docker-compose.dev.yml logs -f car-rental
```

5. Stop and remove containers:

```bash
docker compose -f docker-compose.dev.yml --env-file .env.dev down
```

Note: Add `-v` flag to also remove volumes (this will delete persistent data):
```bash
docker compose -f docker-compose.dev.yml --env-file .env.dev down -v
```

Rebuild specific services:

```bash
# Rebuild single service
docker compose -f docker-compose.dev.yml --env-file .env.dev build car-rental
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d

# Rebuild all services
docker compose -f docker-compose.dev.yml --env-file .env.dev build
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d
```

What this compose file does
- Starts a Postgres container (`postgres:15`) with a persistent volume `postgres-data`.
- Builds the `car-rental` image from `carRental/Dockerfile` and runs it with `SPRING_PROFILES_ACTIVE=prod` so the app will read Postgres connection settings from the `SPRING_DATASOURCE_*` env vars provided by compose.
- Builds and starts the `agreement-service` gRPC server from `agreementServiceServer/Dockerfile`.
- Exposes the following ports on the host:
  - `8080`: car-rental REST API
  - `9090`: agreement-service gRPC endpoint

Healthchecks and caveats
- The `docker-compose.dev.yml` includes healthchecks for Postgres and the `car-rental` service. The car-rental healthcheck calls the actuator `/actuator/health` endpoint. If your runtime image does not include `curl`, the healthcheck may fail — in that case either:
    - install `curl` in the runtime image, or
    - change the healthcheck to use a simple TCP check (nc) or remove it for local development, or
    - run the app with a mounted development image that includes curl.

Environment overrides
- You can override any Spring property by passing environment variables in the compose file (already wired for the datasource). For production, prefer using a secret manager or Kubernetes Secrets instead of plain env files.

Docker Compose is suitable for quick local development. For CI and production use Kubernetes manifests in `k8s/`.

Kubernetes
-----------
After building and pushing images, update `k8s/` manifests if you used different image names/tags and apply them:
```bash
kubectl apply -k k8s/
```

Notes
- The Dockerfiles are multi-stage and run the Gradle build inside the builder image. For CI you may prefer building the jar artifact in the CI runner and then creating a smaller runtime image from it.
- The builder images (e.g. `gradle:8.6-jdk21`) may trigger vulnerability warnings in scanners; it's recommended to scan images in your CI and choose minimal runtime images for production (distroless, slim, or jlink-based images).
- Replace any example credentials or base64 secrets before deploying to production.


# cloud-native-app
