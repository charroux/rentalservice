# Lab 03: Failure and consistency patterns

## Goal

Analyze one failure-prone workflow and design bounded recovery behavior.

## Prerequisites

- Modules 01 and 02 completed
- Familiarity with local database transactions

## Tasks

1. Inspect the `processed_events` migration and entity.
2. Explain which database constraint makes duplicate handling safe.
3. Read the event publisher call site and mark the dual-write window.
4. Create a failure table with four cases: timeout, duplicate, malformed event, and unavailable Redis.
5. Define detection, retry, and operator action for each case.
6. Design an outbox record containing identity, type, version, payload, and publication state.
7. Write one test scenario that proves duplicate processing has no additional business effect.

## Failure injection

Stop Redis immediately before publication. Observe whether the business
transaction succeeds and decide what evidence would reveal a missing event.

## Evidence to submit

- The failure table
- The outbox schema
- The idempotence test scenario

## Review questions

- Which operations can be retried safely?
- What does the caller know after a timeout?
- Which local transaction must include the inbox record?
