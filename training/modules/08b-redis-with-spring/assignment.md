# Independent assignment 08b+: From delivery to reliable processing

## Purpose

The supplied demonstration deliberately uses a destructive `LPOP`. Your task
is to evolve it into an at-least-once delivery protocol, prove its behavior
under failure, and identify which messaging concerns the application is now
implementing itself.

Work from the code in `training/demos/redis-list-spring`. Do not reuse the
production rental platform's consumer or router implementation.

Estimated core work: 3–5 hours. The extension tracks are optional.

## Starting point

The initial implementation has one queue:

```text
tutorial:v1:rental-events
```

The producer appends with `RPUSH`. The insurance consumer removes with `LPOP`
and then creates an in-memory offer.

This sequence contains a loss window:

```text
LPOP succeeds → consumer crashes → offer is not stored → event is gone
```

Create a personal branch before changing the demonstration, for example:

```text
lab/08b-reliable-delivery
```

## Learning outcomes

After completing the core assignment, you should be able to:

- distinguish delivery from successful business processing
- implement acknowledgement as an application protocol over Redis Lists
- recover work left incomplete by a failed consumer
- explain why recovery changes the semantics to at-least-once
- make the business effect idempotent using a stable event identifier
- evaluate the guarantees that Redis does and does not provide

## Core increment 1: Ready and processing Lists

Replace the single queue with two versioned keys:

```text
tutorial:v2:rental-events:ready
tutorial:v2:rental-events:processing
```

The producer must append new events to `ready`.

The consumer must atomically transfer one event from the left of `ready` to the
right of `processing`. Use the Spring Data Redis operation corresponding to:

```text
LMOVE ready processing LEFT RIGHT
```

Do not implement this as a separate pop followed by a push: a crash between
two commands would recreate the loss window.

### Acceptance criteria

- An event exists in exactly one of the two Lists before business processing.
- Moving an event never makes it temporarily absent from both Lists.
- FIFO order is preserved for newly published events.
- An empty ready List causes no error and creates no offer.
- A unit test verifies the Redis operation and its source/destination
  directions.

::: {.callout-tip collapse="true" title="Hints for increment 1"}
1. Start from `RentalEventPublisher.QUEUE` and
   `RentalEventConsumer.QUEUE`. Keep one key constant per service instead of
   scattering literals across methods.
2. Inspect the overloads of `ListOperations.move`. Spring Data represents the
   Redis `LEFT` and `RIGHT` arguments with `RedisListCommands.Direction`.
3. Write the desired queue state on paper before choosing directions. For
   `RPUSH` plus consumption from the left, determine which side of `processing`
   also supports ordered recovery.
4. Mock `ListOperations<String, String>` as the supplied tests do. Verify the
   source key, destination key, and both directions—not only that `move` ran.
5. `LMOVE` returns `null` when the source is empty or absent. Keep the early
   return already used for `leftPop`.
:::

## Core increment 2: Acknowledge only after success

After the offer has been stored successfully, remove exactly one matching event
from `processing`.

The expected logical sequence is:

```text
atomic move → deserialize → validate → calculate → store → acknowledge
```

If deserialization, validation, calculation, or storage fails, the event must
remain visible in `processing`.

### Questions to answer

1. Why is the List move not itself an acknowledgement?
2. Why must acknowledgement happen after the business state change?
3. What does a result of zero from the removal operation mean?
4. What risk appears if two List elements contain exactly the same JSON?
5. Why is `eventId` a better identity than comparing only business fields?

### Acceptance criteria

- A successful operation removes one processing entry.
- A failed business operation leaves the entry in `processing`.
- The consumer logs an acknowledgement failure rather than silently ignoring
  it.
- Automated tests cover success and failure before acknowledgement.

::: {.callout-tip collapse="true" title="Hints for increment 2"}
1. Keep the raw JSON returned by `move`; it is both the value to deserialize
   and the value initially available for acknowledgement.
2. `ListOperations.remove(key, count, value)` maps to `LREM`. Determine which
   positive count removes at most one matching entry.
3. Put acknowledgement after the business-state change. Do not put it in a
   `finally` block, which also executes after a failure.
4. Make the business operation throw in a unit test, then verify that removal
   was never invoked.
5. Treat a `null` or unexpected removal count as a protocol anomaly rather than
   silently reporting success.
:::

## Core increment 3: Failure injection and recovery

Add a controlled failure mechanism for teaching and tests. It may be a Spring
property, a dedicated test double, or a narrowly scoped diagnostic endpoint.
It must not depend on manually killing a process at an unpredictable instant.

