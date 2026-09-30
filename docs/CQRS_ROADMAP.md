# CQRS Roadmap: Du Event-Driven Hybride au CQRS Complet

**Date**: Septembre 2026  
**Public**: Architects, Backend Leads, Full-Stack Engineers  
**Durée estimée**: 6-12 mois (3 phases progressives)  
**Objectif**: Scaler chaque use case indépendamment avec séparation Read/Write

---

## 🎯 Vision Finale: CQRS Complet

```
┌──────────────────────────────────────────────────────────────┐
│                     Angular Frontend                          │
│            (Single Page App - Reads Cachées)                 │
└────────┬──────────────────────────────────────┬──────────────┘
         │                                      │
    ┌────▼─────────┐                   ┌───────▼────────┐
    │ Command API  │                   │  Query API     │
    │ (Écritures)  │                   │  (Lectures)    │
    └────┬─────────┘                   └────────────────┘
         │                                      ▲
         │ SQL Write                            │ Redis Cache
         │                                      │
    ┌────▼────────────────────────┐   ┌────────┴──────────────┐
    │  PostgreSQL Write DB         │   │  PostgreSQL Read DB  │
    │  - rental (Command Model)    │   │  - mv_active_rentals │
    │  - car (Command Model)       │   │  - mv_auction_stats  │
    │  - auction (Command Model)   │   │  - mv_customer_prefs │
    └────┬────────────────────────┘   └─────────────────────────┘
         │
         │ Events (JSON)
         │
    ┌────▼──────────────┐
    │  Redis Streams    │
    │  - auction:events │
    │  - rental:events  │
    │  - car:events     │
    └────┬──────────────┘
         │
    ┌────▼────────────────────────────────────────┐
    │  Event Consumers (Services Indépendants)   │
    │  - RentalEventConsumer                     │
    │  - InsuranceEventConsumer                  │
    │  - FuelServiceEventConsumer                │
    │  - AnalyticsEventConsumer                  │
    └─────────────────────────────────────────────┘
```

---

## 📋 Phase 1: Solidifier Event-Driven Hybride (NOW → 6 semaines)

### Objectif
✅ Stabiliser la queue Redis  
✅ Ajouter Event Consumers  
✅ Garantir idempotence  
✅ Implémenter retry + DLQ  

### 1.1 Upgrade Redis: Streams API (vs Lists)

**Pourquoi?**
- Lists: FIFO simple, pas de consumer groups
- Streams: Consumer groups, acknowledgment, replay capability

**Before:**
```java
// Ancien pattern - pas de guarantees
redisTemplate.opsForList().rightPush("auction:events:queue", event);
```

**After:**
```java
// Nouveau pattern - production-grade
StreamOperations<String, String, String> streamOps = redisTemplate.opsForStream();
streamOps.add("auction:events", Map.of(
    "eventId", event.getEventId(),
    "rentalId", event.getRentalId(),
    "payload", objectMapper.writeValueAsString(event)
));
```

**Task Breakdown:**
- [ ] Créer `RedisStreamConfig.java`
- [ ] Migrer `AuctionEventPublisher` → StreamOperations
- [ ] Créer base `StreamEventConsumer` réutilisable
- [ ] Tester consumer groups + ACK

### 1.2 Ajouter Event Consumers

**Créer consumer indépendant par service:**

```java
// File: auctionService/src/main/java/com/charroux/auction/events/AuctionEventConsumer.java
@Component
public class AuctionEventConsumer {
    
    @Autowired
    private AuctionReadRepository auctionReadRepository;
    
    @PostConstruct
    public void startListening() {
        // Écoute le stream "auction:events"
        // Consumer group: "auction-service-cg"
        // Déduplique par eventId (idempotence)
    }
    
    // Consomme: auction.won, auction.lost, auction.extended
    public void handleAuctionWon(AuctionWonEvent event) {
        // Check idempotence: if event already processed → skip
        if (auditLog.exists(event.getEventId())) {
            logger.info("Event {} already processed", event.getEventId());
            return;
        }
        
        // Process: create/update read model for analytics
        auditLog.record(event.getEventId(), event.getTimestamp());
    }
}
```

