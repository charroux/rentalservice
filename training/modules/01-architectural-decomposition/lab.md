# Lab 01: Architectural decomposition

## Goal

Produce an evidence-based context map of the rental platform.

## Prerequisites

- A local clone of the repository
- A text editor
- Familiarity with Java packages and HTTP APIs

## Tasks

1. Read `settings.gradle` and list the executable services.
2. Inspect every controller and persistence entity.
3. Draw a context map containing Rental, Auction, Insurance, and the Angular application.
4. Label every connection with its protocol and direction.
5. Assign each database table to one service owner.
6. Find one piece of duplicated information and decide whether it is harmful or intentional.
7. Write a short ADR that either keeps or changes one service boundary.

## Failure question

Assume the auction service becomes unavailable for ten minutes. Identify which
business capabilities remain available and which boundary propagates the
failure.

## Evidence to submit

- The context map
- The data ownership table
- The ADR with one rejected alternative

## Review questions

- Does every service own a business capability?
- Does any service read another service's tables?
- Would a modular monolith reduce accidental complexity at this stage?
