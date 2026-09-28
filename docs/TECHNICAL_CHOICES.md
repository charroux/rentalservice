# Guide Technique: Event-Driven Architecture avec Redis

## 🎯 Résumé Exécutif

Ce guide documente les choix architecturaux et techniques pour implémenter une architecture event-driven avec Redis dans une application Spring Boot. Il capture les décisions prises, les alternatives considérées, et les pièges évités pendant l'implémentation.

**Public**: Développeurs et agents IA intégrant Redis événementiel dans des projets Java/Spring.

---

## 1. Stack Technique

### Versions Principales

| Composant | Version | Raison |
|-----------|---------|--------|
| **Java** | 21 LTS | Dernière LTS stable, features modernes (virtual threads), support 5+ ans |
| **Spring Boot** | 3.2.0 | Dernière 3.x stable, support 5+ ans, break from 2.x manageable |
| **Spring Data Redis** | 3.2.0 | Inclus dans Spring Boot 3.2, RedisTemplate + Reactive support |
| **Gradle** | 8.10.1 | Dernière stable, performance optimale, cache global setup |
| **Redis** | 7-alpine | Alpine pour Docker: 50% moins lourd, sécurité renforcée, produits essentiels |
| **Jackson** | 2.15+ | Fourni par Spring Boot, serialization robuste |

### Ecosystem Python

| Composant | Version | Usage |
|-----------|---------|-------|
| **Python 3** | 3.9+ | Pour scripts d'analyse (optionnel) |
| **Redis-CLI** | Latest | Testing et debugging local |

---

## 2. Librairies Redis/Queue

### ✅ Choix Final: RedisTemplate Direct

```gradle
// Dependance uniquement necessaire
implementation 'org.springframework.boot:spring-boot-starter-data-redis:3.2.0'
implementation 'io.lettuce:lettuce-core'  // Driver Redis (auto-inclus)
```

**Raison du choix:**
- ✅ Zéro config, part of Spring ecosystem
- ✅ Support synchrone et asynchrone (Mono/Flux)
- ✅ Connection pooling automatique (Lettuce)
- ✅ Testable avec Mockito
- ✅ Facile migrer vers Redis Streams ou MQ plus tard

### ❌ Alternatives Considérées (ET REJETÉES)

#### 1. Spring Cloud Stream + Redis Binder

```gradle
// REJETÉ - Ne compile pas!
implementation 'org.springframework.cloud:spring-cloud-stream-binder-redis:4.0.1'
```

**Problème**: Dépendance n'existe pas dans Maven Central! ⚠️
- Spring Cloud Stream supporte Kafka/RabbitMQ/Azure Service Bus
- Support Redis supprimé après Spring Cloud 2020.x
- **Leçon**: Vérifier disponibilité Maven Central AVANT conception

**Éviter si**:
- Pas de dépendance Maven Central
- Projet avec lock-in Risk (abandonment)
- Simple queue suffisante

#### 2. Redis Streams API

```java
// Alternative future, pas pour MVP
StreamOperations<String, String, String> streamOps = 
    redisTemplate.opsForStream();
```

**Considéré mais NOT pour prototype:**
- ✅ Meilleure que Lists (consumer groups, delivery guarantee)
- ✅ Produits modernes (Kafka-like)
- ❌ Plus complexe (consumer groups, ACK management)
- ❌ Overkill pour MVP
- ✅ Upgrade path: Lists → Streams (pas breaking)

**Quand utiliser**:
- Garantie delivery requise
- Multiple consumers par event type
- Replay d'events historiques

#### 3. Kafka

**Rejeté pour prototype Docker Compose:**
- ✅ Production-grade, scalable
- ❌ Complexité (Zookeeper/brokers)
- ❌ Lourd pour Docker Compose local
- ✅ Upgrade path futur: Redis → Kafka

---

## 3. Sérialisation JSON: Jackson

### Configuration Requise

#### ❌ PIÈGE: LocalDateTime sans module

```java
// ERREUR - Ceci échoue!
ObjectMapper mapper = new ObjectMapper();
mapper.writeValueAsString(event);  // LocalDateTime not supported!

// java.time.LocalDateTime not supported by default: 
// add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"
```

#### ✅ SOLUTION: Enregistrer JavaTimeModule

```gradle
// Dans build.gradle
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
```

```java
// Dans configuration Spring Boot (auto-détecté)
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Spring Boot fait déjà ceci si jsr310 est dans classpath
        return mapper;
    }
}
```

**Pourquoi c'est obligatoire:**
- Jackson 2.15+ n'inclut pas Java 8 date/time par défaut
- Tous les projets modernes utilisent LocalDateTime (pas java.util.Date!)
- Besoin dans PRODUCTION et TESTS

