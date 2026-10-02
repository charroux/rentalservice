# Lab 04: Docker Compose topology

## Goal

Build, start, inspect, and troubleshoot the complete local platform.

## Prerequisites

- Docker Desktop or Docker Engine with Compose
- Modules 01 to 03 completed

## Tasks

1. Run `docker compose --env-file .env.dev -f docker-compose.dev.yml config`.
2. Draw the resulting services, ports, volumes, and network.
3. Build all images without starting containers.
4. Start the platform and wait for health checks.
5. Inspect logs for Rental, Auction, Event Router, and Insurance.
6. Resolve the Redis hostname from inside one application container.
7. Restart PostgreSQL and verify that rental data persists.
8. Stop the stack without deleting volumes.

## Failure injection

Start the application services while PostgreSQL remains unavailable. Record the
health status and recovery behavior after PostgreSQL starts.

## Evidence to submit

- The Compose topology
- Health status before and after dependency recovery
- One log excerpt that identifies a failed dependency

## Review questions

- Which ports require host exposure?
- Which services need persistent volumes?
- Does startup order guarantee readiness?
