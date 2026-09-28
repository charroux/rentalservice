# Testing Event Publishing with Docker Compose

## Quick Start

### 1. Bring up the Docker Compose stack

```bash
# From the project root
docker-compose -f docker-compose.dev.yml up -d

# Verify all services are healthy
docker-compose -f docker-compose.dev.yml ps
```

Expected output:
```
NAME                 STATUS          PORTS
car-rental-redis     Up (healthy)    0.0.0.0:6379->6379/tcp
car-rental           Up              0.0.0.0:8080->8080/tcp
postgres             Up (healthy)    0.0.0.0:5432->5432/tcp
```

### 2. Wait for carRental service to be ready

```bash
# Check service startup logs
docker-compose -f docker-compose.dev.yml logs car-rental | tail -20

# Wait for message: "Started CarRentalApplication in X seconds"
```

### 3. Test the REST endpoint and verify event publishing

#### Test Auction Participation (Trigger Event Publishing)

```bash
# Make a request to the auction endpoint
curl -X POST http://localhost:8080/carRental/auction/participate \
  -H "Content-Type: application/json" \
  -d '{
    "carModelId": 1
  }' \
  -v

# Expected response:
# HTTP/1.1 200 OK
# {
#   "success": true,
#   "message": "Auction won successfully",
#   "car": { ... car details ... }
# }
```

#### Verify Event in Redis Queue

```bash
# Check queue length (number of events)
redis-cli LLEN auction:events:queue

# Expected output: 1 (or more if you've made multiple requests)

# Inspect the event (peek at oldest event)
redis-cli LRANGE auction:events:queue 0 0

# Pretty-print the JSON (copy the JSON from above)
echo '{"eventType":"AuctionWon",...}' | jq .

# Expected structure:
# {
#   "eventType": "AuctionWon",
#   "eventId": "550e8400-e29b-41d4-a716-446655440000",
#   "timestamp": 1701234567890,
#   "data": {
#     "eventId": "550e8400-e29b-41d4-a716-446655440000",
#     "rentalId": "HERTZ-123",
#     "carId": 42,
#     "plateNumber": "ABC-123-XYZ",
#     "customerId": "CUST-001",
#     "carBrand": "Ferrari",
#     "carModel": "F8",
#     "finalPrice": 850,
#     "originalPrice": 1000,
#     "discount": 150,
#     "rentalStartDate": "2024-12-20T10:30:00",
#     "rentalEndDate": "2024-12-25T10:30:00",
#     "timestamp": 1701234567890
#   }
# }
```

#### Monitor Continuous Events

```bash
# Watch queue growth in real-time
watch -n 1 'redis-cli LLEN auction:events:queue'

# In another terminal, send multiple auction requests
for i in {1..5}; do
  curl -X POST http://localhost:8080/carRental/auction/participate \
    -H "Content-Type: application/json" \
    -d "{\"carModelId\": $i}" \
    -s > /dev/null && echo "Request $i sent"
  sleep 1
done

# Observe queue length increase in real-time
```

### 4. Access Redis CLI

```bash
# Open Redis CLI
redis-cli

# Useful commands:
LLEN auction:events:queue              # Queue length
LRANGE auction:events:queue 0 -1       # View all events (destructive - pops them!)
LRANGE auction:events:queue 0 9        # View first 10 events (non-destructive peek)
TTL auction:events:queue               # Check TTL (expires in)
KEYS *                                 # List all keys
FLUSHDB                                # Clear all data (careful!)

# Exit with Ctrl+D
```

### 5. View Service Logs

```bash
# View carRental service logs in real-time
docker-compose -f docker-compose.dev.yml logs -f car-rental

# View specific log entries related to events
docker-compose -f docker-compose.dev.yml logs car-rental | grep -i "event\|auction"

# View Redis logs
docker-compose -f docker-compose.dev.yml logs -f redis
```

### 6. Check Spring Boot Actuator Health

```bash
# Check application health
curl http://localhost:8080/actuator/health | jq .

# Expected output:
# {
#   "status": "UP",
#   "components": {
#     "redis": {
#       "status": "UP"
#     },
#     "db": {
#       "status": "UP"
#     }
#   }
# }

# View metrics
curl http://localhost:8080/actuator/metrics | jq .

# Check specific metric (e.g., HTTP requests)
curl http://localhost:8080/actuator/metrics/http.server.requests | jq .
```

### 7. Tear Down

```bash
# Stop and remove all containers
docker-compose -f docker-compose.dev.yml down

# Also remove volumes (cleans up persisted data)
docker-compose -f docker-compose.dev.yml down -v
```

## End-to-End Test Scenario

### Scenario: Auction Won Event Publishing

1. **Precondition**: Docker Compose stack running
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

2. **Action**: Trigger auction participation
   ```bash
   RESPONSE=$(curl -s -X POST http://localhost:8080/carRental/auction/participate \
     -H "Content-Type: application/json" \
     -d '{"carModelId": 1}')
   
   echo "Response: $RESPONSE"
   ```

3. **Verify**: Check event was published to Redis
   ```bash
   # Check queue length increased
   QUEUE_LENGTH=$(redis-cli LLEN auction:events:queue)
   echo "Queue length: $QUEUE_LENGTH"
   
   # Inspect event content
   redis-cli LINDEX auction:events:queue 0 | jq .data
   ```