### Alternative: Oman Instant.toEpochMilli()

```java
// Évite le problème Jackson, mais perd timezone
private Long timestamp = Instant.now().toEpochMilli();  // ✅ Works
private LocalDateTime timestamp;                         // ❌ Needs module
```

**Trade-off:**
- Instant: Simple sérialisation, perte timezone
- LocalDateTime: Meilleure UX, nécessite module

**Choix**: LocalDateTime + JavaTimeModule (standard industrie)

---

## 4. Architecture Message

### Format: Metadata Wrapper Pattern

```json
{
  "eventType": "AuctionWon",
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1701234567890,
  "data": {
    "rentalId": "HERTZ-123",
    "carId": 42,
    "finalPrice": 850,
    ...
  }
}
```

**Avantages:**
- ✅ Route-able par eventType sans désérialisation
- ✅ Traçabilité via eventId (UUID)
- ✅ Timestamp server centralisé
- ✅ Data flexible (any schema in data blob)

**Alternative: Domain-Driven Event**

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "rentalId": "HERTZ-123",
  "carId": 42,
  "finalPrice": 850,
  ...
}
```

**Différence:**
- Plus simple, mais plus dur à filtrer/router
- Nécessite désérialisation complète
- Mieux pour: événements homogènes

**Choix**: Metadata wrapper (télématique moderne, support futur multi-types)

---

## 5. Structure de Queue Redis

### ✅ Choix: Redis Lists (FIFO Queue)

```bash
# Implementation
RPUSH auction:events:queue "event-json"     # Enqueue
LPOP auction:events:queue                    # Dequeue (destructive)
LRANGE auction:events:queue 0 -1             # Inspect (non-destructive)
LLEN auction:events:queue                    # Queue size

# Expiration
EXPIRE auction:events:queue 86400            # 24h TTL
```

**Avantages:**
- ✅ Atomique (ACID compatible)
- ✅ Performance: O(1) enqueue/dequeue
- ✅ Easy debug: redis-cli LLEN/LRANGE
- ✅ Facile migrer (pas de consumer state)

**Limitations:**
- ❌ Pas de consumer groups (multiple consumers = duplicate work)
- ❌ Pas de replay (consumer muss remember offset)
- ❌ Pas de ACK (delivery pas guaranteed)

### Alternatives par Use Case

| Use Case | Solution | Pros | Cons |
|----------|----------|------|------|
| **MVP, prototype** | Lists (FIFO) | Simple, testable | No replay, no groups |
| **Production, replay** | Streams | Consumer groups, replay | Plus complexe |
| **Enterprise, saga** | Kafka | Partitions, guarantee | Infrastructure heavy |
| **Real-time analytics** | Redis Streams | Fast, ordered | State management |

---

## 6. Configuration Spring Boot

### application-dev.yml: Développement

```yaml
spring:
  redis:
    host: localhost          # Local Redis
    port: 6379
    timeout: 2000ms          # Connection timeout
    # lettuce:
    #   pool:
    #     max-active: 8       # Connection pool
    #     max-idle: 8
    #     min-idle: 0
  
  jackson:
    default-property-inclusion: non_null  # Skip nulls
    # serialization:
    #   write-dates-as-timestamps: false  # Use ISO-8601
```

**Notes:**
- Pool settings: Defaults suffisant pour dev
- Timeout: 2s assez pour local Redis

### application-test.yml: Tests

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
  # H2 in-memory database
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
```

**Stratégie:**
- Redis externe (testcontainers alternative)
- H2 in-memory pour rapidité
- create-drop: Clean state per test

### application-prod.yml: Production (Future)

```yaml
spring:
  redis:
    host: ${REDIS_HOST:redis-prod}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD}
    ssl: true
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
      shutdown-timeout: 30000ms
```

**Production setup:**
- Variables d'env pour secrets
- SSL/TLS obligatoire
- Pool connections adaptées
- Timeout plus long (réseau distance)

---

## 7. Dependency Management: Gradle

### ✅ Approche: BOM + Version Alignment

```gradle
// Top-level build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

ext {
    set('springCloudVersion', "2023.0.0")
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}

dependencies {
    // Spring
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'io.lettuce:lettuce-core'
    
    // Jackson
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok:1.18.30'
    annotationProcessor 'org.projectlombok:lombok:1.18.30'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.mockito:mockito-junit-jupiter'
    testImplementation 'org.mockito:mockito-inline'
}
```

