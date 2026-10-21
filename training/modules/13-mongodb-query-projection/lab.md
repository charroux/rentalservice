# Lab 13: MongoDB as a query projection

## Goal

Build, disturb, inspect, and reason about a denormalized rental read model.

## Tasks

1. Trace all three V1 event contracts and projection subscriptions.
2. Trigger an auction and query `/rental-views/{rentalId}` during convergence.
3. Accept insurance and observe the view change from absent/proposed to accepted.
4. Redeliver each envelope and prove one effect per `eventId`.
5. Deliver accepted, auction, then proposed and verify convergence.
6. Stop the query service, create a rental, restart it, and observe List recovery.
7. Inspect the Angular extension and document its bounded polling.

## Reconstruction exercise

After generating several views, stop the query consumer and start an isolated
instance with `EVENTS_CONSUMER_ENABLED=false` and
`PROJECTION_REBUILD_FROM_ARCHIVE=true`. Verify that only `rental_views` is
replaced and that the result matches the archived facts.

Then create an NDJSON file with a pre-archive event and import it with
`PROJECTION_BOOTSTRAP_PATH`. Explain why this import is a migration path while
the Mongo archive is the normal durable reconstruction source.

## Design extension

Propose a Redis Streams migration with stream key, consumer group, retention,
checkpoint/replay procedure, and projection-version replacement. Do not remove
the current Lists path in this exercise.

## Evidence to submit

- architecture and adverse-order diagrams;
- API responses for absent, partial, and enriched views;
- duplicate and recovery test output;
- rebuild runbook and retained-log proposal.

## Review questions

- Which database is authoritative for insurance acceptance?
- When is denormalization worth its operational cost?
- What exact source can rebuild the projection today, and why has it no TTL?