**Tasks:**
- [ ] `RentalEventConsumer` (écoute auction.won → crée rental state)
- [ ] `InsuranceEventConsumer` (écoute auction.won → calcule prime assurance)
- [ ] `AnalyticsEventConsumer` (écoute tous → accumule stats)

### 1.3 Garantir Idempotence

**Pattern: Idempotent Key**

```java
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id
    private String eventId;  // UUID from event
    private LocalDateTime processedAt;
    private String consumerName;
}

// Dans le consumer:
if (processedEventRepository.existsById(event.getEventId())) {
    return; // Déjà traité
}
processedEventRepository.save(new ProcessedEvent(
    event.getEventId(),
    LocalDateTime.now(),
    "RentalEventConsumer"
));
// ... traiter l'événement
```

### 1.4 Ajouter Dead Letter Queue (DLQ)

```java
@Component
public class StreamEventConsumer {
    
    private static final String MAIN_STREAM = "auction:events";
    private static final String DLQ_STREAM = "auction:events:dlq";
    private static final int MAX_RETRIES = 3;
    
    public void processEvent(AuctionWonEvent event) {
        try {
            // ... business logic
        } catch (Exception ex) {
            if (event.getRetryCount() < MAX_RETRIES) {
                event.setRetryCount(event.getRetryCount() + 1);
                redisTemplate.opsForStream().add(MAIN_STREAM, event);
            } else {
                // Envoyer à DLQ pour investigation manuelle
                redisTemplate.opsForStream().add(DLQ_STREAM, event);
                logger.error("Event moved to DLQ after {} retries: {}", 
                    MAX_RETRIES, event.getEventId(), ex);
            }
        }
    }
}
```

### 1.5 Monitoring & Alertes

```yaml
# docker-compose.dev.yml
redis-commander:
  image: rediscommander/redis-commander:latest
  environment:
    REDIS_HOSTS: local:redis:6379
  ports:
    - "8081:8081"
  depends_on:
    - redis

# k8s/base/redis-exporter.yaml
apiVersion: v1
kind: Pod
metadata:
  name: redis-stream-monitor
spec:
  containers:
  - name: monitor
    image: redis:7-alpine
    command: ["redis-cli", "XINFO", "STREAM", "auction:events"]
    # Affiche: length (msgs), consumer-groups, first-entry, last-entry
```

**Tasks:**
- [ ] Configurer Redis Commander pour visualiser streams
- [ ] Créer endpoint `/actuator/redis-metrics` 
- [ ] Dashboard Grafana: event lag, consumer lag, DLQ count

---

## 📊 Phase 2: Vues Matérialisées + Read Model Séparé (Semaines 7-14)

### Objectif
✅ Créer READ DB (PostgreSQL Read Replica ou séparé)  
✅ Implémenter vues matérialisées  
✅ Projections CQRS de base  
✅ Cache Redis pour hot queries

### 2.1 Créer PostgreSQL Read Replica

**Option A: Read Replica (Recommandé pour production)**
```bash
# AWS RDS, Google Cloud SQL, Azure Database
# Configuration: automatic failover + async replication
# Lag: ~1-5ms typiquement
```

**Option B: Separate Read DB (Pour MVP local)**
```yaml
# docker-compose.dev.yml
postgres-read:
  image: postgres:15
  environment:
    POSTGRES_DB: carental_read
    POSTGRES_USER: reader
    POSTGRES_PASSWORD: reader_pass
  ports:
    - "5433:5432"
  
# Spring Boot datasource configuration:
# spring.datasource.url=jdbc:postgresql://postgres:5432/carental
# spring.datasource-read.url=jdbc:postgresql://postgres-read:5433/carental_read
```

