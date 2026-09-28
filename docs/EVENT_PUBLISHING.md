# Event Publishing Architecture - auction.won Event

## Overview

This document describes the implementation of event-driven architecture for the Car Rental Service, starting with the **auction.won event** published when a car rental company wins an auction.

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                   Car Rental Service                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  CarRentalRestService.participateInAuctionByCarModelId  │   │
│  │  1. Call gRPC auction service (UNCHANGED)           │   │
│  │  2. Get result car                                  │   │
│  │  3. ✨ NEW: Publish AuctionWonEvent to Redis        │   │
│  └─────────────────────────────────────────────────────┘   │
│         │                                                    │
│         └──► RedisTemplate.opsForList()                     │
│              rightPush("auction:events:queue", event)      │
│                         │                                    │
└─────────────────────────┼────────────────────────────────────┘
                          │
                  ┌───────▼────────┐
                  │  Redis Queue   │
                  │  (7-day TTL)   │
                  └───────┬────────┘
                          │
         ┌────────────────┼────────────────┐
         │                │                │
    ┌────▼────┐      ┌────▼────┐      ┌────▼────┐
    │ Partner  │      │ Partner  │      │ Partner  │
    │ Service  │      │ Service  │      │ Service  │
    │   #1     │      │   #2     │      │   #N     │
    └──────────┘      └──────────┘      └──────────┘
    (Async Consumer)  (Async Consumer)  (Async Consumer)
```

## Implementation Details

### 1. Domain Event Class

**File**: `carRental/src/main/java/com/charroux/carRental/events/AuctionWonEvent.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionWonEvent {
    private String eventId;              // UUID for event deduplication
    private String rentalId;             // Car rental company identifier
    private Long carId;                  // Car database ID
    private String plateNumber;          // License plate
    private String customerId;           // Customer who won auction
    private String carBrand;             // e.g., "Ferrari"
    private String carModel;             // e.g., "F8"
    private Integer finalPrice;          // Auction final price
    private Integer originalPrice;       // Original price before discount
    private Integer discount;            // Discount amount applied
    private LocalDateTime rentalStartDate;  // Rental period start (now)
    private LocalDateTime rentalEndDate;    // Rental period end (now + 5 days)
    private Long timestamp;              // Event timestamp in milliseconds
    
    // Factory method with auto-generated ID and timestamps
    public static AuctionWonEvent create(
        String rentalId, Long carId, String plateNumber, String customerId,
        String carBrand, String carModel, Integer finalPrice, 
        Integer originalPrice, Integer discount) {
        // Auto-generates: eventId (UUID), timestamps, 5-day rental period
    }
}
```

### 2. Event Publisher Service

**File**: `carRental/src/main/java/com/charroux/carRental/events/AuctionEventPublisher.java`

```java
@Component
public class AuctionEventPublisher {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public boolean publishAuctionWon(AuctionWonEvent event) {
        try {
            // Serialize event with metadata wrapper
            String eventJson = objectMapper.writeValueAsString(event);
            String messageWithMetadata = String.format(
                "{\"eventType\":\"AuctionWon\",\"eventId\":\"%s\",\"timestamp\":%d,\"data\":%s}",
                event.getEventId(), event.getTimestamp(), eventJson);
            
            // Push to Redis queue (list data structure)
            Long result = redisTemplate.opsForList()
                .rightPush("auction:events:queue", messageWithMetadata);
            
            if (result != null && result > 0) {
                // Set 24-hour TTL to auto-expire messages
                redisTemplate.expire("auction:events:queue", 24L, TimeUnit.HOURS);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error publishing AuctionWonEvent: {}", e.getMessage());
            return false;
        }
    }
}
```

### 3. REST Endpoint Integration

**File**: `carRental/src/main/java/com/charroux/carRental/web/CarRentalRestService.java`

```java
@PostMapping("/auction/participate")
public ResponseEntity<AuctionResultDTO> participateInAuctionByCarModelId(...) {
    // 1. Existing gRPC auction logic (UNCHANGED - maintains backward compatibility)
    AuctionResultDTO result = auctionService.participateInAuction(...);
    
    if (resultCar != null) {
        try {
            // 2. NEW: Publish event after successful auction
            AuctionWonEvent event = AuctionWonEvent.create(...);
            boolean published = auctionEventPublisher.publishAuctionWon(event);
            
            if (!published) {
                logger.warn("⚠️ Event not published but auction succeeded");
                // Non-blocking: auction already completed
            }
        } catch (Exception e) {
            logger.error("Error publishing event", e);
            // Auction already succeeded, event failure is non-critical
        }
        return ResponseEntity.ok(result);
    }
}
```

## Redis Queue Configuration

### Queue Structure

- **Queue Key**: `auction:events:queue`
- **Data Type**: Redis List (FIFO queue)
- **TTL**: 24 hours (auto-expiring messages)
- **Max Memory**: 512MB with LRU eviction policy

### Message Format

```json
{
  "eventType": "AuctionWon",
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1701234567890,
  "data": {
    "eventId": "550e8400-e29b-41d4-a716-446655440000",
    "rentalId": "HERTZ-123",
    "carId": 42,
    "plateNumber": "ABC-123-XYZ",
    "customerId": "CUST-001",
    "carBrand": "Ferrari",
    "carModel": "F8",
    "finalPrice": 850,
    "originalPrice": 1000,
    "discount": 150,
    "rentalStartDate": "2024-12-20T10:30:00",
    "rentalEndDate": "2024-12-25T10:30:00",
    "timestamp": 1701234567890
  }
}
```

## Dependencies

### Maven/Gradle Dependencies

```gradle
// Redis Template for queue operations
implementation 'org.springframework.boot:spring-boot-starter-data-redis:3.2.0'
implementation 'io.lettuce:lettuce-core'

// Jackson with Java 8 date/time support
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'

// Lombok for reducing boilerplate
compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'
```

### Spring Configuration

**File**: `carRental/src/main/resources/application-dev.yml`

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
  jackson:
    default-property-inclusion: non_null
```

**File**: `carRental/src/main/resources/application-test.yml`

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
```

## Docker Compose Setup

**File**: `docker-compose.dev.yml`

```yaml
services:
  redis:
    image: redis:7-alpine
    container_name: car-rental-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5
    volumes:
      - redis-data:/data

