# Phase 1 Deployment Guide

## 🚀 Quick Start

### Prerequisites
- Docker Desktop running on macOS (required for docker-compose)
- `redis-cli` installed: `brew install redis`
- `psql` installed: `brew install postgresql`
- Phase 1 code compiled: ✅ BUILD SUCCESSFUL

### One-Command Deployment

```bash
cd /Users/benoitcharroux/Documents/rentalservice

# Start Docker Desktop from Applications folder or use:
# open -a Docker

# Then:
docker-compose -f docker-compose.dev.yml --env-file .env.dev up -d

# Wait 15-20 seconds for services to be healthy:
docker-compose -f docker-compose.dev.yml ps
```

**Expected Output (all services should show "Up"):**
```
NAME                 STATUS
car-rental-redis     Up (healthy)
car-rental-postgres  Up (healthy)
auction-service      Up (healthy)
car-rental-app       Up (healthy)
frontend-angular     Up (healthy)
```

---

## 📋 Deployment Architecture

### Services Deployed

```
┌─────────────────────────────────────────────────────────────┐
│                     Phase 1 Deployment                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  📝 car-rental-app (Spring Boot 3.2 + Java 21)            │
│     ├─ Port: 8080 (API)                                   │
│     ├─ Health: /actuator/health                           │
│     ├─ Publishers: AuctionEventPublisher                  │
│     └─ Consumers: AuctionEventConsumer @Scheduled         │
│                                                             │
│  🗄️  car-rental-postgres (PostgreSQL 15)                   │
│     ├─ Port: 5432                                         │
│     ├─ Database: dbcar                                    │
│     ├─ Credentials: dbuser / dbpass                       │
│     └─ Tables:                                             │
│        ├─ rental, car, auction (write model)              │
│        └─ processed_events (idempotence guarantee)        │
│                                                             │
│  ⚡ car-rental-redis (Redis 7-alpine)                      │
│     ├─ Port: 6379                                         │
│     ├─ Queue: auction:events:queue (LIST)                │
│     ├─ Memory: 512MB with LRU eviction                   │
│     └─ Persistence: AOF (Append-Only File)              │
│                                                             │
│  🎯 auction-service (gRPC Server)                          │
│     └─ Port: 9090 (gRPC protocol)                         │
│                                                             │
│  🎨 frontend-angular                                        │
│     └─ Port: 3000 (development server)                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔍 Deployment Steps

### Step 1: Start Docker Daemon
```bash
# macOS only: Open Docker Desktop from Applications
open -a Docker

# Verify Docker is running
docker ps
```

### Step 2: Deploy Services
```bash
cd /Users/benoitcharroux/Documents/rentalservice

# Stop any existing containers
docker-compose -f docker-compose.dev.yml --env-file .env.dev down

# Start all services
docker-compose -f docker-compose.dev.yml --env-file .env.dev up -d

# Wait for health checks to pass (15-20 seconds)
sleep 20

# Check status
docker-compose -f docker-compose.dev.yml ps
```

### Step 3: Verify Database Migration
```bash
# Connect to PostgreSQL
psql -h localhost -U dbuser -d dbcar

# List tables (should include: processed_events, rental, car, auction)
\dt

# Check processed_events schema
\d processed_events

# Expected columns:
#  id (BIGSERIAL) - Primary key
#  event_id (VARCHAR 36) - Unique identifier from AuctionWonEvent
#  consumer_name (VARCHAR 100) - Which consumer processed (auction-event-consumer)
#  event_type (VARCHAR 100) - Type of event (AuctionWonEvent)
#  processed_at (TIMESTAMP) - When processed
#  retry_count (INTEGER) - For Phase 2
#  error_message (VARCHAR 1000) - For Phase 2
#  created_at (TIMESTAMP) - When record created
# Unique constraint: (event_id, consumer_name)

# Exit psql
\q
```

### Step 4: Run Deployment Test
```bash
# From project root:
./PHASE1_DEPLOYMENT_TEST.sh

