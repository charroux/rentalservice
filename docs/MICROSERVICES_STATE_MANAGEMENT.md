# État des Microservices vs Événements: Redis, KTable, et Storage Pattern

**Date**: September 2024  
**Audience**: Architects, Backend Team Leads  
**Topic**: State management for event-driven microservices

---

## 🔍 Question Centrale

> Les microservices vont manipuler les états, pas les événements. Redis n'a pas l'équivalent des KTable de Kafka. Est-ce une bonne pratique d'utiliser Redis pour BOTH événements ET états? Ou faut-il bases dédiées?

**Réponse courte**: ❌ **Non, Redis seul n'est pas suffisant**. Il faut un pattern hybride.

---

## 📊 Kafka KTable vs Redis: Comparaison Technique

### Qu'est-ce qu'une Kafka KTable?

```
Kafka KTable = "Change Data Capture + State Store"

┌─────────────────────────────────────────────────────────┐
│ TOPIC: auction-events                                   │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Event: AuctionWon(id=123, status=CLOSED)           │ │
│ │ Event: AuctionWon(id=123, status=WINNER_PAID)      │ │ ← Updates to same key
│ └─────────────────────────────────────────────────────┘ │
│                          ↓                               │
│        Kafka Streams Materialized View (KTable)         │
│        ┌─────────────────────────────────────────────┐  │
│        │ Key=123 → {status: WINNER_PAID, ...}       │  │
│        │ (Latest state for each key, deduplicated)  │  │
│        └─────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

**Propriétés clés KTable:**
1. **Deduplication**: Multiples events pour same key → 1 état final
2. **Ordering**: Events maintenus dans order par partition
3. **Replay**: Peut rejouer events → recalculer état
4. **State Store**: Peut être in-memory, RocksDB, ou custom
5. **Scalability**: Partitioned par key (parallelizable)
6. **Fault tolerance**: Changelog topic pour recovery

---

## 🚨 Limitations de Redis pour KTable Pattern

### ❌ Problem #1: Pas de Versioning/History

```yaml
# Redis approach:
SET auction:123 '{"status": "WINNER_PAID", "finalPrice": 5000}'

# ⚠️ Previous state LOST! Can't:
# - Audit trail
# - Replay events
# - Recover from bugs
# - Track state transitions

# Kafka KTable:
TOPIC: auction-events partition key=123
  Event 1: {status: OPEN}
  Event 2: {status: CLOSED}
  Event 3: {status: WINNER_PAID}
# ✅ All history preserved!
```

### ❌ Problem #2: Pas de Ordering Guarantee

```java
// Redis scenario:
subscriber1.process(event);  // Thread 1
subscriber2.process(event);  // Thread 2
subscriber3.process(event);  // Thread 3

// ⚠️ 3 threads, same event-key, different order!
// State can diverge across subscribers!

// Kafka KTable:
// ✅ ALL subscribers see SAME ORDER per partition key
```

### ❌ Problem #3: No Built-in State Recovery

```yaml
# Redis failure scenario:
# 1. Events published to Redis: [Event1, Event2, Event3]
# 2. Microservice crashes processing Event2
# 3. Redis memory lost (or only AOF/RDB persists)
# ⚠️ Event1 = processed, Event2 = ?, Event3 = not seen
# ⚠️ State inconsistent, cannot replay!

# Kafka KTable:
# ✅ Consumer group offset tracked
# ✅ Changelog topic persists state
# ✅ Can replay from offset
```

### ❌ Problem #4: No Partition Guarantee

```java
// Redis LIST approach:
List<Event> events = redis.LRANGE("auction:events", 0, 100);
// ⚠️ Same key, multiple consumers
// One gets Event1-50, another gets Event51-100
// But which consumer owns which key for state?
// No answer!

