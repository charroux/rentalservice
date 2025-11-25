# Quick dev using Docker Compose

A ready-to-use Docker Compose file for development has been added: `docker-compose.dev.yml` and an environment file `.env.dev`.

## Basic workflow

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

## Commandes curl pour tester les enchères

### 1. **Lister les modèles de voitures disponibles**
```bash
curl -X GET http://localhost:8080/cars
```

### 2. **Participer à une enchère Ferrari F8**
```bash
# Enchère avec companyId par défaut
curl -X POST http://localhost:8080/auction/Ferrari/F8

# Enchère avec une entreprise spécifique
curl -X POST http://localhost:8080/auction/Ferrari/F8?companyId=HERTZ_COMPANY
```

### 3. **Enchères pour différents modèles**
```bash
# Porsche 911 (600€-900€)
curl -X POST http://localhost:8080/auction/Porsche/911?companyId=AVIS_RENTAL

# Tesla Model S (300€-500€) - Attention à l'encodage URL
curl -X POST "http://localhost:8080/auction/Tesla/Model%20S?companyId=ENTERPRISE"
```

### 4. **Tests d'enchères simultanées**
Pour tester la logique de concurrence, lancez plusieurs enchères en parallèle :

```bash
# Terminal 1
curl -X POST http://localhost:8080/auction/Ferrari/F8?companyId=COMPANY_A &

# Terminal 2
curl -X POST http://localhost:8080/auction/Ferrari/F8?companyId=COMPANY_B &

# Terminal 3
curl -X POST http://localhost:8080/auction/Ferrari/F8?companyId=COMPANY_C &
```

### 5. **Vérifier les résultats d'enchère**

Après une enchère réussie, vous obtenez une plaque d'immatriculation. Vous pouvez alors :

```bash
# Voir les détails de la voiture (pour l'utilisateur final)
curl -X GET http://localhost:8080/cars/FE-001-F8

# Voir la marge de la carRentalCompany
curl -X GET http://localhost:8080/cars/FE-001-F8/margin
```

## Réponses attendues

### Enchère réussie :
```json
{
  "plateNumber": "FE-001-F8",
  "brand": "Ferrari",
  "model": "F8", 
  "rentalPrice": 1200,
  "photo": "default_photo_url"
}
```

### Informations sur la marge :
```
Voiture FE-001-F8 - Prix d'acquisition: 800€, Prix de location: 1200€, Marge: 400€ (50.0%)
```

## Rebuild specific services

```bash
# Rebuild single service
docker compose -f docker-compose.dev.yml --env-file .env.dev build car-rental
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d

# Rebuild all services
docker compose -f docker-compose.dev.yml --env-file .env.dev build
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d
```

## What this compose file does

- Starts a Postgres container (`postgres:15`) with a persistent volume `postgres-data`.
- Builds the `car-rental` image from `carRental/Dockerfile` and runs it with `SPRING_PROFILES_ACTIVE=prod` so the app will read Postgres connection settings from the `SPRING_DATASOURCE_*` env vars provided by compose.
- Builds and starts the `agreement-service` gRPC server from `agreementServiceServer/Dockerfile`.
- Exposes the following ports on the host:
  - `8080`: car-rental REST API
  - `9090`: agreement-service gRPC endpoint

## Healthchecks and caveats

- The `docker-compose.dev.yml` includes healthchecks for Postgres and the `car-rental` service. The car-rental healthcheck calls the actuator `/actuator/health` endpoint. If your runtime image does not include `curl`, the healthcheck may fail — in that case either:
    - install `curl` in the runtime image, or
    - change the healthcheck to use a simple TCP check (nc) or remove it for local development, or
    - run the app with a mounted development image that includes curl.

## Environment overrides

- You can override any Spring property by passing environment variables in the compose file (already wired for the datasource). For production, prefer using a secret manager or Kubernetes Secrets instead of plain env files.

Docker Compose is suitable for quick local development. For CI and production use Kubernetes manifests in `k8s/`.
