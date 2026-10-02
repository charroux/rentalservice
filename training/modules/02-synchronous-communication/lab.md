# Lab 02: REST and gRPC

## Goal

Trace one user request across the synchronous boundaries and specify its
failure contract.

## Prerequisites

- Module 01 completed
- Java 21 or Docker available

## Tasks

1. Inspect `CarRentalRestService` and list its public endpoints.
2. Find the `.proto` file used by the auction service.
3. Match one generated gRPC method to its server implementation.
4. Start the platform and execute one successful auction request.
5. Capture the HTTP request, gRPC operation, and final response.
6. Stop the auction service and repeat the request.
7. Propose an HTTP error representation and a maximum response deadline.

## Design exercise

Decide whether the insurance acceptance operation should use REST, gRPC, or an
asynchronous message. State the required user feedback and consistency before
choosing.

## Evidence to submit

- A sequence diagram for success and failure
- The observed status code and error body
- A proposed timeout budget

## Review questions

- Which contract is easiest for a browser to consume?
- Which call creates temporal coupling?
- Would retrying the auction request be safe without an idempotency key?