Demonstrate these failure points:

| Failure point | Expected queue state | Expected business state |
|---|---|---|
| Before the move | Event remains ready | No offer |
| After the move, before storage | Event remains processing | No offer |
| After storage, before acknowledgement | Event remains processing | Offer exists |
| After acknowledgement | Event is in neither List | Offer exists |

Implement recovery on consumer startup. It must return abandoned processing
entries to the ready List while preserving their processing order as far as
the chosen left/right directions allow.

For this exercise, assume only one logical consumer instance performs startup
recovery. In your report, explain why blindly recovering a shared processing
List is unsafe while another instance may still be working.

### Acceptance criteria

- Recovery uses an atomic Redis List operation.
- A test places multiple events in processing and verifies their recovered
  order.
- Restarting after a pre-storage failure eventually creates the offer.
- Restarting after a post-storage failure attempts the event again.

::: {.callout-tip collapse="true" title="Hints for increment 3"}
1. Keep failure injection outside the event contract. Prefer a test double or
   Spring property to a field every production consumer must understand.
2. `ApplicationReadyEvent` can trigger recovery after Spring builds the
   application context.
3. Recovery can repeat an atomic move until Redis returns `null`. State why a
   production design also needs an ownership or time limit.
4. Simulate processing with three distinguishable values: A, B, and C. Execute
   the moves with `redis-cli` to reveal whether recovery reverses their order.
5. A scheduled recovery beside active consumers may reclaim legitimate work.
   The core exercise avoids that race; Extension B removes the assumption.
:::

## Core increment 4: Idempotent business processing

Recovery after storage but before acknowledgement produces a duplicate
delivery. Use `eventId` to prevent a second business effect.

For the core assignment, choose one of these designs:

### Track A: Redis Set

Record processed identifiers in:

```text
tutorial:v2:insurance:processed-events
```

This track reuses `SISMEMBER` and `SADD`. It is simple, but the processed marker
and the in-memory offer update are not one atomic durable transaction.

### Track B: Local database constraint

Persist offers and processed event identifiers in H2 or PostgreSQL. Enforce a
unique constraint on `eventId` and update the offer and processed-event record
inside one local transaction.

This track requires more code but produces a stronger business guarantee.

Whichever track you choose, document exactly what happens when the consumer
crashes between each state change.

### Acceptance criteria

- Processing the same `eventId` twice produces one offer.
- A duplicate is acknowledged after its previous successful effect is found.
- Two different event IDs are not treated as duplicates merely because they
  contain the same rental data.
- An automated test proves idempotence.

::: {.callout-tip collapse="true" title="Hints for increment 4"}
1. Test the difficult sequence: store the offer, fail before acknowledgement,
   recover the event, and process it again.
2. For Track A, compare check-then-add with the return value of `SADD`. Ask
   which behaves better when two consumers race.
3. A Redis marker and an in-memory offer cannot be committed in one durable
   transaction. Do not claim exactly-once business processing.
4. For Track B, make `eventId` unique in the database. An existence check may
   optimize processing, but the constraint is the final concurrency guard.
5. A duplicate still needs acknowledgement after its business effect is
   skipped.
:::

## Core increment 5: Evaluate the result

Submit a guarantee table for the original and evolved implementations:

| Question | Version 1: `LPOP` | Version 2: move and acknowledge |
|---|---|---|
| Can an event be lost after removal? | | |
| Can an event be delivered twice? | | |
| Is acknowledgement explicit? | | |
| Can abandoned work be inspected? | | |
| Is recovery safe with several instances? | | |
| Can consumed history be replayed? | | |

Your conclusion must distinguish:

- Redis command atomicity;
- end-to-end business atomicity;
- delivery semantics;
- business idempotence;
- durability and replay.

::: {.callout-tip collapse="true" title="Hints for the guarantee table"}
- Avoid “exactly once” unless delivery and the business state transition share
  one enforceable atomic boundary.
- An atomic Redis command does not make a later in-memory or SQL update atomic.
- At-least-once describes delivery attempts; idempotence describes the effect
  of repeated attempts.
- Persistence preserves current state. Replay requires retained history or
  another source from which events can be read again.
:::

## Extension A: Invalid events and a dead-letter List

Handle these permanent errors:

- malformed JSON;
- missing `rentalId`;
- unsupported `eventVersion`;
- negative `dailyPrice`.

After a documented number of attempts, move the event to:

```text
tutorial:v2:rental-events:dead-letter
```

