# Lab 08: Redis data and messaging

## Goal

Manipulate Redis structures and connect their behavior to the rental platform.

## Prerequisites

- Docker
- `redis-cli`, either locally or inside the Redis container
- Module 07 completed

## Part A: Data structures

1. Create a String counter and increment it atomically.
2. Store rental summary fields in a Hash.
3. Store unique vehicle features in a Set.
4. Rank insurance offers in a Sorted Set by daily price.
5. Add expiration to a temporary key and observe its lifetime.
6. Record the commands and resulting types.

## Part B: Persistence

1. Read the Redis command and volume configuration in `docker-compose.dev.yml`.
2. Write a persistent key.
3. Restart the Redis container without deleting its volume.
4. Verify the key remains available.
5. Explain what would differ after `docker compose down -v`.

## Part C: Messaging comparison

1. Publish a value before starting a Pub/Sub subscriber and record the result.
2. Push a value to a List before starting its consumer.
3. Inspect the platform's published, routing, ready, and processing Lists.
4. Use `LMOVE` manually on a disposable pair of Lists.
5. Compare offline-consumer and replay behavior.

## Failure injection

Move a disposable message into a processing List and stop before acknowledging
it. Design the recovery command and state the duplicate-processing risk.

## Evidence to submit

- A command transcript for every structure
- Persistence observations
- A Pub/Sub, Lists, and Streams comparison table

## Review questions

- Which keys need a TTL?
- Which Lists require depth alerts?
- Which Redis configuration assumptions affect durability?
