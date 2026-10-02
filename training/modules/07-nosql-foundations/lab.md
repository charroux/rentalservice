# Lab 07: NoSQL data modeling

## Goal

Design and compare NoSQL models from explicit rental access patterns.

## Prerequisites

- Basic database knowledge
- Module 01 recommended
- No Redis installation required

## Workload

The system must answer these questions:

1. Find a rental by identifier.
2. List active rentals for one customer, newest first.
3. Retrieve the current car and accepted insurance offer together.
4. Record auction events at high write volume.
5. Find the shortest relationship path between customers who shared vehicles.

## Tasks

1. Classify each question as a key-value, document, wide-column, graph, or relational fit.
2. Design a document representation for the first three questions.
3. Design partition and clustering keys for the auction-event workload.
4. Draw the vertices and edges required by the relationship query.
5. Identify duplicated fields in each design.
6. State the source of truth and repair strategy for every duplicate.
7. Define an atomicity boundary for accepting insurance.
8. Compare two candidate models in a short decision table.

## Partition exercise

Assume two regions lose connectivity. Decide whether each operation should
continue, return stale data, or reject the request:

- display a rental summary;
- accept an insurance offer;
- append an analytics event;
- change the assigned vehicle.

Explain the business consequence of each choice.

## Evidence to submit

- Three data models
- The duplication and repair table
- The partition decision record

## Review questions

- Which query determined each key or partition?
- Which update crosses aggregate boundaries?
- What new access pattern would force a model change?
