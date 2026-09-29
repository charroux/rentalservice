# Implementing Hybrid State Management: Code Patterns

**Scope**: Partner microservices handling state with Redis events + dedicated DB  
**Target**: Phase 2 implementation (3-6 months from now)

---

## 🎯 Pattern Overview

```
                 Car Rental Service
                        │
                  Event published
                        │
                        ▼
            ┌───────────────────────┐
            │   Redis Event Queue   │
            │  auction:events:queue │
            └───────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
   ┌─────────┐    ┌─────────┐    ┌─────────┐
   │Rental   │    │Insurance│    │Fuel     │
   │Service  │    │Service  │    │Service  │
   └────┬────┘    └────┬────┘    └────┬────┘
        │              │              │
        ▼              ▼              ▼
   1. Receive event from Redis
   2. Extract data
   3. Check idempotency (already processed?)
   4. Update state in PostgreSQL
   5. Mark as processed
   6. Return success
        │              │              │
        ▼              ▼              ▼
   ┌──────────────────────────────────────┐
   │  Dedicated State DB (PostgreSQL)     │
   │  rental_state, insurance_state, etc. │
   └──────────────────────────────────────┘
```

---

## 📋 Implementation Patterns

### Pattern 1: Event Consumer with Idempotent Processing

```java
// File: RentalEventListener.java
@Component
public class RentalEventListener {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private RentalStateRepository stateRepository;
    
    @Autowired
    private EventProcessingRepository eventRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final Logger logger = LoggerFactory.getLogger(RentalEventListener.class);
    
    /**
     * Listen for auction events and update rental state
     * Pattern: Idempotent - safe to call multiple times for same event
     */
    @Transactional
    public void handleAuctionWonEvent(String eventJson) {
        try {
            // 1. Parse event
            AuctionEventMetadata metadata = objectMapper.readValue(
                eventJson, 
                AuctionEventMetadata.class
            );
            
            // 2. Check if already processed (idempotency)
            if (isEventProcessed(metadata.eventId)) {
                logger.info("Event {} already processed, skipping", metadata.eventId);
                return;
            }
            
            // 3. Extract domain data
            AuctionWonEvent event = objectMapper.readValue(
                metadata.data, 
                AuctionWonEvent.class
            );
            
            // 4. Update state in dedicated DB (transaction)
            updateRentalState(event);
            
            // 5. Mark event as processed (atomically)
            markEventAsProcessed(metadata.eventId, metadata.timestamp);
            
            logger.info("Event {} processed successfully", metadata.eventId);
            
        } catch (Exception e) {
            // Non-blocking: log error, but don't fail
            logger.error("Error processing event", e);
            // Could implement retry logic here
        }
    }
    
    /**
     * Check if event already processed (idempotency key)
     */
    private boolean isEventProcessed(String eventId) {
        ProcessedEvent processed = eventRepository.findByEventId(eventId);
        return processed != null;
    }
    
    /**
     * Update rental state in PostgreSQL
     * This is transaction-protected, durable
     */
    private void updateRentalState(AuctionWonEvent event) {
        RentalState state = stateRepository
            .findByAuctionId(event.getAuctionId())
            .orElse(new RentalState());
        
        state.setAuctionId(event.getAuctionId());
        state.setRentalId(event.getRentalId());
        state.setCarId(event.getCarId());
        state.setStatus(RentalStatus.CONFIRMED);
        state.setFinalPrice(event.getFinalPrice());
        state.setUpdatedAt(Instant.now());
        
        stateRepository.save(state);
    }
    
    /**
     * Mark event as processed (for idempotency)
     * This prevents double-processing if event arrives twice
     */
    @Transactional
    private void markEventAsProcessed(String eventId, Long timestamp) {
        ProcessedEvent processed = new ProcessedEvent();
        processed.setEventId(eventId);
        processed.setProcessedAt(Instant.now());
        processed.setEventTimestamp(new Timestamp(timestamp));
        
        eventRepository.save(processed);
    }
}
```

### Entities for Database Schema

