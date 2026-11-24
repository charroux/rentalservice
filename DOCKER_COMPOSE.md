# Local development with Docker Compose

The development stack is defined in `docker-compose.dev.yml`. It runs the
complete rental journey with the application services, their databases, and
the Redis event path used by the training repositories.

## Prerequisites

- Docker Desktop or Docker Engine with Compose v2
- ports 4200, 6379, 8080, 8081, 8082, 9090, 5432, and 27017 available
- the development credentials from `.env.dev`

The values in `.env.dev` are intended for local development only.

## Start the complete stack

Build the service images and start the stack:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up --build
```

Run it in the background:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up --build -d
```

Inspect service state and logs:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml ps
docker compose --env-file .env.dev -f docker-compose.dev.yml logs -f
```

## Services

| Compose service | Role | Host access |
|---|---|---|
| `frontend-angular` | Angular frontend served by nginx | <http://localhost:4200> |
| `car-rental` | Rental REST API and event producer | <http://localhost:8080> |
| `auction-service` | Auction gRPC server | `localhost:9090` |
| `event-router` | Redis List subscription router | internal port 8090 |
| `insurance-service` | Insurance event consumer and API | <http://localhost:8081> |
| `rental-query-service` | MongoDB rental projection and query API | <http://localhost:8082> |
| `redis` | Event transport | `localhost:6379` |
| `postgres` | Rental relational state | `localhost:5432` |
| `mongodb` | Insurance state and rental projections | `localhost:27017` |
| `mongodb-init` | One-shot MongoDB replica-set initialization | no host port |

Persistent data is stored in the named volumes `car_rental_redis_data`,
`car_rental_postgres_data`, and `car_rental_mongo_data`.

## Event path

The current event path is:

```text
car-rental
  -> events:simple:published
  -> event-router
  -> one Redis List per subscription
  -> rental, insurance, and query consumers
```

The router reads the versioned descriptors from `contracts/subscriptions`.
Event schemas and examples are stored under `contracts/events`.

Inspect the published List:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml exec redis \
  redis-cli LLEN events:simple:published
```

## Start only the infrastructure needed by a lab

Redis only:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d redis
```

Redis and MongoDB:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d \
  redis mongodb mongodb-init
```

Redis and PostgreSQL:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d \
  redis postgres
```

## Health checks

Compose waits for the infrastructure health checks before starting dependent
services. Check the Spring Boot services directly when diagnosing startup:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Follow one service:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml logs -f event-router
docker compose --env-file .env.dev -f docker-compose.dev.yml logs -f insurance-service
docker compose --env-file .env.dev -f docker-compose.dev.yml logs -f rental-query-service
```

## Rebuild one service

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml build --no-cache car-rental
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d car-rental
```

Replace `car-rental` with any Compose service that has a `build` section.

## Stop and clean up

Stop containers while preserving data:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml down
```

Remove containers and all three persistent data volumes:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml down -v
```

The `-v` form permanently deletes the local PostgreSQL, MongoDB, and Redis
data. Use it only when a lab or troubleshooting procedure requires a clean
state.