4. **Validation**: Event contains auction details
   ```bash
   # Extract and verify event fields
   redis-cli LINDEX auction:events:queue 0 | jq '.data | {
     eventId,
     rentalId,
     carId,
     plateNumber,
     customerId,
     carBrand,
     carModel,
     finalPrice,
     originalPrice,
     discount,
     rentalStartDate,
     rentalEndDate
   }'
   ```

5. **Success Criteria**:
   - ✅ HTTP 200 response from `/auction/participate`
   - ✅ Event appears in Redis queue within 1 second
   - ✅ Event contains all required fields (16 total)
   - ✅ Event timestamps are valid (timestamp ≤ current time)
   - ✅ Rental end date is 5 days after start date
   - ✅ Event ID is a valid UUID

## Advanced: Consume Events

### Using Redis CLI

```bash
# Pop events one at a time (removes from queue)
redis-cli LPOP auction:events:queue

# Pop multiple events at once
for i in {1..5}; do
  echo "Event $i:"
  redis-cli LPOP auction:events:queue | jq .
done
```

### Using Node.js Consumer Script

Create `test-consumer.js`:

```javascript
const redis = require('redis');
const client = redis.createClient({host: 'localhost', port: 6379});

async function consumeEvents() {
  await client.connect();
  console.log('Connected to Redis. Listening for auction.won events...\n');
  
  while (true) {
    try {
      const event = await client.lPop('auction:events:queue');
      if (event) {
        const parsed = JSON.parse(event);
        console.log('📨 Received event:');
        console.log(`   Event ID: ${parsed.eventId}`);
        console.log(`   Rental ID: ${parsed.data.rentalId}`);
        console.log(`   Car: ${parsed.data.carBrand} ${parsed.data.carModel}`);
        console.log(`   Price: $${parsed.data.finalPrice}`);
        console.log(`   Timestamp: ${new Date(parsed.timestamp).toISOString()}\n`);
      } else {
        // No events, wait before checking again
        await new Promise(resolve => setTimeout(resolve, 5000));
      }
    } catch (error) {
      console.error('Error consuming event:', error);
    }
  }
}

consumeEvents();
```

Run consumer:
```bash
npm install redis
node test-consumer.js
```

## Troubleshooting

### Docker Compose fails to start

```bash
# Check for port conflicts
lsof -i :6379  # Redis
lsof -i :8080  # carRental
lsof -i :5432  # PostgreSQL

# If ports are in use, either:
# 1. Stop the conflicting service
# 2. Change ports in docker-compose.dev.yml
```

### Redis connection failed from Spring Boot

```bash
# Verify Redis is running
redis-cli ping
# Expected: PONG

# Check Docker network connectivity
docker network inspect car-rental-network

# Verify carRental can reach Redis
docker-compose -f docker-compose.dev.yml exec car-rental \
  bash -c "curl redis:6379 || nc -zv redis 6379"
```

### No events in Redis queue

**Possible causes:**

1. Auction didn't complete successfully
   - Check carRental logs: `docker-compose logs car-rental | grep -i error`
   - Verify gRPC auction service is running: `docker-compose logs auctionServiceServer`

2. Event publishing failed silently
   - Check logs for: "Error publishing event"
   - Verify Redis is healthy: `redis-cli PING`

3. Events already consumed
   - Check if another consumer is running
   - Verify queue wasn't cleared: `redis-cli DBSIZE`

### Event JSON format is invalid

```bash
# Validate JSON structure
redis-cli LRANGE auction:events:queue 0 0 | jq .

# If jq fails, the JSON is malformed
# Check logs for serialization errors
docker-compose logs car-rental | grep -i "serialization\|jackson"
```

## Performance Testing

### Load Test: Send 100 Auction Requests

```bash
#!/bin/bash

# Script: test-load.sh
echo "Starting load test..."
for i in {1..100}; do
  curl -s -X POST http://localhost:8080/carRental/auction/participate \
    -H "Content-Type: application/json" \
    -d "{\"carModelId\": $((RANDOM % 10 + 1))}" > /dev/null &
  
  if [ $((i % 10)) -eq 0 ]; then
    echo "Sent $i requests..."
  fi
done

wait
echo "All 100 requests sent"

# Check final queue size
QUEUE_SIZE=$(redis-cli LLEN auction:events:queue)
echo "Final queue size: $QUEUE_SIZE"

# Monitor memory usage
redis-cli INFO memory | grep used
```

Run with:
```bash
chmod +x test-load.sh
./test-load.sh
```

### Monitor Performance

```bash
# Terminal 1: Watch queue growth
watch -n 0.5 'echo "Queue: $(redis-cli LLEN auction:events:queue) events"'

# Terminal 2: Monitor Redis memory
watch -n 1 'redis-cli INFO memory | grep "^used"'

# Terminal 3: Monitor Spring Boot response time
curl -v http://localhost:8080/carRental/auction/participate \
  -H "Content-Type: application/json" \
  -d '{"carModelId": 1}' 2>&1 | grep "< HTTP\|time\|Date"
```

## Cleanup & Reset

```bash
# Clear all events from Redis
redis-cli DEL auction:events:queue

# Stop services but keep data
docker-compose -f docker-compose.dev.yml stop

# Restart services with persisted data
docker-compose -f docker-compose.dev.yml start

# Full cleanup (removes data)
docker-compose -f docker-compose.dev.yml down -v
```

---

**Status**: ✅ Ready for testing
**Tested**: December 2024
**Docker Compose Version**: 3.9+
**Redis Version**: 7-alpine