  car-rental:
    build:
      context: .
      dockerfile: carRental/Dockerfile
    container_name: car-rental
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    depends_on:
      redis:
        condition: service_healthy

volumes:
  redis-data:
    driver: local
```

## Testing

### Unit Tests

**File**: `carRental/src/test/java/com/charroux/carRental/events/AuctionEventPublisherTest.java`

All 6 tests pass ✅:

1. **testPublishAuctionWonSuccess** - Verifies successful event publication
2. **testPublishAuctionWonMultipleEvents** - Tests queuing multiple events
3. **testPublishAuctionWonException** - Tests error handling and resilience
4. **testAuctionWonEventCreation** - Validates event factory method
5. **testAuctionWonEventRentalDates** - Confirms 5-day rental period calculation
6. **testAuctionWonEventConstructors** - Tests constructor and setters

### Running Tests

```bash
# Run only carRental tests
./gradlew :carRental:test

# Full module build including tests
./gradlew :carRental:build

# Run tests with debug output
./gradlew :carRental:test --info
```

### Test Results

```
BUILD SUCCESSFUL in 7s
✅ AuctionEventPublisherTest > testPublishAuctionWonSuccess() PASSED
✅ AuctionEventPublisherTest > testPublishAuctionWonException() PASSED
✅ AuctionEventPublisherTest > testPublishAuctionWonMultipleEvents() PASSED
✅ AuctionWonEventTest > testAuctionWonEventCreation() PASSED
✅ AuctionWonEventTest > testAuctionWonEventConstructors() PASSED
✅ AuctionWonEventTest > testAuctionWonEventRentalDates() PASSED

6 tests completed, 0 failed
```

## CI/CD Integration

### GitHub Actions Workflow

**File**: `.github/workflows/ci.yml`

The existing CI pipeline includes:

```yaml
- name: Build Java services
  run: |
    chmod +x gradlew
    ./gradlew :carRental:build        # ← Runs tests automatically
    ./gradlew :auctionServiceServer:build -x test
```

The `:carRental:build` task automatically:
1. Compiles Java sources
2. Processes resources
3. **Runs all unit tests**
4. Packages the JAR/Boot JAR
5. Validates the build

## Features & Non-Breaking Changes

### ✅ What's New

- **AuctionWonEvent**: Domain event capturing auction outcomes
- **AuctionEventPublisher**: Service for publishing events to Redis
- **Redis Queue**: Asynchronous message queue for partner services
- **Event Schema**: Metadata wrapper + serialized domain event
- **5-day Rental Period**: Auto-calculated in event factory method

### ✅ Backward Compatibility

- **Existing gRPC Auction Logic**: Completely unchanged
- **CarRentalRestService Endpoint**: Same request/response contract
- **Database Schema**: No modifications
- **Deployment**: Existing services work without changes

### 🔄 Non-Blocking Pattern

- Event publication failures **do not** affect auction success
- Exceptions in `publishAuctionWon()` are caught and logged
- REST endpoint returns success even if event fails to publish
- Enables graceful degradation while infrastructure stabilizes

## Partner Service Consumer Pattern

### Example Consumer Service (Future Phase)

```java
@Component
public class AuctionEventConsumer {
    private final RedisTemplate<String, String> redisTemplate;
    
