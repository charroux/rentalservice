# Service d'assurance complémentaire

`insuranceService` est un microservice pédagogique qui réagit à une enchère
gagnée et crée une proposition d'assurance complémentaire. Il valide un flux
événementiel de bout en bout sans Redis Streams.

## Parcours

```text
Enchère gagnée
    |
    +--> file Redis du service de location
    |
    +--> file Redis du service d'assurance
              |
              +--> proposition PROPOSED en PostgreSQL
                          |
Angular <-- API REST -----+--> acceptation --> ACCEPTED
```

Le service de location publie l'enveloppe `AuctionWonEvent` dans une file
d'entrée stable. Le routeur d'événements la duplique dans les files déclarées
par les descripteurs de souscription. Les copies conservent le même `eventId`.
Le consommateur d'assurance déplace atomiquement son message de la
liste `ready` vers la liste `processing`, puis l'acquitte avec `LREM` après la
transaction en base. Un message non traité reste donc récupérable.

## Règle volontairement simple

La prime journalière vaut 10 % du prix final de location, arrondie à l'euro
supérieur, avec un minimum de 5 €. Une proposition commence à l'état
`PROPOSED` et peut passer à `ACCEPTED`.

## API

```http
GET  /insurance/offers/{rentalId}
POST /insurance/offers/{rentalId}/accept
```

En développement, le service écoute sur `http://localhost:8081`. Dans l'image
Angular, Nginx expose la même API sous `/insurance-api/`. Le composant de
validation interroge brièvement l'API après l'enchère afin d'absorber le délai
normal de cohérence éventuelle, puis affiche le tarif et le bouton
d'acceptation.

## Idempotence et stockage

La colonne `event_id` est unique : une nouvelle livraison du même événement ne
crée pas une seconde offre. Pour limiter l'infrastructure de démonstration,
les services partagent actuellement une instance et une base PostgreSQL, mais
le service d'assurance ne manipule que sa table `insurance_offers`. Cette
simplification pourra être remplacée par une base logique dédiée sans changer
le contrat événementiel.

## Exécution

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up --build
```

Les tests unitaires du consommateur couvrent la création, l'idempotence et la
conservation d'un événement invalide dans la file de reprise.