// Kafka approach:
// ✅ Partition 0: keys [0-49999] → Consumer A
// ✅ Partition 1: keys [50000-99999] → Consumer B
// ✅ Guarantee: Each key processed by ONE consumer
```

### ❌ Problem #5: Temporary Storage Only

```
Redis Default Behavior:
┌─────────────────────────────────────────┐
│ Events pushed: [Event1, Event2, Event3] │
│ Stored 24 hours (default TTL)           │
│ After 24h: Data GONE                    │
│ Cannot audit, cannot track state        │
└─────────────────────────────────────────┘

Kafka:
┌─────────────────────────────────────────┐
│ Events published (partitioned)          │
│ Retention: Days/Weeks/Years (tunable)   │
│ Changelog topic: Audit trail            │
│ State Store: Point-in-time snapshots    │
└─────────────────────────────────────────┘
```

---

## ✅ Redis Strengths (Don't Waste Them!)

Redis EXCELS at:
- ✅ **Real-time cache**: Sub-millisecond access
- ✅ **Session storage**: Temporary state (minutes/hours)
- ✅ **Queue operations**: Fast FIFO/LIFO
- ✅ **Leaderboards**: Sorted sets
- ✅ **Rate limiting**: Counters + TTL
- ✅ **Pub/Sub**: Real-time notifications
- ✅ **Temporary broadcasts**: Fire-and-forget

Redis FAILS at:
- ❌ **Event audit trail**: No version history
- ❌ **Stateful processing**: No ordering guarantee
- ❌ **Durable replay**: Data expires
- ❌ **Multiple subscribers**: No consumer groups
- ❌ **Exactly-once semantics**: No offset tracking

---

## 🏗️ Recommended Architecture: Hybrid Pattern

### Pattern #1: Redis Events + Dedicated State Store

```
┌──────────────────────────────────────────────────────────────────┐
│                    CAR RENTAL SERVICE                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  REST API ──┐                                                    │
│             └──→ AuctionService                                   │
│                  └──→ publishAuctionWon()                         │
│                                                                  │
└────────────────────────────────────────────────────────────────┬─┘
                                                                 │
                        ┌────────────────────────────────────────┘
                        │
                        ▼
        ┌───────────────────────────────────────┐
        │   Redis Event Queue                   │
        │   "auction:events:queue"              │
        │   (TTL: 24h, temporary)               │
        │   [AuctionWon1, AuctionWon2, ...]    │
        └───────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
   ┌─────────┐    ┌─────────┐    ┌─────────┐
   │Partner  │    │Partner  │    │Partner  │
   │Service  │    │Service  │    │Service  │
   │Rental   │    │Insurance│    │Fuel     │
   └────┬────┘    └────┬────┘    └────┬────┘
        │              │              │
        ▼              ▼              ▼
   ┌──────────────────────────────────────────────────┐
   │   Dedicated State Stores (PostgreSQL/MongoDB)   │
   ├──────────────────────────────────────────────────┤
   │ RentalService:                                   │
   │   rental:123 → {status: CONFIRMED, ...}         │
   │   rental:124 → {status: PENDING, ...}           │
   │                                                  │
   │ InsuranceService:                                │
   │   insurance:123 → {coverage: ACTIVE, ...}       │
   │                                                  │
   │ FuelService:                                     │
   │   fuel:123 → {discount: 15%, ...}               │
   └──────────────────────────────────────────────────┘
        ▲              ▲              ▲
        │              │              │
        └──────────────┼──────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        ▼                             ▼
   Event Consumed                State Updated
   (Idempotent)                 (Dedicated DB)