Provide an inspection endpoint or documented CLI procedure. Explain why an
invalid event should not cycle between ready and processing forever.

::: {.callout-tip collapse="true" title="Hints for dead-letter handling"}
- Separate permanent parsing or validation errors from transient failures.
- Preserve the original payload, but consider an envelope containing the
  failure reason, attempt count, and failure time.
- Moving to dead letter and removing from processing are two state changes.
  Identify the new crash window before choosing an implementation.
:::

## Extension B: Retry metadata and visibility timeout

Design retry ownership and timing for multiple consumer instances. Your design
must answer:

- Which consumer owns an event?
- When did processing begin?
- When is another consumer allowed to recover it?
- Where is the attempt count stored?
- How is recovery made safe against an active but slow consumer?

You may use a Sorted Set whose score is a processing deadline. Implementation
is optional; a precise design and race-condition analysis are sufficient.

Identify the broker-like features you are rebuilding around Redis Lists.

::: {.callout-tip collapse="true" title="Hints for visibility timeouts"}
- A Sorted Set can map `eventId` to a deadline expressed as epoch time.
- Metadata still needs a home: consider a Hash or envelope indexed by
  `eventId`.
- A recovery worker should claim only expired work and expect another worker
  to race for the same candidate.
- Compare the design with pending-entry ownership in Redis Streams.
:::

## Extension C: Independent business consumers

Add a minimal audit or notification consumer that must receive every rental
event independently of the insurance consumer.

First demonstrate why attaching both services to the same ready List is
incorrect: they compete and each event reaches only one service.

Then compare at least three possible solutions:

- producer-side duplication;
- a router and one List protocol per service;
- Redis Streams with one consumer group per service.

You may implement one solution, but your decision record must discuss coupling,
recovery, adding consumers, and replay.

::: {.callout-tip collapse="true" title="Hints for fan-out"}
- Publish six numbered events, attach both business consumers to one List, and
  record which service receives each number.
- Scaling one logical service and notifying two independent services are
  different requirements, even though both involve several processes.
- Draw which components must change when a third consumer is added. That
  dependency reveals producer coupling.
:::

## Extension D: Transactional Outbox

Assume the producer must store the rental in SQL and publish its event. Analyze:

```text
SQL commit succeeds → Redis publication fails
Redis publication succeeds → SQL transaction rolls back
```

Design or implement a Transactional Outbox:

```text
one SQL transaction
├── INSERT rental
└── INSERT outbox_event

asynchronous publisher
└── unpublished outbox rows → Redis
```

State how the publisher marks an outbox row as sent, what happens if it crashes
after publishing, and why the consumer must still be idempotent.

::: {.callout-tip collapse="true" title="Hints for the outbox"}
- Commit the rental row and outbox row in the same database transaction.
- Poll only unpublished rows and use a stable outbox/event identifier.
- Publishing and marking a row as sent are not one cross-system atomic action.
  A crash between them causes a retry and possibly a duplicate.
- Define policies for ordering, cleanup, concurrent publishers, and permanently
  failing rows.
:::

## Optional comparison: Redis Streams

Do not replace the List implementation. Create a comparison branch or package
that maps the same event contract to:

- `XADD`;
- `XREADGROUP`;
- pending entries;
- `XACK`;
- stale-message claiming.

Identify which application mechanisms from version 2 become Redis Stream
features and which business responsibilities remain in application code.

::: {.callout-tip collapse="true" title="Hints for the Streams comparison"}
- Compare the ready List with new Stream entries, the processing List with
  pending entries, `LREM` with `XACK`, and custom recovery with stale claiming.
- Streams track delivery state, but do not make an SQL or in-memory business
  update atomic with acknowledgement.
- Use a separate key namespace so both transports run with the same event JSON.
:::

## Required evidence

Submit:

- the modified source and automated tests;
- a sequence diagram for normal processing;
- a sequence diagram for failure after storage but before acknowledgement;
- CLI evidence showing ready and processing states;
- recovery-order evidence with at least three events;
- an idempotence test using the same event twice;
- the completed guarantee table;
- a concise architecture decision record describing remaining limitations.

## Suggested rubric

| Criterion | Points |
|---|---:|
| Atomic ready-to-processing move | 4 |
| Acknowledgement after successful business processing | 3 |
| Deterministic failure injection and recovery | 3 |
| Demonstrated idempotence | 4 |
| Automated failure tests | 3 |
| Analysis of guarantees and remaining limits | 3 |
| **Total** | **20** |

Optional extensions can replace missing core points only at the instructor's
discretion; they do not compensate for an unsafe core protocol.
