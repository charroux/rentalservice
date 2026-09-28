# Onboarding Rapide: Redis Event-Driven pour Projets Java/Spring

## ⚡ TL;DR (2 minutes)

**Vous devez intégrer Redis events dans votre app Spring Boot?**

### Stack minimale

```gradle
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis:3.2.0'
implementation 'io.lettuce:lettuce-core'
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'  // ⚠️ CRITICAL

compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'
```

### Code minimum (3 fichiers)

**1. Event Domain Class**
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyEvent {
    private String eventId;          // UUID.randomUUID().toString()
    private LocalDateTime timestamp; // LocalDateTime.now()
    private String resourceId;
    private String eventType;
    
    public static MyEvent create(String resourceId, String eventType) {
        MyEvent event = new MyEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());
        event.setResourceId(resourceId);
        event.setEventType(eventType);
        return event;
    }
}
```

**2. Event Publisher**
```java
@Component
public class EventPublisher {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public EventPublisher(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());  // ⚠️ MUST DO
    }
    
    public boolean publish(String queueName, MyEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            Long result = redisTemplate.opsForList().rightPush(queueName, json);
            if (result != null && result > 0) {
                redisTemplate.expire(queueName, 24L, TimeUnit.HOURS);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to publish event", e);
            return false;  // Non-blocking!
        }
    }
}
```

**3. Configuration**
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
```

### Test

```java
@ExtendWith(MockitoExtension.class)
class EventPublisherTest {
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ListOperations<String, String> listOps;
    
    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());  // ⚠️ FOR TESTS TOO
        publisher = new EventPublisher(redisTemplate);
        when(redisTemplate.opsForList()).thenReturn(listOps);
    }
    
    @Test
    void testPublish() {
        when(listOps.rightPush(eq("my-queue"), anyString())).thenReturn(1L);
        when(redisTemplate.expire(eq("my-queue"), eq(24L), eq(TimeUnit.HOURS)))
            .thenReturn(true);
        
        MyEvent event = MyEvent.create("resource-1", "created");
        assertTrue(publisher.publish("my-queue", event));
    }
}
```

### Docker Compose

```yaml
services:
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5
```

### CI/CD

```yaml
# .github/workflows/ci.yml
- name: Build and Test
  run: ./gradlew build  # Includes tests automatically
```

---

## ⚠️ Pièges Critiques

| Piège | Problème | Solution |
|-------|----------|----------|
| **Oublier `jackson-datatype-jsr310`** | `LocalDateTime not supported by default` | Ajouter dependency + `mapper.registerModule(new JavaTimeModule())` |
| **Tester sans JavaTimeModule** | Tests passent localement, échouent CI | Setup identique prod+test |
| **rightPush() retourne Long** | Prévoir boolean mais reçoit Long | Check `result != null && result > 0` |
| **Spring Cloud Stream** | `Could not find dependency` | Utiliser RedisTemplate direct |
| **Redis connection refused** | App timeout, Redis unreachable | Docker hostname: `redis` pas `localhost` |
| **TTL pas appliqué** | Vieux events accumulent | Appeler `expire()` après `rightPush()` |

---

## 🚀 Checklist: Copier-Coller

```bash
# 1. Add to build.gradle
cat >> build.gradle << 'EOF'
implementation 'org.springframework.boot:spring-boot-starter-data-redis:3.2.0'
implementation 'io.lettuce:lettuce-core'
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'
EOF

# 2. Build
./gradlew build

# 3. Create event class (use template above)
# 4. Create publisher service (use template above)
# 5. Add to application.yml (use config above)
# 6. Write test (use test template above)
# 7. Test
./gradlew test

# 8. Docker Compose (use yaml above)
docker-compose up -d

# 9. Inspect queue
redis-cli LLEN my-queue
redis-cli LRANGE my-queue 0 0 | jq .
```

---

## 🎯 Décisions Clés

### Q: RedisTemplate vs Spring Cloud Stream?
**A**: RedisTemplate. Spring Cloud Stream dropped Redis support après 2020.x.

### Q: Lists vs Streams vs Kafka?
**A**: 
- **MVP**: Lists (simple, testable)
- **Production**: Streams (consumer groups, replay)
- **Enterprise**: Kafka (partition, cluster)

Upgrade path: Lists → Streams (same queue name, compatible consumers)

### Q: LocalDateTime vs Instant?
**A**: LocalDateTime. Requires `jackson-datatype-jsr310` but standard industry.

### Q: Blocking vs Non-Blocking?
**A**: Non-blocking. Event fail ≠ transaction fail. Graceful degradation FTW.

---

## 📚 Full Documentation

- **TECHNICAL_CHOICES.md**: All decisions, alternatives, trade-offs
- **EVENT_PUBLISHING.md**: Architecture, implementation, patterns
- **TESTING_EVENT_PUBLISHING.md**: E2E testing guide, Redis CLI, debugging

---

## 💬 FAQ

**Q: Mon event échoue à sérialiser, pourquoi?**
- A: Vous avez une date (LocalDateTime/LocalDate). Ajouter `jackson-datatype-jsr310` + `mapper.registerModule(new JavaTimeModule())`.

**Q: Les tests passent localement mais échouent en CI?**
- A: ObjectMapper différent en test. Vérifier que test setup = prod setup (JavaTimeModule).

**Q: Comment debugger la queue Redis?**
- A: `redis-cli LLEN my-queue` (size), `redis-cli LRANGE my-queue 0 0 | jq .` (inspect).

**Q: Publisher returne false mais pas d'erreur log?**
- A: Vérifier: `rightPush()` retourne-t-il Long > 0? Vérifier `result != null`.

**Q: Events disparaissent de la queue?**
- A: TTL en cours. Vérifier: `redis-cli TTL my-queue`. Ajouter log dans publisher.

**Q: Impossible de se connecter au Redis en Docker?**
- A: Utiliser hostname Docker: `SPRING_REDIS_HOST=redis` pas `localhost`.

---

## 🔗 Ressources Rapides

```bash
# Install redis-cli locally (macOS)
brew install redis

# Check Redis version
redis-cli --version

# Test local Redis
redis-cli ping  # Should return PONG

# Docker Compose logs
docker-compose logs -f redis
docker-compose logs -f app-name

# Kill all Docker containers
docker-compose down -v
```

---

**Prêt à copier-coller?** Voir section "Checklist: Copier-Coller" ci-dessus.

**Besoin de détails?** Voir `TECHNICAL_CHOICES.md` (20 pages, très complet).

**Besoin de tester end-to-end?** Voir `TESTING_EVENT_PUBLISHING.md` (étapes pas à pas).

---

**Status**: ✅ Production-ready template
**Last updated**: December 2024