```

**Avantages:**
✅ Events: Temporaires en Redis (performance)
✅ État: Permanent dans DB dédiée (audit trail)
✅ Chaque service = sa DB (autonomie, scaling)
✅ Idempotent processing = OK si event redélivré

**Coût:**
⚠️ 2 systèmes = 2 points of failure
⚠️ Eventual consistency entre Redis et DBs
⚠️ Need idempotent event handlers

---

### Pattern #2: Kafka for Events + Kafka Streams KTable for State

```
┌──────────────────────────────────────────────────────────────────┐
│                    CAR RENTAL SERVICE                            │
└────────────────────────────────────────────────────────────────┬─┘
                                                                 │
                        ┌────────────────────────────────────────┘
                        │
                        ▼
        ┌───────────────────────────────────┐
        │   Kafka Topic: auction-events     │
        │   (Persistent, partitioned)       │
        │   Replication: 3x redundancy      │
        │   Retention: 30 days              │
        └───────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
   ┌─────────┐    ┌─────────┐    ┌─────────┐
   │Kafka    │    │Kafka    │    │Kafka    │
   │Streams  │    │Streams  │    │Streams  │
   │Partner1 │    │Partner2 │    │Partner3 │
   └────┬────┘    └────┬────┘    └────┬────┘
        │              │              │
        ▼              ▼              ▼
   ┌──────────────────────────────────────────────────┐
   │   Kafka KTables (Materialized State Stores)     │
   ├──────────────────────────────────────────────────┤
   │ RentalStateStore: (Changelog topic)              │
   │   123 → {status: CONFIRMED, finalPrice: 5000}   │
   │   124 → {status: PENDING, finalPrice: 4500}     │
   │                                                  │
   │ InsuranceStateStore:                             │
   │   123 → {coverage: ACTIVE, premium: 200}        │
   │                                                  │
   │ FuelStateStore:                                  │
   │   123 → {discount: 15%, perGallon: 0.50}        │
   └──────────────────────────────────────────────────┘
        ▲              ▲              ▲
        │              │              │
   Interactive Queries (REST API on KTable)
```

**Avantages:**
✅ Events: Permanent, versioned, replay-able
✅ État: Derived from events (single source of truth)
✅ Ordering: Guaranteed per partition key
✅ Scalability: Partitioned streams
✅ Recovery: Replayed automatically
✅ Audit trail: Full event history

**Coût:**
⚠️ Kafka complexity (need expertise)
⚠️ More infrastructure (ZK, brokers, cluster)
⚠️ Higher latency than Redis (100ms vs 1ms)
⚠️ Need Interactive Queries for state access

---

### Pattern #3: Hybrid - Redis (Real-time) + Kafka (Audit) + DB (Persistent)

```
YOUR CURRENT SETUP EVOLVED:

┌──────────────────────────────────────────────────────────────────┐
│                    CAR RENTAL SERVICE                            │
└──────────────────────────────┬──────────────────────────────────┬┘
                               │                                  │
                        ┌──────▼────────┐                         │
                        │ PostgreSQL DB │                         │
                        │ (Durable)     │                         │
                        └──────────────┘                         │
                               ▲                                  │
                               │                                  │
                ┌──────────────┴──────────────┐                  │
                │                             │                  │
                ▼                             ▼                  │
        ┌──────────────────┐        ┌──────────────────┐         │
        │ Redis: Events    │        │ PostgreSQL: State│         │
        │ (TTL 24h)        │        │ (Permanent)      │         │
        │ -Fast pub/sub    │        │ -Full audit trail│         │
        │ -Real-time       │        │ -Analytics ready │         │
        └──────┬───────────┘        └────────┬─────────┘         │
               │                             │                  │
               └─────────────┬───────────────┘                  │
                             │                                  │
        ┌────────────────────▼──────────────────────┐           │
        │ Kafka Topic (Optional Layer)              │           │
        │ -Backup event stream                      │           │
        │ -Cross-datacenter replication             │           │
        │ -Can be disabled if costs high            │           │
        └───────────────────┬──────────────────────┘           │
                            │                                  │
        ┌───────────────────┼───────────────┐                 │
        │                   │               │                 │
        ▼                   ▼               ▼                 │
   ┌─────────┐         ┌─────────┐    ┌─────────┐           │
   │Partner  │         │Partner  │    │Partner  │           │
   │Service  │         │Service  │    │Service  │◄──────────┘
   │Rental   │         │Insurance│    │Fuel     │   (Subscribe)
   └─────────┘         └─────────┘    └─────────┘
