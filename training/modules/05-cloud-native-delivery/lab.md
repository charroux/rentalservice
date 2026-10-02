# Lab 05: Delivery evidence

## Goal

Audit the repository pipeline and define a release decision based on evidence.

## Prerequisites

- Modules 01 to 04 completed
- Familiarity with GitHub Actions syntax

## Tasks

1. Read `.github/workflows/ci.yml` and draw its dependency sequence.
2. Classify each step as build, verification, security, deployment, or cleanup.
3. Identify one artifact that lacks a unique version identifier.
4. Locate every environment variable carrying configuration.
5. Separate sensitive and non-sensitive values.
6. Define the logs, metrics, and probes needed before automatic promotion.
7. Write release and rollback criteria for the Rental service.

## Failure exercise

Assume the new image passes unit tests but fails its readiness probe after
deployment. Describe which pipeline step should stop and what evidence the team
needs before retrying.

## Evidence to submit

- The pipeline map
- The configuration classification
- Release and rollback criteria

## Review questions

- Which checks run before image creation?
- Can the team identify exactly which source revision produced an image?
- Which state survives replacement of an application container?
