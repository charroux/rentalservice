# Architecture événementielle simple avec Redis Lists

Cette phase introduit les principes événementiels sans Redis Streams. Elle est
conservée comme implémentation pédagogique lorsque la future voie Streams/CQRS
sera ajoutée en parallèle.

## Flux actuel

```text
CarRentalRestService
        |
        | AuctionWonEvent (JSON, eventId stable)
        v
events:simple:published
        |
        | Event Router + subscription descriptors
        +--> rental-service:ready    --> rental-service:processing
        +--> insurance-service:ready --> insurance-service:processing
                                              |
                                              +--> proposition persistée
```

Le producteur ajoute les événements avec `RPUSH`. Le consommateur déplace le
plus ancien événement avec `LMOVE`, au lieu de le supprimer avec `LPOP`. Ainsi,
un arrêt entre la lecture et la validation en base ne détruit pas le message.

La contrainte unique `(event_id, consumer_name)` de `processed_events` rend une
nouvelle livraison sans effet : l'événement déjà traité est simplement
acquitté.

## Une file par service

Une Redis List distribue le travail entre ses consommateurs ; elle ne diffuse
pas une copie à chaque microservice. Chaque nouveau service doit donc disposer
de sa propre paire de listes :

```text
auction:events:simple:rental-service:ready
auction:events:simple:rental-service:processing

auction:events:simple:insurance-service:ready
auction:events:simple:insurance-service:processing
```

Le publisher écrit une seule fois dans la file d'entrée stable. Le routeur
Python charge les fichiers de `contracts/subscriptions` et publie la même
enveloppe, avec le même `eventId`, dans chaque file concernée. Ajouter un
consommateur ne modifie donc plus le producteur.

## Hypothèses de la phase pédagogique

- Un seul consommateur logique par file `processing`.
- Redis utilise AOF dans Docker Compose et Kubernetes.
- PostgreSQL reste la source de vérité pour l'idempotence.
- Un événement invalide est conservé dans `processing` et retenté. Une dead
  letter queue et une limite de tentatives pourront être ajoutées séparément.
- La future implémentation Streams utilisera d'autres clés et ne remplacera pas
  ces listes.

## Configuration

```properties
events.simple.poll-delay-ms=1000
events.simple.initial-delay-ms=1000
events.simple.retry-delay-ms=30000
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

Dans Docker Compose et Kubernetes, l'hôte Redis est injecté avec
`SPRING_DATA_REDIS_HOST=redis`.

## Vérification manuelle

```bash
redis-cli LLEN auction:events:simple:rental-service:ready
redis-cli LLEN auction:events:simple:rental-service:processing
redis-cli LRANGE auction:events:simple:rental-service:ready 0 -1
redis-cli LLEN auction:events:simple:insurance-service:ready
redis-cli LLEN auction:events:simple:insurance-service:processing
redis-cli LLEN events:simple:published
redis-cli LLEN events:simple:routing
```

Après un traitement réussi, les deux listes doivent être vides et une ligne
correspondante doit exister dans `processed_events`.