# Or with custom parameters:
./PHASE1_DEPLOYMENT_TEST.sh localhost 6379 localhost 5432 dbcar dbuser dbpass http://localhost:8080
```

---

## 🧪 Manual Testing (Without Script)

### Test 1: Verify Redis Queue
```bash
# Check if queue exists
redis-cli LLEN auction:events:queue

# Should return: (integer) 0 (empty at start)
```

### Test 2: Publish Test Event to Redis
```bash
# Create test event JSON
cat > /tmp/test-event.json <<'EOF'
{
  "eventId": "test-auction-won-$(date +%s%N)",
  "eventType": "AuctionWonEvent",
  "auctionId": "auction-123",
  "winnerUserId": "user-456",
  "winningBid": 5000.00,
  "vehicleId": "vehicle-789",
  "timestamp": "2024-10-01T12:34:56Z"
}
EOF

# Publish to Redis LIST
redis-cli LPUSH auction:events:queue "$(cat /tmp/test-event.json)"

# Verify it's in queue
redis-cli LRANGE auction:events:queue 0 -1
```

### Test 3: Monitor Event Consumption
```bash
# Watch carRental logs
docker logs -f car-rental-app

# You should see messages like:
#   2024-10-01 12:34:56 INFO AuctionEventConsumer: Consuming auction events...
#   2024-10-01 12:34:57 INFO AuctionEventConsumer: Processing event: test-auction-won-...
#   2024-10-01 12:34:57 INFO AuctionEventConsumer: ✅ Event marked as processed
```

### Test 4: Verify Event in Database
```bash
psql -h localhost -U dbuser -d dbcar

# Check processed_events table
SELECT * FROM processed_events ORDER BY created_at DESC LIMIT 5;

# Expected output:
#  id | event_id              | consumer_name          | event_type      | processed_at | ...
#  1  | test-auction-won-1... | auction-event-consumer | AuctionWonEvent | 2024-10-01   | ...
```

### Test 5: Verify Idempotence
```bash
# Get last event_id from database (use psql from above)
SELECT event_id FROM processed_events ORDER BY created_at DESC LIMIT 1;

# Copy the event_id (e.g., "test-auction-won-...")
# Create new event with SAME event_id

cat > /tmp/duplicate-event.json <<EOF
{
  "eventId": "test-auction-won-...",
  "eventType": "AuctionWonEvent",
  "auctionId": "auction-123",
  "winnerUserId": "user-456",
  "winningBid": 5000.00,
  "vehicleId": "vehicle-789",
  "timestamp": "2024-10-01T12:34:56Z"
}
EOF

# Publish duplicate
redis-cli LPUSH auction:events:queue "$(cat /tmp/duplicate-event.json)"

# Wait 5 seconds for consumer to poll
sleep 5

# Check database - should still have only ONE record with this event_id
psql -h localhost -U dbuser -d dbcar -c "SELECT COUNT(*) FROM processed_events WHERE event_id = 'test-auction-won-...' AND consumer_name = 'auction-event-consumer';"

# Expected: (1 row) count = 1 (NOT 2, proving idempotence works)
```

---

## 📊 Monitoring & Debugging

### View Container Logs
```bash
# CarRental app logs
docker logs -f car-rental-app

# PostgreSQL logs
docker logs car-rental-postgres

# Redis logs
docker logs car-rental-redis

# All services
docker-compose -f docker-compose.dev.yml logs -f
```

### Check Redis Queue
```bash
# Queue length
redis-cli LLEN auction:events:queue

# View queue contents (last 10 events)
redis-cli LRANGE auction:events:queue 0 9

# Clear queue (⚠️ careful!)
redis-cli DEL auction:events:queue
```

### Query Database
```bash
# Connect
psql -h localhost -U dbuser -d dbcar

# See all processed events
SELECT event_id, consumer_name, event_type, processed_at FROM processed_events;

# See most recent
SELECT * FROM processed_events ORDER BY created_at DESC LIMIT 10;