```

---

## 🎯 Decision Matrix: When to Use What

```
┌────────────────────────────────────────────────────────────────────┐
│                     WHEN TO USE WHAT?                              │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│ 🔵 Redis Only (Simple cases):                                      │
│   ├─ MVP, <10 QPS, single microservice                            │
│   ├─ Events don't need audit trail                                │
│   ├─ No multi-service state coordination                          │
│   └─ Time to market > audit trail requirement                     │
│   ✅ Example: Your current Car Rental (MVP)                       │
│                                                                    │
│ 🟢 Redis + Dedicated DB (Recommended for Most):                   │
│   ├─ Multiple microservices with independent state                │
│   ├─ Need permanent state storage                                 │
│   ├─ Events are notifications, not full history                   │
│   ├─ Each service manages own state DB                            │
│   └─ Good audit trail (query state table, not replayed events)    │
│   ✅ Your likely migration target (phase 2-3)                    │
│                                                                    │
│ 🟠 Kafka + KTable (Complex cases):                                 │
│   ├─ 100+ QPS, complex event coordination                         │
│   ├─ Need full event replay capability                            │
│   ├─ Exactly-once semantics required                              │
│   ├─ Cross-datacenter event replication                           │
│   ├─ Interactive queries on state                                 │
│   └─ Team has Kafka expertise                                     │
│   ✅ Enterprise scale (phase 4+)                                  │
│                                                                    │
│ 🔴 Avoid:                                                          │
│   ├─ Redis Lists as primary state store                           │
│   ├─ Expecting Redis TTL = audit trail                            │
│   ├─ Multiple services sharing Redis keys (conflicts)             │
│   ├─ Kafka without proper consumer group management               │
│   └─ Kafka without monitoring (blackbox)                          │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Your Situation: Car Rental Service

### Current State (TODAY)
```
Redis Events: ✅ Good choice for MVP
├─ Single service (Car Rental)
├─ No distributed state coordination
├─ Events are notifications
└─ 24h TTL acceptable
```

### Phase 2: Multi-Service State (IN 3-6 MONTHS)
```
RECOMMENDED PATTERN:

┌─────────────────────────────────────────────────────────┐
│ CAR RENTAL SERVICE                                      │
│ ├─ Events → Redis (fire-and-forget)                    │
│ └─ State → PostgreSQL (persistent)                      │
│    auction:123 table rows                               │
│    rental:456 table rows                                │
└─────────────────────────────────────────────────────────┘
         ↓ Events via Redis
         │
    ┌────┴────┬──────────┬──────────┐
    ↓         ↓          ↓          ↓
┌──────────┐ ┌──────────┐ ┌──────────┐
│ RENTAL   │ │INSURANCE │ │ FUEL     │
│ SERVICE  │ │ SERVICE  │ │ SERVICE  │
├──────────┤ ├──────────┤ ├──────────┤
│ State DB │ │ State DB │ │ State DB │
│(MySQL)   │ │(MySQL)   │ │(MySQL)   │
└──────────┘ └──────────┘ └──────────┘
```

**Why this pattern?**
- Each service = autonomous (can scale independently)
- Events = light (Redis, TTL OK)
- State = safe (dedicated DB, permanent)
- Idempotent processing = retries safe
- No Kafka complexity (yet!)

---

## 🔴 Anti-Patterns to AVOID

### ❌ Anti-Pattern #1: Shared Redis State Keys
```java
// ❌ WRONG - Multiple services writing same key
redisTemplate.set("auction:123", eventData);  // Rental Service
redisTemplate.set("auction:123", stateData);  // Insurance Service
// ⚠️ Overwrites, no versioning, race conditions!

// ✅ RIGHT - Each service has own key prefix
redisTemplate.set("rental:state:auction:123", rentalState);
redisTemplate.set("insurance:state:auction:123", insuranceState);
// Still temporary, but isolated per service
```

