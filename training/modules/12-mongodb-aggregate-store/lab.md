# Lab 12: MongoDB as an aggregate store

## Goal

Migrate and verify the insurance aggregate without confusing it with a read projection.

## Tasks

1. Start MongoDB, Redis, the router, and `insuranceService`.
2. Inspect `insurance.insurance_offers` and identify the aggregate boundary.
3. List indexes and explain why `event_id` and `rental_id` are unique.
4. Deliver the same `AuctionWon v1` envelope twice and prove one document remains.
5. Accept the offer twice; verify one state transition and one stable accepted event identity.
6. Add a hypothetical optional coverage field and propose a rolling schema-evolution plan.
7. Compare the former SQL model with the document in a decision table.

## Failure investigation

Interrupt the service after the MongoDB transaction but before the outbox relay
publishes. Restart it and prove that the pending event is published. Then model
a Redis append followed by a failed `published_at` update and explain why a
duplicate is possible but safe downstream.

## Evidence to submit

- document and index output;
- duplicate-delivery proof;
- state-transition timeline;
- schema-evolution decision record.

## Review questions

- Why is `InsuranceOffer` a source-of-truth aggregate?
- Which invariant fits inside one document?
- Why does the transaction require a MongoDB replica set?
