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
- Build and start the auction-service (gRPC) on port 9090
- Build and start the Angular frontend on port 4200 (served by nginx)

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

## Commandes curl pour tester l'application

### Accès via le frontend (recommandé)
Le frontend Angular est accessible sur http://localhost:4200 et expose les mêmes endpoints via son proxy nginx.

**Via le frontend nginx (port 4200):**
```bash
# Lister les offres
curl -X GET http://localhost:4200/api/offers

# Participer à une enchère
curl -X POST http://localhost:4200/api/auction/participate \
  -H "Content-Type: application/json" \
  -d '{"carModelId": 1}'
```

**Directement sur le backend (port 8080):**
```bash
# Lister les offres
curl -X GET http://localhost:8080/offers

# Participer à une enchère
curl -X POST http://localhost:8080/auction/participate \
  -H "Content-Type: application/json" \
  -d '{"carModelId": 1}'
```

### 1. **Lister les offres de location (endpoint principal Angular)**
```bash
curl -X GET http://localhost:8080/offers
```
**Réponse attendue:**
```json
[
  {
    "id": 1,
    "brand": "Ferrari",
    "model": "F8",
    "photoUrl": "/assets/cars/ferrari-f8.jpg",
    "rentalPrice": 1500.00
  },
  {
    "id": 2,
    "brand": "Porsche",
    "model": "911",
    "photoUrl": "/assets/cars/porsche-911.jpg",
    "rentalPrice": 900.00
  }
]
```

### 2. **Lister les modèles de voitures (legacy endpoint)**
```bash
curl -X GET http://localhost:8080/car-models
```

### 3. **Lister les voitures disponibles avec plaques (legacy endpoint)**
```bash
curl -X GET http://localhost:8080/cars
```

### 4. **Obtenir une voiture spécifique par plaque**
```bash
curl -X GET http://localhost:8080/cars/FE-001-F8
```

### 5. **Participer à une enchère (endpoint principal Angular)**
```bash
# Enchère avec carModelId (utilisé par Angular)
curl -X POST http://localhost:8080/auction/participate \
  -H "Content-Type: application/json" \
  -d '{"carModelId": 1}'
```
**Réponse attendue:**
```json
{
  "plateNumber": "FE-001-F8",
  "finalPrice": 1200.00,
  "originalPrice": 1500.00,
  "discountAmount": 300.00,
  "discountApplied": true
}
```

### 6. **Soumettre une demande de location (formulaire Angular)**
```bash
curl -X POST "http://localhost:8080/cars/FE-001-F8?firstName=John&lastName=Doe&email=john.doe@example.com&beginDate=2025-12-01&endDate=2025-12-10"
```

### 7. **Tests d'enchères pour différents modèles**
```bash
# Ferrari F8 (carModelId: 1)
curl -X POST http://localhost:8080/auction/participate \
  -H "Content-Type: application/json" \
  -d '{"carModelId": 1}'

# Porsche 911 (carModelId: 2)
curl -X POST http://localhost:8080/auction/participate \
  -H "Content-Type: application/json" \
  -d '{"carModelId": 2}'

# Tesla Model S (carModelId: 3)
curl -X POST http://localhost:8080/auction/participate \
  -H "Content-Type: application/json" \
  -d '{"carModelId": 3}'

# Lamborghini Huracan (carModelId: 4)
curl -X POST http://localhost:8080/auction/participate \
  -H "Content-Type: application/json" \
  -d '{"carModelId": 4}'
```

### 8. **Tests d'enchères simultanées**
Pour tester la logique de concurrence, lancez plusieurs enchères en parallèle :

```bash
# Terminal 1
curl -X POST http://localhost:8080/auction/participate \
  -H "Content-Type: application/json" \
  -d '{"carModelId": 1}' &

# Terminal 2
curl -X POST http://localhost:8080/auction/participate \
  -H "Content-Type: application/json" \
  -d '{"carModelId": 1}' &

# Terminal 3
curl -X POST http://localhost:8080/auction/participate \
  -H "Content-Type: application/json" \
  -d '{"carModelId": 1}' &
```

## Rebuild specific services

```bash
# Rebuild single service
docker compose -f docker-compose.dev.yml --env-file .env.dev build car-rental
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d car-rental

# Rebuild frontend
docker compose -f docker-compose.dev.yml --env-file .env.dev build frontend-angular
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d frontend-angular

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
  - `9090`: auction-service gRPC endpoint
  - `4200`: Angular frontend (nginx serves the app and proxies `/api/` to car-rental service)

## Frontend access

Once the stack is running, you can access:

- **Angular Frontend**: http://localhost:4200
- **Backend API (direct)**: http://localhost:8080
- **Backend API (via frontend proxy)**: http://localhost:4200/api/

The frontend nginx automatically proxies all `/api/*` requests to the `car-rental` service, so the Angular app can make API calls to `/api/offers`, `/api/auction/participate`, etc.

## Healthchecks and caveats

- The `docker-compose.dev.yml` includes healthchecks for Postgres and the `car-rental` service. The car-rental healthcheck calls the actuator `/actuator/health` endpoint. If your runtime image does not include `curl`, the healthcheck may fail — in that case either:
    - install `curl` in the runtime image, or
    - change the healthcheck to use a simple TCP check (nc) or remove it for local development, or
    - run the app with a mounted development image that includes curl.

## Environment overrides

- You can override any Spring property by passing environment variables in the compose file (already wired for the datasource). For production, prefer using a secret manager or Kubernetes Secrets instead of plain env files.

Docker Compose is suitable for quick local development. For CI and production use Kubernetes manifests in `k8s/`.
