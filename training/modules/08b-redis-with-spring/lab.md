# Lab 08b: Redis Lists from Spring Boot

## Goal

Run two isolated Spring Boot services connected by one Redis List, relate each
Java operation to its Redis command, and expose the limits of destructive
consumption.

Estimated time: 60–75 minutes.

## Prerequisites

- Docker with Compose
- `curl.exe` on Windows, or an equivalent HTTP client
- Module 08 completed
- The repository root as the current directory

The demonstration is under `training/demos/redis-list-spring`. It does not use
the rental platform containers or its Redis instance.

## Part A: Read the producer

1. Locate `spring-boot-starter-data-redis` in the producer build file.
2. Identify the Redis host and port used locally and inside Compose.
3. Find the event record and list its metadata and business fields.
4. Find the `StringRedisTemplate` call corresponding to `RPUSH`.
5. Explain why `queueSize` confirms insertion but not event processing.
6. Explain why explicit JSON is preferable to Java native serialization here.

## Part B: Start only Redis and the producer

```powershell
cd training\demos\redis-list-spring
docker compose up --build -d redis rental-producer
docker compose ps
```

Create one rental:

```powershell
curl.exe -X POST http://localhost:8091/rentals `
  -H "Content-Type: application/json" `
  -d '{"rentalId":"RENT-42","vehicleId":"CAR-7","dailyPrice":62}'
```

Record the HTTP status, `eventId`, and `queueSize`.

## Part C: Cross the Java/CLI boundary

Before starting the consumer, use `redis-cli` inside the tutorial container to:

1. verify that `tutorial:v1:rental-events` exists;
2. determine its Redis type;
3. measure its length;
4. display its first ten values;
5. identify every field in the JSON written by Java.

Record the CLI commands and results. Explain why the event remains available
while the consumer is offline.

## Part D: Read and start the consumer

1. Find the `StringRedisTemplate` call corresponding to `LPOP`.
2. Explain the `null` result when the queue is empty.
3. Describe how `@Scheduled` determines the polling rhythm.
4. Calculate the expected premium for a daily rental price of 62 EUR.

Start the consumer:

```powershell
docker compose up --build -d insurance-consumer
docker compose logs --tail 50 insurance-consumer
```

Query the calculated offer:

```powershell
curl.exe http://localhost:8092/insurance/offers/RENT-42
```

Verify with `LLEN`, `EXISTS`, and `TYPE` what happened to the List after its last
element was consumed.

## Part E: Observe ordering

Stop only the consumer, then publish three rentals in this order:

| Rental | Vehicle | Daily price |
|---|---|---:|
| `RENT-101` | `CAR-A` | 40 |
| `RENT-102` | `CAR-B` | 80 |
| `RENT-103` | `CAR-C` | 120 |

1. Inspect the List with `LRANGE` and verify insertion order.
2. Restart the consumer.
3. Follow its logs and record processing order.
4. Query all three offers and verify the premium calculation.
5. Explain why `RPUSH` combined with `LPOP` implements FIFO.

## Part F: Competing consumers

Keep the normal consumer running. In a second terminal, start an additional
temporary instance without publishing its HTTP port:

```powershell
docker compose run --rm --no-deps insurance-consumer
```

Publish at least six distinct rentals while both instances are running.

1. Follow the logs from both consumer containers.
2. Record which instance receives each rental.
3. Verify that no event is delivered to both instances.
4. Explain why this behavior is appropriate for scaling one logical service.
5. Explain why it cannot notify two different business services independently.

Stop the temporary instance with `Ctrl+C` after recording the distribution.

## Part G: Reason about failure

Assume that the consumer crashes immediately after `leftPop` and before
`store.save`.

1. Draw the state of Redis and the insurance store.
2. Can restarting the consumer recover the event? Explain.
3. Identify the exact destructive operation responsible for the loss window.
4. Propose a protocol using `ready`, `processing`, `LMOVE`, and `LREM`.
5. Explain why that protocol can redeliver an event.
6. State the information and storage constraint needed for idempotence.

Do not implement the reliable protocol in this lab: it belongs to the next
architectural increment.

## Cleanup

From `training/demos/redis-list-spring`:

```powershell
docker compose down -v
```

Confirm that the rental platform containers, if any, are unaffected.

## Evidence to submit

- Producer and consumer code locations for every Redis operation
- HTTP and Redis CLI transcript for one complete event
- FIFO observations for the three-rental experiment
- Competing-consumer distribution table
- Failure-window diagram and proposed reliable protocol
- A short comparison of the tutorial and the current rental platform

## Review questions

1. Which responsibilities belong to Spring Boot, Spring Data Redis, Lettuce,
   Jackson, and Redis itself?
2. Why does using JSON not eliminate the need for a versioned contract?
3. What does HTTP `202 Accepted` guarantee in this demonstration?
4. What delivery semantics does `LPOP` provide if the consumer can crash?
5. At what point would a router or Redis Streams become useful?

## Continue independently

The [08b+ reliability assignment](assignment.md) starts from this demonstration
and adds explicit acknowledgement, crash recovery, and idempotence. Its
advanced tracks explore dead-letter handling, fan-out, the Transactional Outbox,
and Redis Streams without changing the core lab.
