# Cloud native app

<div id="user-content-toc">
  <ul>
    <li><a href="#start-the-app">1. Start the app</a>
        <ul>
            <li><a href="#Requirements">1.1. Requirements</a>
                <ul>
                    <li><a href="#install-docker-and-minikube">1.1.1. Install Docker and Minikube</a></li>
                    <li><a href="#install-istio">1.1.2. Install Istio</a></li>
                    <li><a href="#build-the-docker-images">1.1.3. Build the Docker images (optional)</a></li>
                </ul>
            </li>
            <li><a href="#Start-the-docker-daemon">1.2. Start the Docker daemon</a></li>
            <li><a href="#Start the Minikube cluster">1.3. Start the Minikube cluster</a></li>
            <li><a href="#Deploy-the-app-in-the-minikube-cluster">1.4. Deploy the app in the Minikube cluster</a>
                <ul>
                    <li><a href="#Deploy-the-database-and-microservices-gateway">1.4.1. Deploy the database and microservices gateway</a></li>
                    <li><a href="#Deploy-the-microservices-and-service-mesh-proxies">1.4.2. Deploy the microservices and service mesh proxies</a></li>
                    <li><a href="#Get-the-access-to-the-ingress-gateway">1.4.3. Get the access to the Ingress gateway</a></li>
                </ul>
            </li>
        </ul>
    </li>
    <li><a href="#microservices">2. Microservices</a></li>
    <li><a href="#full-duplex-asynchronous-exchange-via-grpc">3. Full duplex asynchronous exchange via gRPC</a>
        <ul>
            <li><a href="#service-contract">3.1. Service contract</a></li>
        </ul>
    </li>
    <li><a href="#data-consistency--distributed-transaction-1">4. Data consistency / distributed transaction</a>
        <ul>
            <li><a href="#the-saga-pattern">4.1. The saga pattern</a></li>
        </ul>
    </li>
    <li><a href="#continuous-integration-with-github-actions">5. Continuous Integration with Github Actions</a>
        <ul>
            <li><a href="#the-ci-workflow">5.1. The CI workflow</a></li>
            <li><a href="#build-and-tests">5.2. Build and tests</a></li>
            <li><a href="#launch-a-workflow-when-the-code-is-updated">5.3. Launch a workflow when the code is updated</a></li>
        </ul>
    </li>
    <li><a href="#kubernetes">6. Kubernetes</a>
        <ul>
            <li><a href="#Pod-and-service">6.1. Pod and Service</a></li>
            <li><a href="#scalability-and-load-balancing-1">6.2. Scalability and load balancing</a></li>
            <li><a href="#auto-restart-in-case-of-failure">6.3. Auto restart in case of failure</a></li>
            <li><a href="#chech-the-health-of-an-application">6.4. Chech the health of an application</a></li>
        </ul>
    </li>
    <li><a href="#service-mesh">7. Service mesh</a>
        <ul>
            <li><a href="#microservices-service-mesh-proxies-and-routing-via-the-gateway">7.1. Microservices, service mesh proxies and routing via the gateway</a></li>
            <li><a href="#circuit-breaker">7.2. Circuit breaker</a></li>
            <li><a href="#monotoring">7.3. Monotoring</a>
                <ul>
                    <li><a href="#display-the-kiali-dashboard">7.3.1. Display the Kiali dashboard</a></li>
                    <li><a href="#Monitoring-with-graphana">7.3.2. Monitoring with Graphana</a></li>
                </ul>
            </li>
        </ul>
    </li>
    <li><a href="#composition-of-services-via-graphql">8. Composition of services via GraphQL</a></li>
    <li><a href="#Delete-resources-and-stop-the-cluster">9. Delete resources and stop the cluster</a></li>
  </ul>
</div>

## Start the app

### Start the Docker Daemon

Start Docker

### Start the Minikube cluster
```
minikube start --cpus=2 --memory=5000 --driver=docker
```
### Deploy the app in the Minikube cluster

Set the Kubernetes config
```
kubectl apply -f configmap.yaml
```
Set the Kubernetes secret
```
kubectl apply -f secret.yaml
```
##### Deploy the database and microservices gateway
<img src="images/servicemesh.png">