**Best Practices:**
- Version centralisée (pas version strings répétées)
- BOM pour Spring Cloud compatibility
- Lombok: compileOnly + annotationProcessor (pas runtime)

### ❌ À ÉVITER

```gradle
// Mauvais: Version loose
implementation 'org.springframework.boot:spring-boot-starter-data-redis'

// Mauvais: Conflicting versions
implementation 'org.springframework.boot:spring-boot-starter-web:3.2.0'
implementation 'org.springframework.data:spring-data-redis:2.7.0'

// Mauvais: Unused dependency
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-joda'
```

---

## 8. Testing: Mockito + JUnit 5

### ❌ PIÈGE: ObjectMapper non configuré

```java
@ExtendWith(MockitoExtension.class)
class AuctionEventPublisherTest {
    @BeforeEach
    void setUp() {
        // ERREUR - Jackson n'a pas le module!
        objectMapper = new ObjectMapper();  
        
        // ✅ CORRECT
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
}
```

**Impact:**
- Tests passent en isolation, échouent en CI
- Difficile debugger (error masqué dans publisher)
- Solution: Toujours initialiser ObjectMapper identique prod+test

### Pattern: Mock RedisTemplate

```java
@ExtendWith(MockitoExtension.class)
class AuctionEventPublisherTest {
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ListOperations<String, String> listOperations;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        publisher = new AuctionEventPublisher(redisTemplate, objectMapper);
        
        // Mock the chain: redisTemplate.opsForList()
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }
    
    @Test
    void testPublishSuccess() {
        // Setup: Mock rightPush return value (queue position)
        when(listOperations.rightPush(eq("auction:events:queue"), anyString()))
            .thenReturn(1L);
        when(redisTemplate.expire(eq("auction:events:queue"), eq(24L), eq(TimeUnit.HOURS)))
            .thenReturn(true);
        
        // Act
        AuctionWonEvent event = AuctionWonEvent.create(...);
        boolean result = publisher.publishAuctionWon(event);
        
        // Assert
        assertTrue(result);
        verify(listOperations).rightPush(eq("auction:events:queue"), anyString());
        verify(redisTemplate).expire(eq("auction:events:queue"), eq(24L), eq(TimeUnit.HOURS));
    }
}
```

**Points clés:**
- rightPush retourne Long (queue position), pas boolean
- Vérifier result != null && result > 0
- Matcher explicite: eq(TimeUnit.HOURS) NOT any() (sinon verification échoue)

### ❌ Pièges Mockito

```java
// ERREUR 1: Matcher mix (generic + explicit)
when(redisTemplate.expire("auction:events:queue", 24L, any()))
    .thenReturn(true);
// verify() later avec eq() fails → UnnecessaryStubbingException

// ERREUR 2: Oublier mock du chain
when(redisTemplate.opsForList()).thenReturn(listOperations);  // OBLIGATOIRE
// Sans cela: NullPointerException

// ERREUR 3: Verify trop strict
verify(listOperations, times(1)).rightPush(...);  // Too rigid
// Mieux: verify(...) ou times(3) pour boucles
```

---

## 9. Docker Compose: Redis Stack

### ✅ Configuration Optimale

```yaml
services:
  redis:
    image: redis:7-alpine
    container_name: car-rental-redis
    ports:
      - "6379:6379"
    command: >
      redis-server 
      --appendonly yes
      --maxmemory 512mb
      --maxmemory-policy allkeys-lru
      --loglevel warning
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5
      start_period: 10s
    volumes:
      - redis-data:/data
    networks:
      - car-rental-network
    restart: unless-stopped

  car-rental:
    build:
      context: .
      dockerfile: carRental/Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/carrent
      SPRING_DATASOURCE_USERNAME: carrent
      SPRING_DATASOURCE_PASSWORD: carrent123
    depends_on:
      redis:
        condition: service_healthy
      postgres:
        condition: service_healthy
    networks:
      - car-rental-network

volumes:
  redis-data:
    driver: local

networks:
  car-rental-network:
    driver: bridge
```

