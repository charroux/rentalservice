# Lab 10: Add a decoupled consumer

## Goal

Design a notification capability without modifying the rental producer or the
existing Insurance extension.

## Prerequisites

- Module 09 completed
- Basic Python, Java, or TypeScript knowledge

## Tasks

1. Read both files under `contracts/subscriptions`.
2. Create a proposed descriptor for `notification-service` and `AuctionWon` V1.
3. Define its ready and processing List names.
4. Design its inbox or processed-event uniqueness rule.
5. List the backend files the new service would own.
6. Inspect `generate_extensions.py` and the generated registry contract.
7. Design an Angular notification extension using only `RentalExtensionContext`.
8. Prove that the Rental publisher requires no source change.

## Failure injection

Assume the router crashes after writing to the Rental queue but before writing
to the Notification queue. Describe recovery, duplicate risk, and the evidence
each consumer must retain.

## Contract exercise

The notification service needs a customer email address that V1 does not carry.
Compare these options:

- enrich `AuctionWon` V1 in place;
- publish V2;
- let Notification query the owning service;
- publish a separate customer-contact event.

Choose one and state its coupling and privacy consequences.

## Evidence to submit

- The subscription descriptor
- Backend and frontend ownership lists
- The contract decision

## Review questions

- Can a new consumer deploy independently?
- Which shared element still creates coupling?
- Does the UI expose eventual consistency clearly?