### 2.2 Définir Vues Matérialisées

**Vue #1: Offres Disponibles (Lecture Frontend)**

```sql
-- File: database/migrations/V002__create_read_views.sql

-- Vue: Offres avec stats en temps réel (remplace /offers endpoint)
CREATE MATERIALIZED VIEW mv_available_offers AS
SELECT 
    cm.id,
    cm.brand,
    cm.model,
    cm.lowest_price,
    cm.highest_price,
    COUNT(c.id) as cars_available,
    AVG(c.final_customer_price)::INT as avg_rental_price,
    MAX(b.final_price)::INT as last_winning_bid,
    COUNT(b.id) as total_auctions_this_month
FROM car_model cm
LEFT JOIN car c ON cm.id = c.car_model_id 
    AND c.status = 'AVAILABLE'
LEFT JOIN bidding b ON c.id = b.car_id 
    AND b.status = 'WON'
    AND b.created_at > NOW() - INTERVAL '30 days'
GROUP BY cm.id, cm.brand, cm.model, cm.lowest_price, cm.highest_price
WITH DATA;

CREATE UNIQUE INDEX idx_mv_available_offers_id ON mv_available_offers(id);

-- Vue: Statistiques d'enchères par modèle
CREATE MATERIALIZED VIEW mv_auction_stats AS
SELECT 
    cm.brand || ' ' || cm.model as model_name,
    COUNT(b.id) as total_bids,
    AVG(b.final_price)::INT as avg_winning_bid,
    MIN(b.final_price)::INT as min_winning_bid,
    MAX(b.final_price)::INT as max_winning_bid,
    (MAX(b.final_price) - MIN(b.final_price))::INT as price_volatility,
    DATE(b.created_at) as bid_date
FROM bidding b
JOIN car c ON b.car_id = c.id
JOIN car_model cm ON c.car_model_id = cm.id
WHERE b.status = 'WON'
GROUP BY cm.id, cm.brand, cm.model, DATE(b.created_at)
WITH DATA;

CREATE INDEX idx_mv_auction_stats_date ON mv_auction_stats(bid_date DESC);

-- Vue: Préférences clients (pour recommandations)
CREATE MATERIALIZED VIEW mv_customer_preferences AS
SELECT 
    cust.id as customer_id,
    cm.brand,
    cm.model,
    COUNT(rc.id) as rental_count,
    AVG(EXTRACT(DAY FROM rc.end_date - rc.start_date))::INT as avg_rental_days,
    MAX(rc.created_at) as last_rental_date
FROM customer cust
LEFT JOIN rental_contract rc ON cust.id = rc.customer_id
LEFT JOIN car c ON rc.car_id = c.id
LEFT JOIN car_model cm ON c.car_model_id = cm.id
GROUP BY cust.id, cm.brand, cm.model
WITH DATA;

CREATE INDEX idx_mv_customer_prefs_customer ON mv_customer_preferences(customer_id);
```

### 2.3 Refresh Strategy

```java
// File: carRental/src/main/java/com/charroux/carRental/analytics/MaterializedViewRefresher.java

@Component
public class MaterializedViewRefresher {
    
    @Autowired
    @Qualifier("readDataSource")
    private DataSource readDataSource;
    
    // Option 1: Refresh immédiat après auction.won
    @EventListener
    public void onAuctionWon(AuctionWonEvent event) {
        refreshViewAsync("mv_available_offers");
        refreshViewAsync("mv_auction_stats");
    }
    
    // Option 2: Refresh programmé (moins cher en CPU)
    @Scheduled(cron = "0 */5 * * * *")  // Toutes les 5 minutes
    public void refreshViewsScheduled() {
        refreshViewConcurrently("mv_available_offers");
        refreshViewConcurrently("mv_auction_stats");
        refreshViewConcurrently("mv_customer_preferences");
    }
    
    private void refreshViewAsync(String viewName) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = readDataSource.getConnection()) {
                String sql = "REFRESH MATERIALIZED VIEW CONCURRENTLY " + viewName;
                conn.createStatement().execute(sql);
                logger.info("Refreshed view: {}", viewName);
            } catch (SQLException ex) {
                logger.error("Failed to refresh view: {}", viewName, ex);
            }
        });
    }
}
```

