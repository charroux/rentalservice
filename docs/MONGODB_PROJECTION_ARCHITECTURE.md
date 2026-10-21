# MongoDB aggregate and rental projection

## Ownership and responsibilities

- `insuranceService` owns `insurance.insurance_offers`. `InsuranceOffer` is an
  authoritative business aggregate; accepting it is a command and a
  state transition. A unique `rental_id` index enforces one current offer per
  rental.
- `insuranceService` also owns `insurance.insurance_outbox`. The aggregate and
  its publication intent are committed in one MongoDB transaction.
- `rentalQueryService` owns `rental_query.rental_views` and
  `rental_query.projection_event_archive`. It accepts no business commands. Its
  views are derived, denormalized, eventually consistent, and rebuildable from
  its append-only archive.
- Both logical databases may use the same MongoDB server, but neither service
  reads or writes the other's collections.

## Event flow

`AuctionWon v1` is unchanged. `InsuranceOfferProposed v1` and
`InsuranceOfferAccepted v1` use the standard `eventId`, `eventType`,
`eventVersion`, and `occurredAt` envelope fields. The insurance transaction
inserts an outbox document; a retrying relay later appends its payload to the
existing published Redis List. Delivery is therefore at least once.

Subscription descriptors fan all three event types into the query service's
own ready List. The consumer atomically moves a message to its processing List.
In one MongoDB transaction, it inserts the immutable raw event into
`projection_event_archive` and updates `rental_views`; only then does it
acknowledge the List message. The archive's unique `event_id` makes duplicate
delivery a no-op without growing every view document. Insurance acceptance can
arrive first and create a partial view; later proposal data fills missing
metadata without changing `ACCEPTED` back to `PROPOSED`.

## Durable reconstruction source

Redis Lists remain transport queues, not history: acknowledged messages are
removed and AOF does not retain them as a replay log. The durable reconstruction
source chosen for this increment is therefore the query service's MongoDB
`projection_event_archive`. This is the smallest extension compatible with the
current architecture and avoids introducing Redis Streams in this project.

To rebuild, first stop the normal query consumer and start an isolated query
instance against the target database with:

```bash
EVENTS_CONSUMER_ENABLED=false
PROJECTION_REBUILD_FROM_ARCHIVE=true
```

The bootstrap deletes only `rental_views`, replays archived payloads in receive
order, and leaves the archive intact. Compare archive/view counts and sampled
rentals before switching query traffic and re-enabling consumption. The service
refuses rebuild mode while the consumer is enabled.

For events predating deployment of the archive, a complete external NDJSON
export can be imported once with `PROJECTION_BOOTSTRAP_PATH`. Each line passes
through the same archive-plus-projection transaction, so duplicates are safe.
Without either the Mongo archive or a complete historical export, old facts
consumed from Redis Lists cannot be recovered.

Redis Streams with adequate retention can be evaluated in a separate project.
It would provide transport IDs, consumer groups, pending entries, and replay
from a chosen point, but would not remove the need for schema compatibility,
idempotent projectors, poison-event handling, or projection-version cutover.

## DLQ and retention policy

- Deterministically invalid JSON, unsupported contracts, invalid dates, and
  malformed payloads move atomically from processing to a service-specific
  Redis DLQ, with original payload, source queue, error, and failure time.
- Infrastructure failures remain in processing and are returned to ready for
  retry. This prevents transient MongoDB or Redis outages from becoming DLQ
  messages.
- DLQ records are retained until an operator fixes/replays or explicitly
  discards them. Alert on any arrival and on queue age.
- The projection archive has no TTL: it is the rebuild source. Back it up,
  monitor collection/index growth, and introduce a versioned snapshot plus a
  verified retention boundary before pruning anything.
- Published outbox rows are retained for audit initially. At larger volume,
  archive or delete only rows older than an operationally agreed window after
  confirming publication and downstream recovery coverage.

## Remaining operational limits

- Redis List processing is designed for one active consumer instance per
  service; horizontal consumption needs stronger claiming/lease semantics.
- Outbox publication is at least once: a Redis append followed by a failed
  `published_at` update can publish a duplicate. Stable event IDs and the query
  archive make that safe.
- MongoDB transactions require the replica set configured by Compose. A
  standalone MongoDB process is not sufficient.