```java
// File: RentalState.java
@Entity
@Table(name = "rental_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private Long auctionId;
    
    private Long rentalId;
    private Integer carId;
    
    @Enumerated(EnumType.STRING)
    private RentalStatus status;  // PENDING, CONFIRMED, ACTIVE, COMPLETED
    
    private BigDecimal finalPrice;
    
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

// File: ProcessedEvent.java
@Entity
@Table(name = "processed_events", indexes = {
    @Index(name = "idx_event_id", columnList = "event_id", unique = true)
})
@Data
@NoArgsConstructor
public class ProcessedEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String eventId;
    
    private Instant processedAt;
    private Timestamp eventTimestamp;
    
    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }
}

// File: RentalStatus.java
public enum RentalStatus {
    PENDING,       // Event received, not yet confirmed
    CONFIRMED,     // Auction won, rental confirmed
    ACTIVE,        // Rental started
    COMPLETED,     // Rental finished
    CANCELLED
}
```

### Repositories

```java
// File: RentalStateRepository.java
@Repository
public interface RentalStateRepository extends JpaRepository<RentalState, Long> {
    Optional<RentalState> findByAuctionId(Long auctionId);
    Optional<RentalState> findByRentalId(Long rentalId);
    List<RentalState> findByStatus(RentalStatus status);
}

// File: EventProcessingRepository.java
@Repository
public interface EventProcessingRepository extends JpaRepository<ProcessedEvent, Long> {
    Optional<ProcessedEvent> findByEventId(String eventId);
}
```

---

## 🔄 Pattern 2: Polling Consumer from Redis

```java
// File: RedisEventPollingService.java
@Component
public class RedisEventPollingService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private RentalEventListener eventListener;
    
    private static final Logger logger = LoggerFactory.getLogger(RedisEventPollingService.class);
    private static final String QUEUE_KEY = "auction:events:queue";
    private static final long POLL_INTERVAL_MS = 1000;  // Poll every second
    
    @PostConstruct
    public void startPolling() {
        Thread pollingThread = new Thread(this::pollEvents, "Redis-Event-Poller");
        pollingThread.setDaemon(false);
        pollingThread.start();
        logger.info("Redis event polling started");
    }
    
    /**
     * Poll events from Redis queue and process them
     * Uses LPOP (left pop) for FIFO order
     */
    private void pollEvents() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // LPOP: Remove and return first element
                String eventJson = (String) redisTemplate.opsForList()
                    .leftPop(QUEUE_KEY, 100, TimeUnit.MILLISECONDS);
                
                if (eventJson != null) {
                    logger.debug("Received event from Redis: {}", eventJson);
                    
                    // Process event with idempotency
                    eventListener.handleAuctionWonEvent(eventJson);
                } else {
                    // Queue empty, wait before polling again
                    Thread.sleep(POLL_INTERVAL_MS);
                }
                
            } catch (InterruptedException e) {
                logger.info("Redis event polling interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error polling Redis events", e);
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
```

---

## 🔍 Pattern 3: Query State via REST API

```java
// File: RentalStateController.java
@RestController
@RequestMapping("/api/rental-service")
public class RentalStateController {
    
    @Autowired
    private RentalStateRepository stateRepository;
    
    /**
     * Get rental state by rental ID
     * This queries the permanent state DB, not Redis
     */
    @GetMapping("/rental/{rentalId}/state")
    public ResponseEntity<RentalStateDTO> getRentalState(@PathVariable Long rentalId) {
        return stateRepository.findByRentalId(rentalId)
            .map(this::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all rentals by status
     */
    @GetMapping("/rentals/by-status/{status}")
    public ResponseEntity<List<RentalStateDTO>> getRentalsByStatus(
        @PathVariable String status
    ) {
        RentalStatus rentalStatus = RentalStatus.valueOf(status.toUpperCase());
        List<RentalState> states = stateRepository.findByStatus(rentalStatus);
        return ResponseEntity.ok(
            states.stream()
                .map(this::toDTO)
                .collect(Collectors.toList())
        );
    }
    
    private RentalStateDTO toDTO(RentalState state) {
        return new RentalStateDTO(
            state.getRentalId(),
            state.getAuctionId(),
            state.getCarId(),
            state.getStatus().toString(),
            state.getFinalPrice(),
            state.getUpdatedAt()
        );
    }
}

// File: RentalStateDTO.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalStateDTO {
    private Long rentalId;
    private Long auctionId;
    private Integer carId;
    private String status;
    private BigDecimal finalPrice;
    private Instant updatedAt;
}
```

---

## 🗄️ Database Schema (SQL)

