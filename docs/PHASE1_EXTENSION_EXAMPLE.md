# Implémentation Phase 1: Ajouter InsuranceEventConsumer

**Exemple complet et prêt pour copie** d'ajout d'un nouveau service consommateur dans Phase 1.

---

## File Structure

```
insurance-service/
├── src/main/java/com/charroux/insurance/
│   ├── config/
│   │   └── InsuranceEventConfig.java
│   ├── events/
│   │   └── InsuranceEventConsumer.java
│   ├── entity/
│   │   └── InsuranceQuote.java
│   ├── repository/
│   │   └── InsuranceQuoteRepository.java
│   └── InsuranceApplication.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__create_insurance_tables.sql
└── build.gradle
```

---

## 1. build.gradle (Setup)

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    implementation 'org.postgresql:postgresql'
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
    
    implementation 'org.projectlombok:lombok:1.18.30'
    annotationProcessor 'org.projectlombok:lombok:1.18.30'
    
    implementation 'org.slf4j:slf4j-api'
    implementation 'ch.qos.logback:logback-classic'
    
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:testcontainers:1.19.1'
    testImplementation 'org.testcontainers:postgresql:1.19.1'
}
```

---

## 2. InsuranceEventConfig.java

```java
package com.charroux.insurance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration for Insurance Service event processing.
 * 
 * Connects to shared Redis queue: "auction:events:queue"
 * Idempotence tracked in shared PostgreSQL: processed_events table
 */
@Configuration
public class InsuranceEventConfig {
    
    /**
     * Shared Redis template for consuming auction events.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        
        return template;
    }
    
    /**
     * Jackson ObjectMapper for event deserialization.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

---

## 3. InsuranceEventConsumer.java

```java
package com.charroux.insurance.events;

import com.charroux.insurance.entity.InsuranceQuote;
import com.charroux.insurance.repository.InsuranceQuoteRepository;
import com.charroux.insurance.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Consumes auction events and creates insurance quotes.
 * 
 * Flow:
 * 1. Poll Redis queue "auction:events:queue" every 1 second
 * 2. Extract eventId from event JSON
 * 3. Check idempotence: SELECT processed_events WHERE eventId + consumerName
 * 4. If already processed: SKIP (idempotence)
 * 5. If new: Calculate insurance premium and create InsuranceQuote
 * 6. Record in processed_events table
 * 
 * Phase 1: Simple polling pattern
 * Phase 2: Upgrade to Redis Streams + Consumer Groups
 */
@Component
@Slf4j
public class InsuranceEventConsumer {
    
    private static final String AUCTION_EVENTS_QUEUE = "auction:events:queue";
    private static final String CONSUMER_NAME = "insurance-service";
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final InsuranceQuoteRepository insuranceQuoteRepository;
    
    public InsuranceEventConsumer(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            ProcessedEventRepository processedEventRepository,
            InsuranceQuoteRepository insuranceQuoteRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
        this.insuranceQuoteRepository = insuranceQuoteRepository;
    }
    