Launch the Kubernetes deployment and service for PostgreSQL, and the Ingress gataway:
```
kubectl apply -f infrastructure.yaml
```
##### Deploy the microservices and service mesh proxies

```
kubectl apply -f microservices.yaml
```
##### Get the access to the Ingress gateway
```
kubectl -n istio-system port-forward deployment/istio-ingressgateway 31380:8080
```
Get the list of cars to be rented:
```
http://localhost:31380/carservice/cars
```
Rent 2 cars:
```
curl --header "Content-Type: application/json" --request POST --data '{"customerId":1,"numberOfCars":2}' http://localhost:31380/carservice/cars
```

## Microservices

<img src="images/carservicearchi.png">

https://github.com/charroux/servicemesh/tree/main/carservice/src/main/java/com/charroux/carservice

## Full duplex asynchronous exchange via gRPC   

<img src="images/full_duplex.png">

### Service contract
https://github.com/charroux/servicemesh/blob/main/carservice/src/main/proto/carservice.proto

### gRPC client
https://github.com/charroux/servicemesh/blob/main/carservice/src/main/java/com/charroux/carservice/service/RentalServiceImpl.java

### gRPC server
https://github.com/charroux/servicemesh/blob/main/carstat/src/main/java/com/charroux/carstat/CarRentalServiceImpl.java

## Data consistency / distributed transaction
### The saga pattern
<img src="images/sagapattern.png">

<img src="images/saga.png">

https://github.com/charroux/servicemesh/blob/main/carservice/src/main/java/com/charroux/carservice/service/RentalServiceImpl.java

## Kubernetes

### Pod and Service

<img src="images/pod_service.png">

```
kubectl get pods
```
```
kubectl get services
```

https://github.com/charroux/servicemesh/blob/main/infrastructure.yaml

Enter inside the Docker containers:
```
kubectl exec -it [pod name] -- /bin/sh
```
```
ls
```
You should view the java jar file.
```
exit
```
Use Minikube to reach the carservice:
```
minikube service carservice --url
```
```
http://127.0.0.1:[port]/cars
```


### Scalability and load balancing

<img src="images/scaling.png">

How many instance are actually running:
```
kubectl get pods
```
```
kubectl get deployments
```
Start a second instance:
```
kubectl scale --replicas=2 deployment/[deployment name]
```

### Auto restart in case of failure
```
kubectl get pods
```
```
kubectl delete pods [pod name]
```

### Chech the health of an application
A probe is a mechanism used to determine the health of an application running in a container. 
Kubernetes uses probes to know when a container will be restarted, ready, and started.

#### Liveness Probe
Checks whether the application running inside the container is still alive and functioning properly. 
If the liveness probe fails, Kubernetes considers the container to be in a failed state and restarts it. 

#### Readiness Probe
Determines whether the application inside the container is ready to accept traffic or requests. 
When a readiness probe fails, Kubernetes stops sending traffic to the container until it passes the probe. 
This is useful during application startup or when the application needs some time to initialize before serving traffic.

Health probes must be enabled by the app. See
```
management.health.probes.enabled=true
```
In https://github.com/charroux/servicemesh/blob/main/carservice/src/main/resources/application.properties

Actuator must be added as library. See
```
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```
In https://github.com/charroux/servicemesh/blob/main/carservice/build.gradle

Liveness and Readiness must be configured by kubernetes. See
```
        livenessProbe:
          initialDelaySeconds: 180
          httpGet:
            path: /actuator/health
            port: 8080
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
```
In https://github.com/charroux/servicemesh/blob/main/microservices.yaml

On the services are launched, they display the probes at (throw the gateway): http://localhost:31380/carservice/actuator/health

## Continuous Integration with GitHub Actions

GitHub Actions allows to automate, customize, and execute development workflows right in a repository.

### The CI workflow

<img src="images/workflow.png">

https://github.com/charroux/servicemesh/blob/main/.github/workflows/actions.yml

Kubernetes configuration to test a service in isolation: https://github.com/charroux/servicemesh/blob/main/deploymentaction.yaml

### Build and tests

