# Architecture Decision: Redis Events vs State Storage

**Decision Date**: September 2024  
**Status**: APPROVED for Phase 1-2  
**Review Date**: Q1 2025

---

## The Question

> Les microservices vont manipuler les états, pas les événements. Redis ne possède pas l'équivalent des KTable de Kafka. Est-ce une bonne pratique d'utiliser Redis à la fois comme broker d'événements ET comme stockage des états? Ou faut-il mieux utiliser des bases dédiées?

---

## 🎯 Decision

**Use a Hybrid Pattern:**
- **Events**: Redis (temporary, real-time)
- **State**: Dedicated PostgreSQL per service (permanent, auditable)
- **Idempotency**: Idempotent event handlers (safe retries)

---

## 📊 Decision Matrix

| Aspect | Redis Only | Hybrid (Rec) | Kafka |
|--------|-----------|-------------|-------|
| **Events Storage** | ✅ | ✅ | ✅ |
| **State Storage** | ❌ | ✅ | ✅ |
| **Audit Trail** | ❌ | ✅ | ✅ |
| **Replay Events** | ❌ | ❌ | ✅ |
| **Ops Complexity** | ✅ Low | ✅ Low | ❌ High |
| **Cost** | ✅ Low | ✅ Low | ⚠️ Med |
| **Latency** | ✅ <1ms | ⚠️ 5ms | ❌ 100ms |
| **Best for MVP** | ✅ Yes | ✅ Yes | ❌ No |
| **Best for Scale** | ❌ | ⚠️ | ✅ |

**Recommendation**: ✅ **Hybrid (Phase 1-2)**

---

## 🏗️ Architecture

### Current (Phase 1 - NOW)
```
CarRentalService ──→ Redis Events Queue ──→ (No consumers yet)
```

### Recommended (Phase 2 - 3-6 months)
```
CarRentalService 
    ↓
Redis Events: auction:events:queue (TTL: 24h)
    ↓
┌───────────────────────────────────────┐
│ Rental Service              │ (Event listener)
│ Insurance Service          │ (Event listener)
│ Fuel Service               │ (Event listener)
└───────────────────────────────────────┘
    ↓
PostgreSQL per service
│ rental_state (permanent)
│ insurance_state (permanent)
│ fuel_state (permanent)
```

---

## ✅ Why This Pattern Works

### For MVP (Now)
- ✅ Simple (Redis only)
- ✅ Fast (sub-millisecond)
- ✅ Proven (works well at 10-100 QPS)
- ✅ Easy to extend (add services later)

### For Multi-Service (Phase 2)
- ✅ Each service autonomous (own DB)
- ✅ Events = notifications (not state source)
- ✅ State = permanent (auditable)
- ✅ Idempotent handlers (safe retry)
- ✅ No Kafka complexity (yet)

### For Enterprise (Phase 3+)
- If scale requires: Kafka upgrade path clear
- Not locked into Redis
- Lessons learned about event handling
- Team matured on patterns

---

## 🚫 What NOT to Do

### ❌ Anti-Pattern 1: Redis as Permanent State
```java
// ❌ WRONG
redisTemplate.set("rental:123", rentalState);  // No TTL
// After 30 days: Cannot audit, cannot query history

// ✅ RIGHT
postgresqlDB.save(rentalState);  // Permanent
```

### ❌ Anti-Pattern 2: Shared Redis Keys
```java
// ❌ WRONG
redisTemplate.set("state:123", event);  // Overwritten!

// ✅ RIGHT
redisTemplate.set("rental:state:123", rentalState);
redisTemplate.set("insurance:state:123", insuranceState);
```

### ❌ Anti-Pattern 3: No Idempotency
```java
// ❌ WRONG
eventQueue.consume().forEach(e -> process(e));  // Duplicates = Boom!

// ✅ RIGHT
if (!alreadyProcessed(e.eventId)) {
    updateState(e);
    markAsProcessed(e.eventId);
}
```

---

## 🔄 Implementation Roadmap

### Phase 1: MVP (Week 1-2) ✅ DONE
```
Duration: 1-2 weeks
Effort: Low
Scope:
  ├─ CarRentalService publishes AuctionWon event
  ├─ Events go to Redis queue (audit:events:queue)
  ├─ TTL: 24h (temporary)
  └─ No consumers yet (waiting for Phase 2)

Status: ✅ COMPLETE & TESTED
```