```sql
-- Rental state table (permanent storage)
CREATE TABLE rental_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auction_id BIGINT UNIQUE NOT NULL,
    rental_id BIGINT NOT NULL,
    car_id INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    final_price DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rental_id (rental_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- Processed events table (idempotency tracking)
CREATE TABLE processed_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_timestamp TIMESTAMP,
    INDEX idx_event_id (event_id),
    INDEX idx_processed_at (processed_at)
);

-- Audit log (optional, for compliance)
CREATE TABLE rental_state_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rental_id BIGINT NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(255),
    change_reason VARCHAR(500),
    INDEX idx_rental_id (rental_id),
    INDEX idx_changed_at (changed_at)
);
```

---

## 📊 Spring Boot Configuration

```yaml
# application.yml - Rental Service config
spring:
  application:
    name: rental-service
  
  datasource:
    url: jdbc:mysql://localhost:3306/rental_service?useSSL=false&serverTimezone=UTC
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 600000
  
  jpa:
    hibernate:
      ddl-auto: validate  # Don't auto-create schema
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
        jdbc:
          batch_size: 20
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

# Logging
logging:
  level:
    com.charroux.rentalservice: DEBUG
    org.springframework.data: WARN
    org.hibernate: WARN
```

---

## 🧪 Testing: Idempotent Event Handler

```java
// File: RentalEventListenerTest.java
@SpringBootTest
@DataJpaTest
class RentalEventListenerTest {
    
    @Autowired
    private RentalEventListener eventListener;
    
    @Autowired
    private RentalStateRepository stateRepository;
    
    @Autowired
    private EventProcessingRepository eventRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String eventJson;
    
    @BeforeEach
    void setUp() throws JsonProcessingException {
        // Create test event
        AuctionWonEvent event = AuctionWonEvent.create(
            123L,  // rentalId
            1,     // carId
            "ABC123",  // plateNumber
            1L,    // customerId
            "Tesla",   // carBrand
            "Model S",  // carModel
            5000L  // finalPrice
        );
        
        // Wrap in metadata
        AuctionEventMetadata metadata = new AuctionEventMetadata();
        metadata.setEventType("AuctionWon");
        metadata.setEventId(UUID.randomUUID().toString());
        metadata.setTimestamp(System.currentTimeMillis());
        metadata.setData(objectMapper.writeValueAsString(event));
        
        eventJson = objectMapper.writeValueAsString(metadata);
    }
    
    @Test
    void testEventProcessedSuccessfully() {
        // Act
        eventListener.handleAuctionWonEvent(eventJson);
        
        // Assert
        List<RentalState> states = stateRepository.findAll();
        assertEquals(1, states.size());
        
        RentalState state = states.get(0);
        assertEquals(123L, state.getRentalId());
        assertEquals(RentalStatus.CONFIRMED, state.getStatus());
        assertEquals(BigDecimal.valueOf(5000), state.getFinalPrice());
    }
    
    @Test
    void testEventIdempotencyDouble() {
        // Act: Process same event twice
        eventListener.handleAuctionWonEvent(eventJson);
        eventListener.handleAuctionWonEvent(eventJson);  // Same event again
        
        // Assert: Only ONE state record (not two)
        List<RentalState> states = stateRepository.findAll();
        assertEquals(1, states.size());
        
        // ProcessedEvent table has only ONE entry
        List<ProcessedEvent> processed = eventRepository.findAll();
        assertEquals(1, processed.size());
    }
    
    @Test
    void testInvalidEventFormatIgnored() {
        // Act: Send malformed JSON
        eventListener.handleAuctionWonEvent("invalid json");
        
        // Assert: No state created (but no exception thrown)
        List<RentalState> states = stateRepository.findAll();
        assertEquals(0, states.size());
    }
}
```

---

## 🔌 Integration Test: End-to-End Flow