In memory database for unit testing in order to test a service in isolation: https://github.com/charroux/servicemesh/blob/main/carservice/src/test/resources/application.properties

Entities test: https://github.com/charroux/servicemesh/blob/main/carservice/src/test/java/com/charroux/carservice/entity/CarRepositoryTests.java

Use of mocks to test service in isolation: https://github.com/charroux/servicemesh/blob/main/carservice/src/test/java/com/charroux/carservice/service/RentalServiceTests.java

Microservice tests: https://github.com/charroux/servicemesh/blob/main/carservice/src/test/java/com/charroux/carservice/web/CarRentalRestServiceTests.java

### Générer le code protobuf / gRPC

Le projet utilise protobuf pour définir les contracts gRPC. Le plugin Gradle génère les sources Java à partir des fichiers `.proto`.

#### Commandes de régénération après modification d'un fichier .proto

Lorsque vous modifiez un fichier `.proto` (comme `/auctionService/src/main/proto/auctionService.proto`), vous devez régénérer les classes Java correspondantes :

**1. Régénération pour un module spécifique :**
```bash
# Pour le module auctionService (client)
./gradlew :auctionService:clean :auctionService:generateProto :auctionService:compileJava

# Pour le module auctionServiceServer (serveur)
./gradlew :auctionServiceServer:clean :auctionServiceServer:compileJava
```

**2. Régénération complète (recommandée) :**
```bash
# Nettoyer, régénérer et compiler tous les modules
./gradlew clean build
```

**3. Régénération depuis le dossier racine multi-module :**
```bash
# Si vous êtes dans le dossier racine du projet
./gradlew auctionService:clean auctionService:build
./gradlew auctionServiceServer:clean auctionServiceServer:build
./gradlew carRental:clean carRental:build
```

#### Autres commandes utiles

- Générer les sources protobuf pour le module `auctionService` :
```bash
./gradlew :auctionService:generateProto
```

- Générer puis compiler le module serveur (génère les sources et compile) :
```bash
./gradlew :auctionService:generateProto :auctionServiceServer:compileJava
```

#### Où trouver les fichiers générés

- Par défaut dans ce projet les fichiers générés sont placés sous `auctionService/src/generated` (configuré dans `auctionService/build.gradle`).
- Les classes générées incluent les messages (ex: `CarModel.java`), les services gRPC (`AuctionServiceGrpc.java`), etc.

#### Conseils IDE / CI

- Après génération, rafraîchissez le projet Gradle dans votre IDE (IntelliJ/VS Code) pour que le répertoire `src/generated` soit reconnu comme source Java.
- Le plugin télécharge automatiquement `protoc` et le plugin gRPC si nécessaire ; pas besoin d'installer `protoc` manuellement pour la compilation Gradle.
- **Important :** Si vous modifiez les fichiers `.proto`, regénérez les sources puis reconstruisez TOUS les modules consommateurs (ex. `auctionServiceServer`, `carRental`) pour éviter les erreurs de compilation.

#### Workflow typique après modification d'un .proto

1. Modifiez le fichier `.proto` (ex: renommer `Car` en `CarModel`)
2. Mettez à jour le code Java qui utilise les anciennes classes générées
3. Régénérez avec `./gradlew clean build`
4. Vérifiez que tous les tests passent


### Launch a workflow when the code is updated
Check an existing workflow: https://github.com/charroux/servicemesh/actions

Create a new branch: 
```
git branch newcarservice
```
Move to the new branch:
```
git checkout newcarservice
```
Update the code and commit changes:
```
git commit -a -m "newcarservice"
```
Push the changes to GitHub:
```
git push -u origin newcarservice
```
Create a Pull request on GitHub and follow the workflow.

Delete the branch:
```
git checkout main
```
```
git branch -D newcarservice
```
```
git push origin --delete newcarservice
```

## Service mesh


### Microservices, service mesh proxies and routing via the gateway

<img src="images/servicemesh.png">

```
kubectl apply -f infrastructure.yaml
```
https://github.com/charroux/servicemesh/blob/main/infrastructure.yaml