### 2.4 Projection CQRS: Query Service

```java
// File: carRental/src/main/java/com/charroux/carRental/query/OfferQueryService.java

@Service
public class OfferQueryService {
    
    @Autowired
    @Qualifier("readDataSource")
    private JdbcTemplate readDb;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // Lecture: /offers (via vues matérialisées)
    public List<OfferDTO> getAvailableOffers(String brand) {
        // 1. Check Redis cache first
        String cacheKey = "offers:" + (brand != null ? brand : "all");
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return objectMapper.readValue(cached, new TypeReference<>() {});
        }
        
        // 2. Query read replica (vues matérialisées)
        String sql = """
            SELECT id, brand, model, lowest_price, highest_price,
                   cars_available, avg_rental_price, last_winning_bid
            FROM mv_available_offers
            """ + (brand != null ? " WHERE brand = ?" : "");
        
        List<OfferDTO> offers = readDb.query(sql,
            brand != null ? new Object[]{brand} : new Object[]{},
            mapperForOfferDTO());
        
        // 3. Cache in Redis (TTL: 5 min)
        redisTemplate.opsForValue().set(
            cacheKey,
            objectMapper.writeValueAsString(offers),
            Duration.ofMinutes(5)
        );
        
        return offers;
    }
    
    // Lecture: Statistiques d'enchères
    public List<AuctionStatsDTO> getAuctionStats(LocalDate fromDate) {
        String sql = """
            SELECT model_name, total_bids, avg_winning_bid, 
                   min_winning_bid, max_winning_bid, price_volatility
            FROM mv_auction_stats
            WHERE bid_date >= ?
            ORDER BY total_bids DESC
            """;
        
        return readDb.query(sql, new Object[]{fromDate}, 
            mapperForAuctionStats());
    }
    
    // Lecture: Recommandations personnalisées
    public List<String> getRecommendedModels(String customerId) {
        String sql = """
            SELECT DISTINCT brand, model
            FROM mv_customer_preferences
            WHERE customer_id = ?
            ORDER BY rental_count DESC
            LIMIT 5
            """;
        
        return readDb.query(sql, new Object[]{customerId},
            (rs, rowNum) -> rs.getString("brand") + " " + rs.getString("model"));
    }
}
```

### 2.5 Mise à jour Frontend: Query Côté Client

```typescript
// File: car-rental-angular/src/app/services/offer-query.service.ts

@Injectable()
export class OfferQueryService {
  
  constructor(private http: HttpClient) {}
  
  // Lecture: Via vues matérialisées (rapide, pas de computation)
  getAvailableOffers(brand?: string): Observable<Offer[]> {
    const params = new HttpParams();
    if (brand) {
      params = params.set('brand', brand);
    }
    return this.http.get<Offer[]>('/api/query/offers', { params });
  }
  
  // Lecture: Stats d'enchères
  getAuctionStats(fromDate: Date): Observable<AuctionStat[]> {
    const params = new HttpParams().set('fromDate', fromDate.toISOString());
    return this.http.get<AuctionStat[]>('/api/query/auction-stats', { params });
  }
  
  // Lecture: Recommandations perso
  getRecommendedModels(customerId: string): Observable<string[]> {
    return this.http.get<string[]>(`/api/query/recommendations/${customerId}`);
  }
}
```

### 2.6 Séparation Command/Query Controllers