### Phase 2: Multi-Service State (Month 3-4) ⏳ PLANNED
```
Duration: 4 weeks
Effort: Medium
Scope:
  ├─ RentalService
  │  ├─ Listen to Redis events
  │  ├─ Update PostgreSQL rental_state
  │  ├─ Idempotent processing
  │  └─ REST API to query state
  │
  ├─ InsuranceService (same pattern)
  │  ├─ Listen to Redis events
  │  ├─ Update PostgreSQL insurance_state
  │  └─ REST API
  │
  └─ FuelService (same pattern)
     ├─ Listen to Redis events
     ├─ Update PostgreSQL fuel_state
     └─ REST API

Code:
  ├─ EventListener component (idempotent)
  ├─ State entity + repository
  ├─ ProcessedEvent table (for idempotency)
  └─ Unit tests (6+ tests minimum)

Config:
  ├─ PostgreSQL connection per service
  ├─ Spring Data JPA + Hibernat
  ├─ Application-{dev,test,prod}.yml
  └─ Flyway migrations for schema
```

### Phase 3: Kafka (Month 6+) ❓ OPTIONAL
```
Trigger: If ANY of these true:
  ├─ >1000 QPS
  ├─ Regulatory audit (7+ years history)
  ├─ Exactly-once semantics required
  ├─ Multi-site failover needed
  ├─ Event replay demanded
  └─ Team has Kafka expertise

If none true: Stay with hybrid!

Migration:
  ├─ Redis → Kafka topics (1-1 mapping)
  ├─ Add Kafka Streams topology
  ├─ KTable for materialized state
  ├─ Interactive queries service
  └─ Database state becomes cache
```

---

## 💡 Why NOT Go Directly to Kafka?

| Reason | Impact |
|--------|--------|
| Overkill for MVP | Adds 3 weeks of work |
| Kafka learning curve | Team needs training |
| Operational complexity | Need ZK, multiple brokers, monitoring |
| Cost overhead | $$$$ for managed service or self-hosted |
| Latency impact | 100ms vs 1ms (10x slower) |
| Not needed yet | MVP works fine with Redis |

**Better**: Grow into Kafka naturally when scale demands it.

---

## ✅ Verification Checklist

Before moving to Phase 2, verify:

- [ ] Phase 1 working (events flowing to Redis)
- [ ] All 6 unit tests passing
- [ ] CI/CD pipeline green
- [ ] Load tested at 100 QPS
- [ ] Docker Compose works locally
- [ ] Team understands non-blocking pattern
- [ ] Team understands idempotency pattern
- [ ] All code reviewed
- [ ] Documentation complete (this repo)

---

## 📚 References

**Detailed Analysis:**
- `MICROSERVICES_STATE_MANAGEMENT.md`: Complete trade-offs (KTable, Kafka, Redis)
- `HYBRID_STATE_PATTERNS.md`: Code patterns for Phase 2

**Current Implementation:**
- `TECHNICAL_CHOICES.md`: Phase 1 decisions
- `QUICK_START_REDIS_EVENTS.md`: Getting started
- `EVENT_PUBLISHING.md`: Implementation guide

---

## 🎓 Key Learnings

### What Redis DOES Well:
✅ Real-time event transport (temporary)
✅ Sub-millisecond latency
✅ Fire-and-forget pub/sub
✅ Simple mental model
✅ Easy to operate

### What Redis DOESN'T Do Well:
❌ Long-term state storage (no versioning)
❌ Audit trail (TTL expires data)
❌ Ordered delivery to multiple consumers
❌ Exactly-once semantics
❌ Replay capability

### The Solution:
✨ **Use the right tool for each job:**
- Redis: Event notification (temporary)
- PostgreSQL: State persistence (permanent)
- Kafka: (Later) Event log for replay/audit

---

## 🏁 Recommendation Summary

| When | Use | Why |
|------|-----|-----|
| **NOW** | Redis events | MVP, simple, fast |
| **In 3-6 mo** | Add PostgreSQL state | Permanent, auditable, scalable |
| **In 6+ mo** | Consider Kafka | If scale demands it |

**Action**: Proceed with Phase 1 (DONE). Plan Phase 2 for Q4 2024.

---

**Approval:**
- Architecture Review: ✅ Approved
- Security Review: ✅ OK (PostgreSQL encrypted)
- Operations Review: ✅ OK (standard stack)
- Finance Review: ✅ OK (low cost)

**Next Review**: Q1 2025 (after Phase 2 implementation)

---

## Questions?

See:
1. `MICROSERVICES_STATE_MANAGEMENT.md` - Detailed analysis
2. `HYBRID_STATE_PATTERNS.md` - Code examples
3. `TECHNICAL_CHOICES.md` - Phase 1 decisions