```
kubectl apply -f microservices.yaml
```
https://github.com/charroux/servicemesh/blob/main/microservices.yaml
#### Get the access to the Ingress gateway
```
./ingress-forward.sh
```
Ask carservice the list of cars:
```
http://localhost:31380/carservice/cars
```
### Circuit breaker
<img src="images/circuit_breaker.png">

Adding a circuit breaker to carservice:
```
kubectl apply -f circuit-breaker.yaml
```
Test the circuit breaker:
http://localhost:31380/carservice/cars

Disable the circuit breaker using:
```
kubectl delete -f circuit-breaker.yaml
```
### Monotoring
#### Display the Kiali dashboard
Kiali is a console for Istio service mesh.
```
kubectl -n istio-system port-forward deployment/kiali 20001:20001
```
Launch the console: http://localhost:20001/

Active again carservice:

http://localhost:31380/carservice/cars

Then inspect the cluster in Kiali.

<img src="images/kiali1.png">

#### Monitoring with Graphana
```
kubectl -n istio-system port-forward deployment/grafana 3000:3000
```
http://localhost:3000/

<img src="images/graphana_resources.png">

<img src="images/graphana_istio_dashboard.png">

<img src="images/graphana-envoy.png">

#### Monitoring with Jaeger
```
kubectl port-forward -n istio-system $(kubectl get pod -n istio-system -l app=jaeger -o jsonpath='{.items[0].metadata.name}') 16686:16686
```
Launch the console: http://localhost:16686/

## Composition of services via GraphQL

<img src="images/composition.png">

https://github.com/charroux/servicemesh/blob/main/rentalservice/src/main/resources/graphql/rentalAgreement.graphqls

https://github.com/charroux/servicemesh/blob/main/rentalservice/src/main/resources/graphql/customer.graphqls

https://github.com/charroux/servicemesh/blob/main/rentalservice/src/main/resources/graphql/car.graphqls

Server side coding:
https://github.com/charroux/servicemesh/blob/main/rentalservice/src/main/java/com/charroux/rentalservice/agreements/RentalController.java

## Delete resources and stop the cluster
```
kubectl delete -f infrastructure.yaml
```
```
kubectl delete -f microservices.yaml
```
```
minikube stop
```

# Requirements
## Install Docker and Minikube
https://www.docker.com/get-started/

https://minikube.sigs.k8s.io/docs/start/

Then start the Kubernetes cluster:
```
minikube start --cpus=2 --memory=5000 --driver=docker
```
## Install Istio
https://istio.io/latest/docs/setup/getting-started/
```
cd istio-1.17.0    
export PATH=$PWD/bin:$PATH    
istioctl install --set profile=demo -y
cd ..   
```
Enable auto-injection of the Istio side-cars when the pods are started:
```
kubectl label namespace default istio-injection=enabled
```
Install the Istio addons (Kiali, Prometheus, Jaeger, Grafana):
```
kubectl apply -f samples/addons
```
## 
Enable auto-injection of the Istio side-cars when the pods are started:
```
kubectl label namespace default istio-injection=enabled
```

Configure Docker so that it uses the Kubernetes cluster:
```
minikube docker-env
eval $(minikube -p minikube docker-env)
eval $(minikube docker-env)  
```

## Build the Docker images (optional)

Docker images for the services can be built locally. New Dockerfiles have been added to the repository:

- `carRental/Dockerfile` — multi-stage builder (Gradle + JDK 21) and runtime (Temurin JRE).
- `agreementService/Dockerfile` — builds the agreement service artifacts and packages them.
- `agreementServiceServer/Dockerfile` — builds the gRPC server service.
- `Dockerfile.multi` — multi-target Dockerfile at repository root with targets `carRental` and `postgres`.

You can also find some images published to Docker Hub under the `charroux` namespace; if you prefer to use those, update the manifests in `k8s/` accordingly.

Examples — build and push to your registry (replace `charroux` with your registry/namespace):

Build the car-rental image (single-service Dockerfile):
```bash
docker build -f carRental/Dockerfile -t charroux/car-rental:latest .
docker push charroux/car-rental:latest
```