```java
// File: carRental/src/main/java/com/charroux/carRental/api/CarRentalCommandController.java

@RestController
@RequestMapping("/api/commands")
public class CarRentalCommandController {
    
    @Autowired
    private CarRentalCommandService commandService;
    
    @PostMapping("/auction/participate")
    public ResponseEntity<Car> participateInAuction(
            @RequestParam String carModelId,
            @RequestParam String carRentalCompanyId) {
        // WRITE operation
        return ResponseEntity.ok(commandService.participateInAuction(...));
    }
}

// File: carRental/src/main/java/com/charroux/carRental/api/CarRentalQueryController.java

@RestController
@RequestMapping("/api/query")
public class CarRentalQueryController {
    
    @Autowired
    private OfferQueryService queryService;
    
    @GetMapping("/offers")
    public ResponseEntity<List<OfferDTO>> getOffers(
            @RequestParam(required = false) String brand) {
        // READ operation (fast, via materialized views)
        return ResponseEntity.ok(queryService.getAvailableOffers(brand));
    }
    
    @GetMapping("/auction-stats")
    public ResponseEntity<List<AuctionStatsDTO>> getAuctionStats(
            @RequestParam LocalDate fromDate) {
        // READ operation
        return ResponseEntity.ok(queryService.getAuctionStats(fromDate));
    }
}
```

---

## 🚀 Phase 3: CQRS Complet + Event Sourcing (Semaines 15+)

### Objectif
✅ Event Sourcing (audit trail complet)  
✅ Projection complète (Command ≠ Query)  
✅ Scalabilité indépendante  
✅ Temporal queries (time-travel)  

### 3.1 Event Sourcing: Command Model Storage

```sql
-- File: database/migrations/V003__event_sourcing_tables.sql

-- Immutable event log (append-only)
CREATE TABLE event_store (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    metadata JSONB,  -- {userId, ipAddress, timestamp, etc.}
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL
);

CREATE INDEX idx_event_store_aggregate ON event_store(aggregate_id, aggregate_type);
CREATE INDEX idx_event_store_type ON event_store(event_type, created_at DESC);

-- Snapshot cache (optimization)
CREATE TABLE aggregate_snapshot (
    aggregate_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100),
    version INT,
    state JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Projection state tracking
CREATE TABLE projection_checkpoint (
    projection_name VARCHAR(100) PRIMARY KEY,
    event_store_position BIGINT,
    last_updated TIMESTAMP
);
```

### 3.2 Event Sourced Aggregate

```java
// File: carRental/src/main/java/com/charroux/carRental/domain/Auction.java

@Entity
public class Auction {
    @Id
    private UUID id;
    
    private AuctionStatus status;  // OPEN, CLOSED, WON
    private String winningBidderId;
    private Integer finalPrice;
    
    // Event sourced: reconstruct state from events
    public static Auction fromEvents(List<AuctionEvent> events) {
        Auction auction = new Auction();
        events.forEach(event -> {
            if (event instanceof AuctionStartedEvent e) {
                auction.id = e.getAuctionId();
                auction.status = AuctionStatus.OPEN;
            } else if (event instanceof AuctionBidPlacedEvent e) {
                if (e.getBidAmount() > (auction.finalPrice != null ? auction.finalPrice : 0)) {
                    auction.finalPrice = e.getBidAmount();
                    auction.winningBidderId = e.getBidderId();
                }
            } else if (event instanceof AuctionClosedEvent e) {
                auction.status = AuctionStatus.CLOSED;
            }
        });
        return auction;
    }
    
    // Get all changes as events (CQRS write model)
    public List<AuctionEvent> getUncommittedEvents() {
        // ...
    }
}

// Domain events (value objects, immutable)
@Data
public abstract class AuctionEvent {
    protected UUID aggregateId;
    protected Long timestamp;
}

@Data
public class AuctionStartedEvent extends AuctionEvent {
    private String carModelId;
    private Integer reservePrice;
}

@Data
public class AuctionBidPlacedEvent extends AuctionEvent {
    private String bidderId;
    private Integer bidAmount;
}
```

### 3.3 Event Store Repository