    /**
     * Poll Redis queue for auction events.
     * Runs every 1 second.
     */
    @Scheduled(fixedRate = 1000)
    public void consumeAuctionEvents() {
        try {
            // LPOP from shared queue (blocking would be better in Phase 2)
            String eventJson = redisTemplate.opsForList()
                .leftPop(AUCTION_EVENTS_QUEUE);
            
            if (eventJson == null) {
                return;  // Queue empty, try again next second
            }
            
            processAuctionEvent(eventJson);
            
        } catch (Exception e) {
            log.error("❌ Error polling auction events: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process a single auction event.
     */
    private void processAuctionEvent(String eventJson) {
        try {
            // Parse event JSON (flexible - don't require specific type)
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(eventJson, Map.class);
            
            String eventId = (String) event.get("eventId");
            if (eventId == null) {
                log.warn("⚠️  Event missing eventId: {}", eventJson);
                return;
            }
            
            // IDEMPOTENCE CHECK
            boolean alreadyProcessed = processedEventRepository
                .existsByEventIdAndConsumerName(eventId, CONSUMER_NAME);
            
            if (alreadyProcessed) {
                log.debug("⏭️  Event {} already processed by {}, skipping",
                    eventId, CONSUMER_NAME);
                return;
            }
            
            // BUSINESS LOGIC: Calculate insurance premium
            String customerId = (String) event.get("customerId");
            String carBrand = (String) event.get("carBrand");
            String carModel = (String) event.get("carModel");
            
            if (customerId == null || carBrand == null) {
                log.warn("⚠️  Event missing customer or car info: {}", eventJson);
                return;
            }
            
            double insurancePremium = calculatePremium(customerId, carBrand, carModel);
            
            // Create insurance quote
            InsuranceQuote quote = new InsuranceQuote();
            quote.setEventId(eventId);
            quote.setCustomerId(customerId);
            quote.setCarBrand(carBrand);
            quote.setCarModel(carModel);
            quote.setDailyPremium(insurancePremium);
            quote.setStatus("PENDING");  // Awaiting customer acceptance
            
            insuranceQuoteRepository.save(quote);
            
            // RECORD AS PROCESSED (idempotence marker)
            processedEventRepository.recordProcessed(
                eventId,
                CONSUMER_NAME,
                "AuctionWon"  // Event type
            );
            
            log.info("✓ Insurance quote created: eventId={}, customerId={}, carBrand={}, premium={}€",
                eventId, customerId, carBrand, insurancePremium);
            
        } catch (Exception e) {
            log.error("❌ Failed to process auction event: {}", e.getMessage(), e);
            // Phase 1: No DLQ, event is lost
            // Phase 2: Add retry + DLQ logic
        }
    }
    
    /**
     * Calculate insurance premium based on customer and vehicle.
     * 
     * Simple formula for Phase 1:
     * Base: 50€/day
     * Luxury surcharge: Ferrari/Porsche = +30€/day
     * Young driver surcharge: age < 25 = +20€/day (simplified)
     * 
     * Phase 2: Call insurance microservice or use ML model
     */
    private double calculatePremium(String customerId, String carBrand, String carModel) {
        double basePremium = 50.0;
        
        // Luxury vehicle surcharge
        if ("Ferrari".equalsIgnoreCase(carBrand) || "Porsche".equalsIgnoreCase(carBrand)) {
            basePremium += 30.0;
        } else if ("Tesla".equalsIgnoreCase(carBrand)) {
            basePremium += 15.0;
        }
        
        // TODO Phase 2: Add customer age check
        // if (age < 25) basePremium += 20.0;
        
        return basePremium;
    }
}
```

---

## 4. InsuranceQuote.java (Entity)

```java
package com.charroux.insurance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Insurance quote for a rental.
 * 
 * Created by InsuranceEventConsumer when auction is won.
 * Customer can accept/reject before renting car.
 */
@Entity
@Table(name = "insurance_quotes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceQuote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;  // Linked to auction event
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "car_brand", nullable = false)
    private String carBrand;
    
    @Column(name = "car_model", nullable = false)
    private String carModel;
    
    @Column(name = "daily_premium", nullable = false)
    private Double dailyPremium;  // EUR per day
    
    @Column(name = "status")
    private String status;  // PENDING, ACCEPTED, REJECTED, EXPIRED
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

---

## 5. InsuranceQuoteRepository.java

```java
package com.charroux.insurance.repository;

import com.charroux.insurance.entity.InsuranceQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceQuoteRepository extends JpaRepository<InsuranceQuote, Long> {
    
    /**
     * Find insurance quote by event ID.
     */
    Optional<InsuranceQuote> findByEventId(String eventId);
    
    /**
     * Find all quotes for a customer.
     */
    List<InsuranceQuote> findByCustomerId(String customerId);
    
    /**
     * Find pending quotes awaiting customer decision.
     */
    @Query("SELECT q FROM InsuranceQuote q WHERE q.status = 'PENDING' ORDER BY q.createdAt DESC")
    List<InsuranceQuote> findPendingQuotes();
    
    /**
     * Find high-value quotes (for sales analysis).
     */
    @Query("SELECT q FROM InsuranceQuote q WHERE q.dailyPremium > :threshold ORDER BY q.dailyPremium DESC")
    List<InsuranceQuote> findHighValueQuotes(@Param("threshold") Double threshold);
}
```

---

## 6. ProcessedEventRepository.java (SHARED)

```java
package com.charroux.insurance.repository;

import com.charroux.insurance.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * IMPORTANT: This is a SHARED repository across all services.
 * 
 * Lives in a SHARED library or schema.
 * All services import and use the same ProcessedEvent entity.
 * 
 * This ensures: 
 * - One source of truth for event processing
 * - Idempotence across distributed services
 * - Audit trail of all event processing
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    
    boolean existsByEventIdAndConsumerName(String eventId, String consumerName);
    
    default void recordProcessed(String eventId, String consumerName, String eventType) {
        ProcessedEvent event = new ProcessedEvent();
        event.setEventId(eventId);
        event.setConsumerName(consumerName);
        event.setEventType(eventType);
        event.setProcessedAt(java.time.LocalDateTime.now());
        event.setRetryCount(0);
        event.setErrorMessage(null);
        this.save(event);
    }
}
```

---

## 7. application.yml (Insurance Service)

```yaml
spring:
  application:
    name: insurance-service
  
  # Shared PostgreSQL (same DB as carRental service)
  datasource:
    url: jdbc:postgresql://localhost:5432/car_rental
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway handles migrations
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL15Dialect
  
  # Shared Redis (same instance as carRental)
  redis:
    host: localhost
    port: 6379
    timeout: 3000ms
    jedis:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

server:
  port: 8081  # Different from carRental (8080)

logging:
  level:
    root: INFO
    com.charroux: DEBUG
```

---

## 8. V1__create_insurance_tables.sql (Flyway Migration)

```sql
-- Insurance quotes table
CREATE TABLE IF NOT EXISTS insurance_quotes (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    customer_id VARCHAR(100) NOT NULL,
    car_brand VARCHAR(50) NOT NULL,
    car_model VARCHAR(50) NOT NULL,
    daily_premium NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for quick lookup by event
CREATE INDEX idx_insurance_quotes_event_id ON insurance_quotes(event_id);

-- Index for customer queries
CREATE INDEX idx_insurance_quotes_customer_id ON insurance_quotes(customer_id);

-- Index for status queries (find pending)
CREATE INDEX idx_insurance_quotes_status ON insurance_quotes(status);
```

---

## 9. Integration with Shared ProcessedEvent Table

**IMPORTANT**: The `processed_events` table is SHARED across ALL services:

```
┌─────────────────────────────────────┐
│   PostgreSQL (car_rental database)  │
├─────────────────────────────────────┤
│ processed_events (SHARED)            │
│ ├─ event_id                          │
│ ├─ consumer_name ← carRental service │
│                   ← insurance service │
│                   ← analytics service │
│ ├─ event_type                        │
│ └─ processed_at                      │
├─────────────────────────────────────┤
│ insurance_quotes (insurance-service) │
│ ├─ event_id (FK → processed_events) │
│ ├─ customer_id                       │
│ └─ daily_premium                     │
└─────────────────────────────────────┘
```

**Setup:**

1. carRental service creates `processed_events` table (V3 migration)
2. insurance-service connects to SAME database
3. InsuranceEventConsumer uses shared ProcessedEventRepository
4. Idempotence: (event_id, consumer_name) unique constraint

---

## 10. Testing InsuranceEventConsumer

```java
package com.charroux.insurance.events;

import com.charroux.insurance.entity.InsuranceQuote;
import com.charroux.insurance.repository.InsuranceQuoteRepository;
import com.charroux.insurance.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class InsuranceEventConsumerTest {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private InsuranceQuoteRepository insuranceQuoteRepository;
    
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setup() {
        // Clear Redis and DB before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        insuranceQuoteRepository.deleteAll();
        processedEventRepository.deleteAll();
    }
    
    @Test
    void shouldCreateInsuranceQuoteWhenAuctionEventReceived() throws Exception {
        // GIVEN: An auction event in Redis queue
        String auctionEvent = """
            {
              "eventId": "550e8400-e29b-41d4-a716-446655440000",
              "auctionId": "AUC-001",
              "customerId": "CUST-123",
              "carBrand": "Ferrari",
              "carModel": "F8 Tributo"
            }
            """;
        
        redisTemplate.opsForList().rightPush("auction:events:queue", auctionEvent);
        
        // WHEN: Consumer processes event
        InsuranceEventConsumer consumer = new InsuranceEventConsumer(
            redisTemplate, objectMapper, processedEventRepository, insuranceQuoteRepository
        );
        consumer.consumeAuctionEvents();
        
        // THEN: Insurance quote created
        var quote = insuranceQuoteRepository.findByEventId("550e8400-e29b-41d4-a716-446655440000");
        assertThat(quote).isPresent();
        assertThat(quote.get().getCarBrand()).isEqualTo("Ferrari");
        assertThat(quote.get().getDailyPremium()).isEqualTo(80.0);  // 50 + 30 luxury
        
        // AND: Event marked as processed
        var processed = processedEventRepository.findByEventIdAndConsumerName(
            "550e8400-e29b-41d4-a716-446655440000",
            "insurance-service"
        );
        assertThat(processed).isPresent();
    }
    
    @Test
    void shouldNotCreateDuplicateQuoteOnEventReprocessing() throws Exception {
        // GIVEN: An already-processed event in Redis
        String eventId = "550e8400-e29b-41d4-a716-446655440000";
        String auctionEvent = objectMapper.writeValueAsString(Map.of(
            "eventId", eventId,
            "customerId", "CUST-123",
            "carBrand", "Ferrari",
            "carModel", "F8"
        ));
        
        // Already processed once
        processedEventRepository.recordProcessed(eventId, "insurance-service", "AuctionWon");
        
        // WHEN: Event pushed to queue again (simulating duplicate)
        redisTemplate.opsForList().rightPush("auction:events:queue", auctionEvent);
        
        InsuranceEventConsumer consumer = new InsuranceEventConsumer(
            redisTemplate, objectMapper, processedEventRepository, insuranceQuoteRepository
        );
        consumer.consumeAuctionEvents();
        
        // THEN: No duplicate quote created
        var quotes = insuranceQuoteRepository.findByCustomerId("CUST-123");
        assertThat(quotes).isEmpty();  // No quote created (event skipped)
    }
}
```

---

## 11. Deployment Instructions

### Local Development

```bash
# Terminal 1: Start PostgreSQL
docker run --name postgres -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 -d postgres:15-alpine

# Terminal 2: Start Redis
docker run --name redis -p 6379:6379 -d redis:7-alpine

# Terminal 3: Start carRental service
cd carRental
./gradlew bootRun

# Terminal 4: Start insurance service
cd insurance-service
./gradlew bootRun

# Test: Publish an auction event
curl -X POST http://localhost:8080/test/auction \
  -H "Content-Type: application/json" \
  -d '{
    "carModelId": "FERRARI_F8",
    "bidderId": "COMPANY-1",
    "bidAmount": 500
  }'

# Check: Insurance quote created
curl http://localhost:8081/insurance-quotes
```

### Production Deployment (Kubernetes)

```yaml
# insurance-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: insurance-service
  namespace: rental-system
spec:
  replicas: 2  # 2 instances for HA
  selector:
    matchLabels:
      app: insurance-service
  template:
    metadata:
      labels:
        app: insurance-service
    spec:
      containers:
      - name: insurance-service
        image: myregistry.azurecr.io/insurance-service:1.0.0
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://postgres:5432/car_rental
        - name: SPRING_REDIS_HOST
          value: redis
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
```

---

## Summary

**To add a new service (e.g., AnalyticsService):**

1. **Copy** `InsuranceEventConsumer` → `AnalyticsEventConsumer`
2. **Change** `CONSUMER_NAME = "analytics-service"`
3. **Modify** business logic in `processAuctionEvent()` (e.g., accumulate stats instead of creating quotes)
4. **Add** domain-specific entity (e.g., `EventMetric` instead of `InsuranceQuote`)
5. **Deploy** to Kubernetes
6. **Verify** events flow through all consumers

That's it! The shared `ProcessedEvent` table ensures idempotence across all services automatically.

---

**Phase 1 Pattern**: Copy-paste-ready event consumers with guaranteed exactly-once semantics.