**Choix:**
- **7-alpine**: 80% moins lourd que debian (30MB vs 150MB)
- **appendonly yes**: AOF persistence (crash-safe)
- **maxmemory 512mb**: Limiter usage local
- **allkeys-lru**: Eviction policy (vieux events d'abord)
- **healthcheck**: Kubernetes-ready probe
- **depends_on condition**: Wait for Redis healthy

### ❌ À ÉVITER

```yaml
# Mauvais: Pas de healthcheck
redis:
  image: redis:7
  # Service "up" avant Redis prêt!

# Mauvais: Pas de volume
redis:
  image: redis:7
  # Data perdu au redémarrage

# Mauvais: Limiter mémoire
command: redis-server --maxmemory 100mb
# OOM kill en production

# Mauvais: Pas réseau explicite
# Default bridge ok, mais pas portable
```

---

## 10. Logging & Debugging

### Spring Boot Logging

```properties
# application.properties
logging.level.com.charroux.carRental.events=DEBUG
logging.level.org.springframework.data.redis=DEBUG
logging.level.io.lettuce.core=WARN

logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

### Dans le Code

```java
@Component
public class AuctionEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(AuctionEventPublisher.class);
    
    public boolean publishAuctionWon(AuctionWonEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String messageWithMetadata = String.format(
                "{\"eventType\":\"AuctionWon\",\"eventId\":\"%s\",\"timestamp\":%d,\"data\":%s}",
                event.getEventId(), event.getTimestamp(), eventJson);
            
            Long result = redisTemplate.opsForList()
                .rightPush("auction:events:queue", messageWithMetadata);
            
            if (result != null && result > 0) {
                redisTemplate.expire("auction:events:queue", 24L, TimeUnit.HOURS);
                logger.info("✅ Event published: eventId={}, queuePos={}", 
                    event.getEventId(), result);
                return true;
            }
            
            logger.warn("⚠️ Event not queued: eventId={}, result={}", 
                event.getEventId(), result);
            return false;
            
        } catch (JsonProcessingException e) {
            logger.error("❌ JSON serialization error: eventId={}, error={}", 
                event.getEventId(), e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("❌ Redis publish error: eventId={}, error={}", 
                event.getEventId(), e.getMessage(), e);
            return false;
        }
    }
}
```

### Redis CLI Debugging

```bash
# Connexion
redis-cli -h localhost -p 6379

# Inspection queue
LLEN auction:events:queue                    # Longueur
LRANGE auction:events:queue 0 -1             # Tous events (destructive!)
LRANGE auction:events:queue 0 9              # First 10 (non-destructive)
LINDEX auction:events:queue 0                # Premier event

# Jq pour pretty-print
redis-cli LINDEX auction:events:queue 0 | jq .

# Monitoring real-time
MONITOR                # Toutes commandes

# Stats
INFO server
INFO memory
INFO stats
```

---

## 11. Pièges Rencontrés & Solutions

| Piège | Symptôme | Solution | Prévention |
|-------|---------|----------|-----------|
| **Jackson LocalDateTime** | `InvalidDefinitionException` in tests | Ajouter `jackson-datatype-jsr310` | Vérifier Maven Central availability |
| **Date crossing months** | `testAuctionWonEventRentalDates` échoue | Utiliser `ChronoUnit.DAYS.between()` | Tester cas limites (28-31 jours) |
| **Mock verification strict** | `UnnecessaryStubbingException` | Utiliser `eq()` explicite pour enums | Matcher types: `eq(TimeUnit.HOURS)` |
| **RedisTemplate.opsForList()** | `NullPointerException` en test | Mocker le chain: `when(redisTemplate.opsForList()).thenReturn(...)` | Setup @BeforeEach complet |
| **rightPush return type** | `assertFalse()` dépasse attendu | rightPush retourne Long, check `> 0` | Vérifier JavaDoc RedisTemplate |
| **Spring Cloud Stream unavailable** | `Could not find dependency` | Utiliser RedisTemplate direct | Vérifier Maven Central AVANT design |
| **Redis TTL non appliqué** | Vieux events restent | Appeler `expire()` après `rightPush()` | Tester TTL: `TTL auction:events:queue` |
| **Docker Redis unreachable** | App timeout connexion | Configurer hostname: `SPRING_REDIS_HOST: redis` | Vérifier docker-compose networks |

---

## 12. Checklist Implémentation

### Phase 1: Design Event

- [ ] Définir structure event (Domain fields + metadata)
- [ ] Déterminer queue strategy (Lists vs Streams vs Kafka)
- [ ] Planifier TTL et retention
- [ ] Documenter JSON schema

### Phase 2: Dépendances

- [ ] Ajouter `spring-boot-starter-data-redis`
- [ ] Ajouter `jackson-datatype-jsr310`
- [ ] Vérifier versions Maven Central
- [ ] Tester build local: `./gradlew build`

### Phase 3: Implementation

- [ ] Créer Domain Event class (Lombok @Data)
- [ ] Implémenter Publisher service (RedisTemplate)
- [ ] Intégrer dans endpoint existant (non-blocking)
- [ ] Configuration Spring (application-*.yml)

### Phase 4: Testing

- [ ] Unit tests Event class
- [ ] Unit tests Publisher (Mockito)
- [ ] Configuration tests (spring-test)
- [ ] Tous tests passent: `./gradlew test`

### Phase 5: Local Validation

- [ ] Docker Compose stack up
- [ ] App startup logs clean
- [ ] REST endpoint functional
- [ ] Events visible en Redis

### Phase 6: CI/CD

- [ ] GitHub Actions: `./gradlew build` (includes tests)
- [ ] Trivy scan Docker images
- [ ] Build artifacts production-ready

### Phase 7: Documentation

- [ ] Architecture diagram
- [ ] API schema (Event JSON)
- [ ] Testing procedures
- [ ] Troubleshooting guide
- [ ] Consumer example code

---

## 13. Upgrade Path: Lists → Streams → Kafka

```
Phase 1 (MVP)       Phase 2 (Production)    Phase 3 (Scale)
┌─────────────┐     ┌─────────────────┐    ┌─────────────┐
│ Redis Lists │     │ Redis Streams   │    │ Kafka       │
│ - Simple    │ ──→ │ - Consumer Grp  │ ──→│ - Partition │
│ - FIFO      │     │ - Replay        │    │ - Topics    │
│ - 24h TTL   │     │ - Guarantee     │    │ - Cluster   │
└─────────────┘     └─────────────────┘    └─────────────┘