    @Scheduled(fixedDelay = 5000)  // Poll every 5 seconds
    public void consumeAuctionEvents() {
        String event = redisTemplate.opsForList()
            .leftPop("auction:events:queue");
        
        if (event != null) {
            processAuctionWonEvent(event);
        }
    }
}
```

### Alternative: Redis Stream (Future Scalability)

For higher throughput or guaranteed delivery, migrate to Redis Streams:

```java
// Consumer with Redis Streams
StreamOperations<String, String, String> streamOps = 
    redisTemplate.opsForStream();

// Creates consumer group automatically
streamOps.createGroup("auction:events:stream", "partners-group");

// Poll with automatic acknowledgment
List<MapRecord<String, String, String>> messages = 
    streamOps.read(Consumer.from("partners-group", "consumer-1"), 
                   StreamReadOptions.empty().block(Duration.ofSeconds(1)),
                   StreamOffset.fromEnd("auction:events:stream"));
```

## Monitoring & Observability

### Redis Queue Health

```bash
# Check queue length
redis-cli LLEN auction:events:queue

# Peek at oldest event (non-destructive)
redis-cli LRANGE auction:events:queue 0 0

# Inspect recent events
redis-cli LRANGE auction:events:queue 0 9

# Check queue TTL
redis-cli TTL auction:events:queue
```

### Logging

The `AuctionEventPublisher` logs at three levels:

- **ERROR**: Serialization or Redis connection failures
- **WARN**: Event not published but auction succeeded
- **INFO**: (Future) Event published successfully

### Spring Boot Actuator Health

```bash
curl http://localhost:8080/actuator/health

# Response includes Redis health:
{
  "status": "UP",
  "components": {
    "redis": {"status": "UP"}
  }
}
```

## Next Steps

### Phase 2: Partner Services

1. Create consumer services that subscribe to `auction:events:queue`
2. Implement business logic for each partner (e.g., inventory sync, notifications)
3. Add idempotency keys using `eventId` for deduplication
4. Implement dead-letter queue for failed messages

### Phase 3: Module Federation

1. Extend car-rental-angular to load partner widgets via Module Federation
2. Partner services deploy Angular components as micro frontends
3. Shared styling and component libraries via npm

### Phase 4: Kubernetes Deployment

1. Deploy Redis in Kubernetes with StatefulSet
2. Configure PersistentVolumes for Redis data
3. Deploy carRental and partner services as Deployments
4. Use ConfigMaps for queue configuration
5. Monitor with Prometheus + Grafana

## Troubleshooting

### Issue: "Redis connection refused"

```
redis.connection.error: Connection refused

Solution:
1. Verify Redis is running: redis-cli ping
2. Check connection parameters: spring.redis.host/port
3. Check Docker Compose networking: docker network ls
```

### Issue: "Jackson serialization error: LocalDateTime not supported"

```
InvalidDefinitionException: Java 8 date/time type not supported

Solution:
// Ensure in build.gradle:
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'

// And in ObjectMapper initialization:
objectMapper.registerModule(new JavaTimeModule());
```

### Issue: "Queue growing indefinitely"

```
Solution:
1. Check consumer health (is anything reading from queue?)
2. Monitor queue length: redis-cli LLEN auction:events:queue
3. Verify TTL is set: redis-cli TTL auction:events:queue
4. Increase max-memory if needed in redis-server config
```

## References

- [Redis Lists Documentation](https://redis.io/docs/data-types/lists/)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Jackson Data Type JSR310](https://github.com/FasterXML/jackson-modules-java8/tree/master/datetime)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)

---

**Status**: ✅ Implementation Complete & Tested
**Last Updated**: December 2024
**CI/CD Status**: ✅ All tests passing in GitHub Actions