### ❌ Anti-Pattern #2: Expecting Redis as Audit Trail
```java
// ❌ WRONG
List<Event> history = redis.LRANGE("audit:events", 0, -1);
// After 24h TTL: history GONE!
// Not a real audit trail!

// ✅ RIGHT - Use PostgreSQL for audit
SELECT * FROM events WHERE created_at > NOW() - INTERVAL '1 year';
// Permanent, queryable, auditable
```

### ❌ Anti-Pattern #3: No Idempotent Event Processing
```java
// ❌ WRONG - Redis as state = processing must be exactly-once
// But Redis Lists don't guarantee this!

// ✅ RIGHT - Process idempotently
public void handleAuctionWon(AuctionWonEvent event) {
    // Check if already processed
    if (eventStore.exists(event.eventId)) {
        return;  // Already handled
    }
    
    // Update permanent state
    updateAuctionState(event);
    
    // Mark as processed
    eventStore.mark(event.eventId);
}
// Now safe to retry event!
```

---

## 🚀 Implementation Roadmap

### Phase 1: MVP (NOW) - Redis Events Only ✅
```
Effort: Done
Duration: 1 week
Scope: Single service, temporary events
Redis:
  └─ auction:events:queue [Event1, Event2, ...]
     TTL: 24h
Status: ✅ DEPLOYED
```

### Phase 2: Multi-Service State (In 3 months) ⏳
```
Effort: Medium
Duration: 4 weeks
Scope: Partner services with persistent state
Add:
  ├─ PostgreSQL DB per service
  ├─ Event → DB mapping (idempotent)
  └─ Separate state schema per service
Redis:
  └─ Still events (unchanged)
New Code:
  ├─ RentalStateStore (PostgreSQL)
  ├─ InsuranceStateStore (PostgreSQL)
  └─ FuelStateStore (PostgreSQL)
```

### Phase 3: Kafka (Optional, 6+ months)
```
Effort: High
Duration: 8+ weeks
Scope: Enterprise-scale event processing
Add:
  ├─ Kafka cluster (3 brokers minimum)
  ├─ Kafka Streams topology
  ├─ KTable materialized state stores
  └─ Interactive queries service
Migrate:
  └─ Redis events → Kafka topics
Benefits:
  ├─ Full replay capability
  ├─ Multi-site failover
  └─ Exactly-once semantics
```

---

## 📊 Storage Consideration: Permanent vs Temporary

### Redis Default: Temporary (TTL-based)
```yaml
Strategy: Append-Only File (AOF)
├─ Every write: fsync to disk (slow)
├─ Durability: Order of seconds
├─ Recovery: Replay AOF on restart
└─ TTL: Data expires after set time
   ├─ auction:events: TTL=24h
   ├─ After 24h: key DELETED
   └─ Cannot audit beyond 24h

Usecase: Real-time notifications
├─ Rental Service publishes event
├─ Partner reads within 24h
└─ Event expires (no need to keep forever)
```

### PostgreSQL: Permanent (Audit-Ready)
```yaml
Strategy: WAL (Write-Ahead Log) + Snapshots
├─ Every transaction: WAL fsync (fast)
├─ Durability: Order of microseconds
├─ Recovery: Replay WAL from backup
└─ Retention: Indefinite (design dependent)
   ├─ Auction state table: retention=FOREVER
   ├─ Audit log table: retention=7 years
   └─ Can query any point in time

Usecase: Permanent state + audit trail
├─ RentalService stores rental state
├─ InsuranceService stores coverage state
└─ Auditors query 2023 transactions (7 years later!)
```

### Hybrid Approach
```yaml
Redis:
├─ Purpose: Event transportation layer
├─ Storage: Temporary (TTL: 24-72h)
├─ Goal: Real-time pub/sub
└─ If lost: Events can be retried from source

PostgreSQL:
├─ Purpose: State persistence + audit trail
├─ Storage: Permanent (7+ years)
├─ Goal: Durable state + auditability
└─ If lost: Data recovery required (backups)

Kafka (future):
├─ Purpose: Event log + recovery
├─ Storage: Permanent (30 days-years)
├─ Goal: Replay capability + multi-site replication
└─ If lost: Replicated to 3 brokers (rare)
```

