Kubernetes manifests for the car-rental composite

Usage

1. Build container images for your services and push them to a registry. Example tags used in manifests:
   - charroux/car-rental:latest
   - charroux/agreement-service:latest
   - charroux/agreement-service-server:latest

2. Adjust images in the YAML files under `k8s/` to point to your registry if necessary.

3. Apply the kustomization:

```bash
kubectl apply -k k8s/
```

4. To remove the stack:

```bash
kubectl delete -k k8s/
```

Notes
- The PostgreSQL StatefulSet uses a 1Gi PVC. Change storageClassName or size to match your cluster.
- Production credentials are provided via `postgres-secret.yaml`; replace the base64 values with secure ones or use an external secret manager.
- The `car-rental` deployment activates the Spring `prod` profile via `SPRING_PROFILES_ACTIVE=prod` and reads datasource settings from the `postgres-credentials` secret. The app must respect these environment variables (Spring Boot does by default).
- For tests and local development you can keep using H2; the manifests here configure Postgres for production only.

Suggested workflow
- Locally: run Postgres with docker-compose for quick iteration, or use `minikube`/`kind` and adjust manifests.
- CI: build images, push to registry, then `kubectl apply -k k8s/` on your cluster.