Build the agreement-service image:
```bash
docker build -f agreementService/Dockerfile -t charroux/agreement-service:latest .
docker push charroux/agreement-service:latest
```

Build the agreement-service-server image:
```bash
docker build -f agreementServiceServer/Dockerfile -t charroux/agreement-service-server:latest .
docker push charroux/agreement-service-server:latest
```

Build the car-rental image using the multi-target root Dockerfile (alternate):
```bash
# build only the carRental target from Dockerfile.multi
docker build -f Dockerfile.multi --target carRental -t charroux/car-rental:latest .
docker push charroux/car-rental:latest
```

Build a custom Postgres image (optional, includes init scripts if present):
```bash
docker build -f Dockerfile.multi --target postgres -t charroux/postgres:15 .
docker push charroux/postgres:15
```

Quick dev using Docker Compose
---------------------------------
A ready-to-use Docker Compose file for development has been added: `docker-compose.dev.yml` and an environment file `.env.dev`.

Basic workflow
1. Copy or edit the example environment file (optional):

```bash
cp .env.dev .env
# edit .env to change POSTGRES_USER/POSTGRES_PASSWORD/POSTGRES_DB if needed
```

2. Start the stack (build images and run all services):

```bash
docker compose -f docker-compose.dev.yml --env-file .env.dev up
```

This command:
- Uses development environment variables from `.env.dev`
- Starts PostgreSQL database
- Starts car-rental service (REST API)
- Starts agreement-service (gRPC)

This will:
- Build and start the Postgres database
- Build and start the car-rental service on port 8080
- Build and start the agreement-service (gRPC) on port 9090

3. Run in detached mode:

```bash
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d
```
4. View logs:

```bash
docker compose -f docker-compose.dev.yml logs -f car-rental
```

5. Stop and remove containers:

```bash
docker compose -f docker-compose.dev.yml --env-file .env.dev down
```

Note: Add `-v` flag to also remove volumes (this will delete persistent data):
```bash
docker compose -f docker-compose.dev.yml --env-file .env.dev down -v
```

Rebuild specific services:

```bash
# Rebuild single service
docker compose -f docker-compose.dev.yml --env-file .env.dev build car-rental
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d

# Rebuild all services
docker compose -f docker-compose.dev.yml --env-file .env.dev build
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d
```

What this compose file does
- Starts a Postgres container (`postgres:15`) with a persistent volume `postgres-data`.
- Builds the `car-rental` image from `carRental/Dockerfile` and runs it with `SPRING_PROFILES_ACTIVE=prod` so the app will read Postgres connection settings from the `SPRING_DATASOURCE_*` env vars provided by compose.
- Builds and starts the `agreement-service` gRPC server from `agreementServiceServer/Dockerfile`.
- Exposes the following ports on the host:
  - `8080`: car-rental REST API
  - `9090`: agreement-service gRPC endpoint

Healthchecks and caveats
- The `docker-compose.dev.yml` includes healthchecks for Postgres and the `car-rental` service. The car-rental healthcheck calls the actuator `/actuator/health` endpoint. If your runtime image does not include `curl`, the healthcheck may fail — in that case either:
    - install `curl` in the runtime image, or
    - change the healthcheck to use a simple TCP check (nc) or remove it for local development, or
    - run the app with a mounted development image that includes curl.

Environment overrides
- You can override any Spring property by passing environment variables in the compose file (already wired for the datasource). For production, prefer using a secret manager or Kubernetes Secrets instead of plain env files.

Docker Compose is suitable for quick local development. For CI and production use Kubernetes manifests in `k8s/`.

Kubernetes
-----------
After building and pushing images, update `k8s/` manifests if you used different image names/tags and apply them:
```bash
kubectl apply -k k8s/
```

Notes
- The Dockerfiles are multi-stage and run the Gradle build inside the builder image. For CI you may prefer building the jar artifact in the CI runner and then creating a smaller runtime image from it.
- The builder images (e.g. `gradle:8.6-jdk21`) may trigger vulnerability warnings in scanners; it's recommended to scan images in your CI and choose minimal runtime images for production (distroless, slim, or jlink-based images).
- Replace any example credentials or base64 secrets before deploying to production.


# cloud-native-app
