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
              +--> proposition PROPOSED dans MongoDB (`insurance`)
                          |
Angular --commande------->+--> acceptation --> ACCEPTED
Angular <--vue consolidée-- rentalQueryService
```

Le service de location publie l'enveloppe `AuctionWonEvent` dans une file
d'entrée stable. Le routeur d'événements la duplique dans les files déclarées
par les descripteurs de souscription. Les copies conservent le même `eventId`.
Le consommateur d'assurance déplace atomiquement son message de la
liste `ready` vers la liste `processing`, puis l'acquitte avec `LREM` après la
transaction en base. Un message non traité reste donc récupérable.

La création ou l'acceptation d'une offre écrit, dans la même transaction
MongoDB, l'agrégat et un message dans `insurance_outbox`. Un relais asynchrone
publie ensuite l'événement vers Redis avec reprise exponentielle. La publication
est au moins une fois : les identifiants stables rendent les doublons sûrs.

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
validation interroge brièvement `rentalQueryService` après l'enchère afin
d'absorber le délai normal de cohérence éventuelle. La commande d'acceptation
reste adressée à `insuranceService`, propriétaire de l'agrégat.

## Idempotence et stockage

Le champ indexé `event_id` est unique : une nouvelle livraison du même événement
ne crée pas une seconde offre. `rental_id` est également unique pour matérialiser
la règle d'une offre courante par location. `insuranceService` possède la
collection `insurance.insurance_offers`; `rentalQueryService` possède séparément
`rental_query.rental_views`. Le premier document est un agrégat métier, le
second une projection dérivée et reconstructible seulement si ses événements
d'entrée ont été conservés. `rentalQueryService` les archive désormais dans
`rental_query.projection_event_archive` au sein de la même transaction que la
mise à jour de la vue.

Les messages invalides sont déplacés vers
`auction:events:simple:insurance-service:dead-letter` avec le payload et le
diagnostic. Les pannes transitoires restent dans `processing` pour être reprises.

## Exécution

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up --build
```

Les tests couvrent la création, l'acceptation idempotente, l'outbox, la mise en
DLQ d'un événement invalide, les doublons et l'arrivée défavorable des
événements dans la projection.
