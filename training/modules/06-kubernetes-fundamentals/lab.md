# Lab 06: Kubernetes desired state

## Goal

Deploy the platform locally and observe reconciliation and service discovery.

## Prerequisites

- Docker
- `kubectl`
- Kind or Minikube
- Modules 01 to 05 completed

## Tasks

1. Run `kubectl kustomize k8s/overlays/kind` or the Minikube overlay.
2. Identify Deployments, StatefulSets, Services, configuration, and storage.
3. Create the local cluster with the repository script.
4. Deploy the platform and inspect Pods and endpoints.
5. Resolve the Redis Service name from an application Pod.
6. Delete one Rental Pod and watch the Deployment recreate it.
7. Compare readiness and liveness behavior during the replacement.
8. Trace one request through the gateway.

## Failure injection

Scale the Auction Deployment to zero, issue a rental request, and inspect the
result from the browser boundary and from application logs.

## Evidence to submit

- A table mapping workload, Service, probe, and storage
- Reconciliation observations after Pod deletion
- The request path through the gateway

## Review questions

- Which names remain stable when Pods change?
- Which data survives Pod replacement?
- Which controller implements external routing in this cluster?