Code impact:        Code change:           Significant
- Minimal          - Consumer setup       - New patterns
- Testing stays    - Group management     - Partitioning
- Same queue name  - Same queue name      - Offset tracking
```

**Migration Strategy:**
1. Consumer reads from both (Lists + Streams)
2. Gradual switch 10% → 50% → 100%
3. Keep Lists as backup 1 month
4. Remove Lists code

---

## 14. Production Readiness Checklist

- [ ] **Deployment**: Docker Compose → Kubernetes (StatefulSet Redis)
- [ ] **Monitoring**: Prometheus metrics RedisTemplate
- [ ] **Alerting**: Queue length thresholds
- [ ] **Backup**: Redis AOF + RDB dumps
- [ ] **Security**: Redis AUTH, SSL/TLS, Network policies
- [ ] **Performance**: Load test avec 1000+ events/sec
- [ ] **Resilience**: Retry logic, circuit breaker
- [ ] **Observability**: Tracing (Sleuth), correlationId
- [ ] **Documentation**: Runbook, incidents playbook
- [ ] **Training**: Team onboarding session

---

## Résumé: Les 5 Décisions Critiques

### 1️⃣ RedisTemplate Direct (vs Spring Cloud Stream)

```
❌ Spring Cloud Stream: Pas disponible Maven Central
✅ RedisTemplate: Core Spring Data, production-proven
```

**Impact**: Réduit dépendances, améliore stabilité.

### 2️⃣ Jackson JavaTimeModule Obligatoire

```
❌ ObjectMapper par défaut: LocalDateTime non supporté
✅ Ajouter jackson-datatype-jsr310: Fonctionne prod+test
```

**Impact**: Élimine serialization errors, même en CI.

### 3️⃣ Redis Lists pour MVP

```
❌ Streams: Complexe pour prototype
✅ Lists: Simple, testable, upgradeable
```

**Impact**: MVP rapide, upgrade path transparent.

### 4️⃣ Non-Blocking Publisher

```
❌ Event publish = transaction main: Risque faillite
✅ Fire-and-forget: Auction réussit même si event échoue
```

**Impact**: Résilience, graceful degradation.

### 5️⃣ Metadata Wrapper Pattern

```
❌ Event seul: Difficile router/filtrer
✅ {eventType, eventId, timestamp, data}: Télématique moderne
```

**Impact**: Extensibilité future, observabilité.

---

## Ressources

### Documentation
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Redis Data Types](https://redis.io/docs/data-types/)
- [Jackson Date/Time](https://github.com/FasterXML/jackson-modules-java8)
- [Mockito Matchers](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)

### Outils
- redis-cli: `brew install redis`
- Docker Desktop: https://www.docker.com/products/docker-desktop
- JetBrains DataGrip: IDE Redis client (payant)
- redis-ui: Browser-based Redis manager (OSS)

### Patterns
- Event Sourcing: https://martinfowler.com/eaaDev/EventSourcing.html
- Consumer Pattern: https://redis.io/docs/manual/client-side-caching/
- Circuit Breaker: https://resilience4j.readme.io/

---

**Document Version**: 1.0
**Date**: December 2024
**Auteur**: Architecture Team
**Status**: ✅ Frozen (utilisé pour production)

Consultez ce document avant implémenter event-driven Redis dans tout nouveau projet.
