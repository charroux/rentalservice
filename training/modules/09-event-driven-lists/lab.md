# Lab 09: Reliable events with Redis Lists

## Goal

Trace an `AuctionWon` event and prove recovery and idempotence behavior.

## Prerequisites

- Modules 07 and 08 completed
- The Docker Compose platform available

## Tasks

1. Validate the V1 example against its JSON Schema.
2. Start the platform and record all relevant List lengths.
3. Trigger an auction through the Angular application or REST API.
4. Capture the event envelope without modifying it.
5. Follow its movement through published, routing, ready, and processing Lists.
6. Locate the persisted business result and processed-event identity.
7. Replay the same envelope and verify that no second business effect appears.
8. Explain every point where a crash can cause retry or duplication.

## Failure injection

Stop the Insurance service, trigger another auction, and inspect its ready List.
Restart the service and verify eventual processing. Repeat by interrupting a
message after it reaches a processing List.

## Design exercise

Specify a dead-letter record containing the original payload, failure category,
attempt count, timestamps, and last error. Define who may replay it.

## Evidence to submit

- A sequence diagram with Redis commands
- Queue lengths before, during, and after recovery
- Proof of one business effect after duplicate delivery

## Review questions

- Which step provides transport safety?
- Which step provides business idempotence?
- What ordering guarantee does the workflow actually provide?