```java
// File: carRental/src/main/java/com/charroux/carRental/infra/EventStoreRepository.java

@Repository
public class EventStoreRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public void saveEvents(UUID aggregateId, String aggregateType, 
                          List<AuctionEvent> events) {
        events.forEach(event -> {
            String sql = """
                INSERT INTO event_store 
                (aggregate_id, aggregate_type, event_type, event_data, metadata, version)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
            jdbcTemplate.update(sql,
                aggregateId,
                aggregateType,
                event.getClass().getSimpleName(),
                objectMapper.writeValueAsString(event),
                objectMapper.writeValueAsString(Map.of(
                    "userId", getCurrentUser(),
                    "timestamp", System.currentTimeMillis()
                )),
                getNextVersion(aggregateId)
            );
        });
    }
    
    public List<AuctionEvent> getEventsFor(UUID aggregateId) {
        String sql = """
            SELECT event_data, event_type FROM event_store
            WHERE aggregate_id = ?
            ORDER BY id ASC
            """;
        
        return jdbcTemplate.query(sql, new Object[]{aggregateId},
            (rs, rowNum) -> {
                String eventType = rs.getString("event_type");
                String eventData = rs.getString("event_data");
                // Deserialize JSON to appropriate event type
                return (AuctionEvent) objectMapper.readValue(eventData, 
                    resolveEventClass(eventType));
            });
    }
    
    // Time-travel: reconstruct state at any point in time
    public Auction reconstructAt(UUID auctionId, Instant pointInTime) {
        String sql = """
            SELECT event_data, event_type FROM event_store
            WHERE aggregate_id = ? AND created_at <= ?
            ORDER BY id ASC
            """;
        
        List<AuctionEvent> events = jdbcTemplate.query(sql,
            new Object[]{auctionId, pointInTime},
            // ... mapper
        );
        
        return Auction.fromEvents(events);
    }
}
```

### 3.4 Projection CQRS Complète

```java
// File: carRental/src/main/java/com/charroux/carRental/projection/AuctionProjection.java

@Service
public class AuctionProjection {
    
    @Autowired
    private EventStoreRepository eventStore;
    
    @Autowired
    @Qualifier("readDataSource")
    private JdbcTemplate readDb;
    
    // Background job: projette l'event store → read model
    @Scheduled(fixedRate = 5000)
    public void projectEvents() {
        // 1. Get last projection checkpoint
        Long lastPosition = getProjectionCheckpoint("auction-projection");
        
        // 2. Get new events since checkpoint
        String sql = "SELECT id, event_type, event_data FROM event_store WHERE id > ?";
        List<AuctionEvent> newEvents = eventStore.getNewEventsSince(lastPosition);
        
        // 3. Update READ model for each event
        newEvents.forEach(event -> {
            if (event instanceof AuctionWonEvent e) {
                updateAuctionReadModel(e);
            }
        });
        
        // 4. Update checkpoint
        updateProjectionCheckpoint("auction-projection", lastPosition);
    }
    
    private void updateAuctionReadModel(AuctionWonEvent event) {
        String sql = """
            INSERT INTO auction_read_model (auction_id, status, winner_id, final_price, created_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(auction_id) DO UPDATE SET
                status = EXCLUDED.status,
                winner_id = EXCLUDED.winner_id,
                final_price = EXCLUDED.final_price
            """;
        
        readDb.update(sql,
            event.getAuctionId(),
            "WON",
            event.getWinnerId(),
            event.getFinalPrice(),
            event.getTimestamp()
        );
    }
}
```

### 3.5 Scalabilité Indépendante