# Check unique constraint works
SELECT event_id, COUNT(*) FROM processed_events GROUP BY event_id HAVING COUNT(*) > 1;
```

### Check API Health
```bash
# Health endpoint
curl http://localhost:8080/actuator/health

# Expected: {"status":"UP"}
```

---

## ⚠️ Troubleshooting

### Issue: Docker daemon not running
```bash
# Solution: Start Docker Desktop
open -a Docker
sleep 5
docker ps  # Should work now
```

### Issue: Port already in use
```bash
# Find process using port 8080
lsof -i :8080

# Kill it
kill -9 <PID>

# Restart docker-compose
docker-compose -f docker-compose.dev.yml --env-file .env.dev up -d
```

### Issue: PostgreSQL migration failed
```bash
# Check Flyway logs
docker logs car-rental-postgres

# Manually run migration
psql -h localhost -U dbuser -d dbcar -f carRental/src/main/resources/db/migration/V3__create_processed_events_table.sql

# Verify table
psql -h localhost -U dbuser -d dbcar -c "\d processed_events"
```

### Issue: Consumer not processing events
```bash
# Check carRental app is running
docker ps | grep car-rental-app

# Check logs
docker logs -f car-rental-app

# Verify Redis is up
redis-cli ping

# Verify PostgreSQL is up
psql -h localhost -U dbuser -d dbcar -c "SELECT 1"

# Ensure processed_events table exists
psql -h localhost -U dbuser -d dbcar -c "\d processed_events"
```

---

## 🎯 Success Criteria

Phase 1 is successfully deployed when:

✅ **Connectivity**
- All 5 containers running and healthy (status: Up)
- Can connect to PostgreSQL on 5432
- Can ping Redis on 6379
- CarRental API responds on 8080

✅ **Database**
- processed_events table created with Flyway V3 migration
- Unique constraint (event_id, consumer_name) present
- Indexes on event_id, consumer_name, processed_at, retry_count

✅ **Event Flow**
- Can publish events to Redis LIST: `auction:events:queue`
- AuctionEventConsumer polls every 1 second
- Events appear in processed_events table within 5 seconds
- Event record shows: event_id, consumer_name = "auction-event-consumer", event_type = "AuctionWonEvent"

✅ **Idempotence**
- Republishing same event_id → NOT reprocessed
- Database shows only 1 record per (event_id, consumer_name) pair
- Unique constraint prevents duplicates

✅ **Performance**
- Consumer latency: < 5 seconds from publish to processed_events
- Redis queue empties within polling interval (1 second)
- No errors in logs

---

## 📚 Related Documentation

- [CQRS_ROADMAP.md](./CQRS_ROADMAP.md) - Phase 1 architecture & design
- [PHASE1_FINAL_SUMMARY.md](./PHASE1_FINAL_SUMMARY.md) - Executive summary
- [DELIVERABLES.md](./DELIVERABLES.md) - What was delivered
- [docs/ARCHITECTURE_DECISION_STATE_MANAGEMENT.md](./docs/ARCHITECTURE_DECISION_STATE_MANAGEMENT.md) - Architecture decisions
- [docs/HYBRID_STATE_PATTERNS.md](./docs/HYBRID_STATE_PATTERNS.md) - State management patterns

---

## 🔄 Next Steps

1. **Verify deployment** using PHASE1_DEPLOYMENT_TEST.sh
2. **Monitor logs** to see event consumption in real-time
3. **Load test** with multiple events to validate throughput
4. **Prepare Phase 2** - feature branch: `feature/cqrs-phase2-improvements`

---

## 📞 Support

If deployment fails, check:
1. Docker daemon running: `docker ps`
2. Environment file exists: `.env.dev` with POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB
3. Ports are available: 5432 (PostgreSQL), 6379 (Redis), 8080 (CarRental), 9090 (Auction)
4. Flyway migration ran: `SELECT * FROM flyway_schema_history;`