```java
// File: RentalEventEndToEndTest.java
@SpringBootTest
@IntegrationTest
@Testcontainers
class RentalEventEndToEndTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .waitingFor(Wait.forListeningPort());
    
    @Container
    static GenericContainer<?> mysql = new GenericContainer<>(DockerImageName.parse("mysql:8.0"))
        .withExposedPorts(3306)
        .withEnv("MYSQL_ROOT_PASSWORD", "test")
        .withEnv("MYSQL_DATABASE", "rental_service")
        .waitingFor(Wait.forLogMessage(".*ready for connections.*", 1));
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private RentalStateRepository stateRepository;
    
    @Autowired
    private RedisEventPollingService pollingService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testEventFlowFromRedisToDatabase() throws Exception {
        // 1. Publish event to Redis
        AuctionWonEvent event = AuctionWonEvent.create(999L, 1, "XYZ789", 1L, "BMW", "X5", 8000L);
        
        AuctionEventMetadata metadata = new AuctionEventMetadata();
        metadata.setEventType("AuctionWon");
        metadata.setEventId(UUID.randomUUID().toString());
        metadata.setTimestamp(System.currentTimeMillis());
        metadata.setData(objectMapper.writeValueAsString(event));
        
        String eventJson = objectMapper.writeValueAsString(metadata);
        
        // Push to Redis queue
        redisTemplate.opsForList().rightPush("auction:events:queue", eventJson);
        
        // 2. Wait for polling to process
        Thread.sleep(2000);  // Give polling service time
        
        // 3. Verify state in database
        Optional<RentalState> state = stateRepository.findByRentalId(999L);
        assertTrue(state.isPresent());
        assertEquals(RentalStatus.CONFIRMED, state.get().getStatus());
        assertEquals(BigDecimal.valueOf(8000), state.get().getFinalPrice());
        
        // 4. Verify Redis queue is empty
        Long queueSize = redisTemplate.opsForList().size("auction:events:queue");
        assertEquals(0, queueSize.longValue());
    }
}
```

---

## 📈 Monitoring & Observability

```java
// File: EventProcessingMetrics.java
@Component
public class EventProcessingMetrics {
    
    private final MeterRegistry meterRegistry;
    private final AtomicLong eventsProcessed = new AtomicLong(0);
    private final AtomicLong eventsFailed = new AtomicLong(0);
    private final Timer eventProcessingTimer;
    
    public EventProcessingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Register metrics
        Gauge.builder("rental.events.processed.total", eventsProcessed::get)
            .description("Total events processed")
            .register(meterRegistry);
        
        Gauge.builder("rental.events.failed.total", eventsFailed::get)
            .description("Total events failed")
            .register(meterRegistry);
        
        this.eventProcessingTimer = Timer.builder("rental.event.processing.time")
            .description("Time to process event")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }
    
    public void recordEventProcessed() {
        eventsProcessed.incrementAndGet();
    }
    
    public void recordEventFailed() {
        eventsFailed.incrementAndGet();
    }
    
    public Timer.Sample recordEventProcessing() {
        return Timer.start(meterRegistry);
    }
}

// Usage in listener:
@Component
public class RentalEventListenerWithMetrics {
    
    @Autowired
    private EventProcessingMetrics metrics;
    
    public void handleAuctionWonEvent(String eventJson) {
        Timer.Sample sample = metrics.recordEventProcessing();
        
        try {
            // Process...
            metrics.recordEventProcessed();
        } catch (Exception e) {
            metrics.recordEventFailed();
        } finally {
            sample.stop(metrics.eventProcessingTimer);
        }
    }
}
```

---

## ✅ Deployment Checklist

- [ ] PostgreSQL database created with schemas
- [ ] Flyway/Liquibase migrations applied
- [ ] Redis connection configured and tested
- [ ] Event listener thread starts on application boot
- [ ] Idempotency table has unique index on event_id
- [ ] Monitoring metrics exposed on /actuator/metrics
- [ ] Health check on /actuator/health includes DB and Redis
- [ ] Application can be scaled horizontally (multiple instances)
- [ ] Duplicate event handling verified via tests
- [ ] Error logging configured for operations team

---

## 🎯 Best Practices Summary

✅ **DO:**
- Use idempotency keys for duplicate detection
- Store state in permanent DB (PostgreSQL/MongoDB)
- Keep events temporary in Redis (TTL: 24-72h)
- Process events in transaction (atomic update)
- Log all state changes for audit
- Monitor event processing latency

❌ **DON'T:**
- Use Redis as permanent state store
- Assume exactly-once delivery
- Skip idempotency checks
- Process events without transaction boundary
- Query state from Redis (it may expire)
- Store sensitive data in Redis (not encrypted)

---

## 📞 Next Steps

1. **Schema Review**: Have DBA review SQL schemas
2. **Load Testing**: Test with 1000 events/sec
3. **Failover Testing**: Test duplicate event handling
4. **Monitoring Setup**: Configure alerts on metrics
5. **Documentation**: Document state model for team
6. **Rollout Plan**: Plan canary deployment

Done! You have a production-ready pattern. 🚀