```yaml
# k8s/base/cqrs-deployment.yaml

---
# Command Service (Write): Petite instance, haute disponibilité
apiVersion: apps/v1
kind: Deployment
metadata:
  name: carRental-command
spec:
  replicas: 3  # Replicas pour write consistency
  selector:
    matchLabels:
      app: carRental-command
  template:
    metadata:
      labels:
        app: carRental-command
    spec:
      containers:
      - name: carRental-command
        image: carRental-command:latest
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://postgres:5432/carental
        - name: SPRING_PROFILES_ACTIVE
          value: command

---
# Query Service (Read): Grosse instance, stateless
apiVersion: apps/v1
kind: Deployment
metadata:
  name: carRental-query
spec:
  replicas: 10  # Scale horizontalement pour lectures massives
  selector:
    matchLabels:
      app: carRental-query
  template:
    metadata:
      labels:
        app: carRental-query
    spec:
      containers:
      - name: carRental-query
        image: carRental-query:latest
        env:
        - name: SPRING_DATASOURCE_READ_URL
          value: jdbc:postgresql://postgres-read-replica:5432/carental_read
        - name: SPRING_PROFILES_ACTIVE
          value: query
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "2Gi"
            cpu: "1000m"

---
# Load Balancer: Route /api/commands → Command Service, /api/query → Query Service
apiVersion: v1
kind: Service
metadata:
  name: carRental-api
spec:
  type: LoadBalancer
  selector:
    app: carRental
  ports:
  - name: commands
    port: 8080
    targetPort: 8080
    nodePort: 30001
  - name: query
    port: 8081
    targetPort: 8081
    nodePort: 30002
```

---

## 📈 Métriques de Succès par Phase

### Phase 1: Event-Driven Solidifié
- [ ] Event delivery > 99.9% (monitored)
- [ ] Event lag < 100ms (p99)
- [ ] DLQ size = 0 (ou < 0.1% des events)
- [ ] Idempotence test coverage > 90%

### Phase 2: Read Model Séparé
- [ ] Query latency < 50ms (p99) via vues matérialisées
- [ ] Cache hit rate > 80%
- [ ] Read replica lag < 1 sec
- [ ] New `/query/*` endpoints tested end-to-end

### Phase 3: CQRS Complet
- [ ] Command service: 3-5 replicas, ~50ms latency
- [ ] Query service: 10+ replicas, ~20ms latency
- [ ] Event store: 100% audit trail completeness
- [ ] Time-travel queries functional (audit trails)

---

## 🔄 Sequencing & Dependencies

```
Phase 1: Event-Driven Solidifié
    │
    ├─ Redis Streams API ✅
    ├─ Event Consumers ✅
    ├─ Idempotence layer ✅
    └─ DLQ + Monitoring ✅
    
    ↓ (après 6 semaines)
    
Phase 2: Read Model + Vues Matérialisées
    │
    ├─ PostgreSQL read replica ✅
    ├─ Materialized views (3) ✅
    ├─ Projection services ✅
    ├─ Query service + cache ✅
    └─ Frontend query routes ✅
    
    ↓ (après 14 semaines)
    
Phase 3: CQRS Complet + Event Sourcing
    │
    ├─ Event store tables ✅
    ├─ Aggregate reconstruction ✅
    ├─ Projection scheduler ✅
    ├─ K8s CQRS deployment ✅
    └─ Time-travel queries ✅
```

---

## 📚 Ressources & Références

### Architecture
- **CQRS Pattern**: https://martinfowler.com/bliki/CQRS.html
- **Event Sourcing**: https://martinfowler.com/eaaDev/EventSourcing.html
- **Microservices Patterns**: https://microservices.io/patterns/data/event-sourcing.html

### Implémentation
- **Spring Data Redis Streams**: https://docs.spring.io/spring-data/redis/docs/current/reference/html/
- **PostgreSQL Materialized Views**: https://www.postgresql.org/docs/current/sql-creatematerializedview.html
- **Kafka vs Redis**: https://kafka.apache.org/documentation/#comparison

### Monitoring
- **Redis Stream Monitoring**: https://redis.io/docs/latest/commands/xinfo/
- **Spring Boot Actuator**: https://spring.io/guides/gs/actuator-service/

---

**Next Step**: Commencez par Phase 1! Voulez-vous que je crée les fichiers de configuration pour démarrer?