---

## ✅ Recommendations for Your Architecture

### NOW (Phase 1: MVP) - Keep as is
```
✅ Redis Events: auction:events:queue
   └─ TTL: 24h
   └─ Good enough for single service

⚠️ Note: Not suitable for multi-service state yet
```

### 3 MONTHS (Phase 2: Multi-Service)
```
✅ Redis Events: Keep as transport layer
   └─ Real-time notifications

✅ Add Dedicated State DBs:
   ├─ RentalService: MySQL rental_state table
   ├─ InsuranceService: MySQL insurance_state table
   └─ FuelService: MySQL fuel_state table

✅ Pattern: Idempotent Event Handlers
   └─ Event received → Check if processed
   └─ If new: Update state DB → Mark as processed
   └─ If duplicate: Skip (safe)
```

### 6+ MONTHS (Phase 3: Enterprise)
```
❓ Kafka only if:
   ├─ >1000 QPS
   ├─ Need full replay (regulatory)
   ├─ Need exact ordering (order system)
   ├─ Have Kafka expertise
   └─ Justify infrastructure cost

Otherwise: Redis + PostgreSQL sufficient
```

---

## 🎓 Summary Table

```
┌──────────────────┬──────────────┬──────────────┬──────────────┐
│ Layer            │ Redis        │ PostgreSQL   │ Kafka        │
├──────────────────┼──────────────┼──────────────┼──────────────┤
│ Events (MVP)     │ ✅ Good      │ ❌ Slow      │ ⚠️ Overkill  │
│ State (MVP)      │ ⚠️ Risky     │ ✅ Good      │ N/A          │
│                  │              │              │              │
│ Events (Scale)   │ ⚠️ Limited   │ ❌ Slow      │ ✅ Good      │
│ State (Scale)    │ ❌ Risky     │ ✅ Good      │ ✅ Best      │
│                  │              │              │              │
│ Ordering Guar.   │ ❌ No        │ ⚠️ Via PK    │ ✅ Yes       │
│ Replay Events    │ ❌ No (TTL)  │ ⚠️ Via logs  │ ✅ Yes       │
│ Audit Trail      │ ❌ No (TTL)  │ ✅ Yes       │ ✅ Yes       │
│ Exactly-once     │ ❌ No        │ ⚠️ Via txn   │ ✅ Yes       │
│                  │              │              │              │
│ Ops Complexity   │ ✅ Low       │ ✅ Medium    │ ❌ High      │
│ Latency          │ ✅ <1ms      │ ⚠️ 5-10ms    │ ❌ 100ms     │
│ Cost             │ ✅ Low       │ ✅ Low       │ ⚠️ Medium    │
└──────────────────┴──────────────┴──────────────┴──────────────┘
```

---

## 🏁 Conclusion

### Does Redis Replace Kafka KTable?
**No.** Redis and Kafka KTable solve different problems:
- Redis = Real-time transport (temporary)
- KTable = Event log + state derivation (permanent)

### Should you use Redis for BOTH events AND state?
**No.** Better split:
- Events → Redis (temporary, fast)
- State → PostgreSQL (permanent, auditable)

### Best practice for your partner services?
**Hybrid approach** (most common):
```
Events: Redis (for real-time)
State: Dedicated DB per service
Idempotency: Mark processed events
Audit: Query state table (not replay events)
```

### When to switch to Kafka?
Only if:
✅ >1000 QPS
✅ Regulatory audit trail (7+ years)
✅ Exactly-once semantics required
✅ Multi-site failover needed
✅ Team has Kafka expertise

Otherwise: Redis + PostgreSQL is production-ready! 🚀

---

## 📚 Related Documentation

See also:
- `TECHNICAL_CHOICES.md`: Architecture decisions
- `QUICK_START_REDIS_EVENTS.md`: Getting started
- `EVENT_PUBLISHING.md`: Implementation patterns
