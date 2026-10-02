# Lab 11: Operability and architecture review

## Goal

Evaluate the complete platform through measurable behavior and a controlled
failure drill.

## Prerequisites

- Modules 01 to 10 completed
- The Docker Compose platform available

## Tasks

1. Select two user journeys: auction completion and insurance acceptance.
2. Define one service-level indicator and objective for each journey.
3. List the logs, metrics, identifiers, and timestamps required to calculate them.
4. Record a healthy baseline from the running platform.
5. Inject one bounded dependency or consumer failure.
6. Observe queue state, HTTP behavior, persistence, and recovery.
7. Confirm whether the recovery meets the objective.
8. Complete an architecture scorecard covering reliability, evolvability, security, cost, and learnability.

## Failure drill options

- Stop the Insurance consumer while auctions continue.
- Restart Redis while AOF and the named volume remain enabled.
- Stop Auction during a synchronous rental request.
- Introduce an invalid event into a disposable consumer queue.

Choose one. State the abort condition before starting.

## Decision exercise

Recommend one next increment: dead-letter handling, transactional outbox,
distributed tracing, Redis Streams, or a CQRS projection. Use observed evidence
and identify what the increment will not solve.

## Evidence to submit

- Objective and indicator definitions
- A timestamped failure-drill record
- The architecture scorecard
- A one-page recommendation

## Review questions

- Could an operator detect the failure before a user reports it?
- Did the system recover without manual data correction?
- Which architectural claim remains untested?
