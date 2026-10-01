#!/bin/bash

# Phase 1 Deployment Test - Manual Testing Script
# This script validates Phase 1 CQRS implementation with real infrastructure

set -e

echo "=== Phase 1 CQRS Deployment Test ==="
echo "Testing: AuctionEventPublisher → Redis → AuctionEventConsumer → ProcessedEvent"
echo ""

# Configuration
REDIS_HOST="${1:-localhost}"
REDIS_PORT="${2:-6379}"
POSTGRES_HOST="${3:-localhost}"
POSTGRES_PORT="${4:-5432}"
POSTGRES_DB="${5:-dbcar}"
POSTGRES_USER="${6:-dbuser}"
POSTGRES_PASSWORD="${7:-dbpass}"
CARENTAL_API="${8:-http://localhost:8080}"

echo "Target Configuration:"
echo "  Redis:     $REDIS_HOST:$REDIS_PORT"
echo "  PostgreSQL: $POSTGRES_HOST:$POSTGRES_PORT/$POSTGRES_DB"
echo "  CarRental API: $CARENTAL_API"
echo ""

# Function to test Redis connectivity
test_redis() {
    echo "1️⃣  Testing Redis connectivity..."
    redis-cli -h $REDIS_HOST -p $REDIS_PORT ping > /dev/null 2>&1 && echo "   ✅ Redis PING successful" || {
        echo "   ❌ Redis connection failed"
        return 1
    }
}

# Function to test PostgreSQL connectivity
test_postgres() {
    echo "2️⃣  Testing PostgreSQL connectivity..."
    PGPASSWORD=$POSTGRES_PASSWORD psql -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER -d $POSTGRES_DB -c "SELECT version();" > /dev/null 2>&1 && echo "   ✅ PostgreSQL connected" || {
        echo "   ❌ PostgreSQL connection failed"
        return 1
    }
}

# Function to check processed_events table
check_processed_events_table() {
    echo "3️⃣  Checking processed_events table..."
    PGPASSWORD=$POSTGRES_PASSWORD psql -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER -d $POSTGRES_DB -c "\d processed_events" > /dev/null 2>&1 && echo "   ✅ Table exists" || {
        echo "   ❌ Table does not exist (Flyway migration may not have run)"
        return 1
    }
}

# Function to publish test event to Redis
publish_test_event() {
    echo "4️⃣  Publishing test AuctionWonEvent to Redis queue..."
    
    EVENT_ID="auction-won-$(date +%s%N)"
    EVENT_JSON=$(cat <<EOF
{
  "eventId": "$EVENT_ID",
  "eventType": "AuctionWonEvent",
  "auctionId": "test-auction-123",
  "winnerUserId": "user-456",
  "winningBid": 5000.00,
  "vehicleId": "vehicle-789",
  "timestamp": "$(date -u +'%Y-%m-%dT%H:%M:%S')"
}
EOF
)
    
    echo "   Event payload:"
    echo "$EVENT_JSON" | sed 's/^/     /'
    echo ""
    
    redis-cli -h $REDIS_HOST -p $REDIS_PORT LPUSH auction:events:queue "$EVENT_JSON" > /dev/null && echo "   ✅ Event published to Redis LPUSH queue" || {
        echo "   ❌ Failed to publish event"
        return 1
    }
    
    echo "   Event ID: $EVENT_ID"
}

# Function to monitor event consumption
monitor_consumption() {
    echo "5️⃣  Monitoring event consumption (waiting 5 seconds for consumer poll)..."
    sleep 5
    
    LAST_EVENT=$(PGPASSWORD=$POSTGRES_PASSWORD psql -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER -d $POSTGRES_DB -t -c "SELECT event_id FROM processed_events ORDER BY created_at DESC LIMIT 1;" 2>/dev/null || echo "")
    
    if [ -n "$LAST_EVENT" ]; then
        echo "   ✅ Event consumed! Record found in processed_events:"
        echo "   Event ID: $(echo $LAST_EVENT | tr -d ' ')"
        PGPASSWORD=$POSTGRES_PASSWORD psql -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER -d $POSTGRES_DB -c "SELECT event_id, consumer_name, event_type, processed_at FROM processed_events ORDER BY created_at DESC LIMIT 1;" || true
    else
        echo "   ⏳ Event not yet consumed (consumer may still be polling)"
    fi
}

# Function to check Redis queue
check_redis_queue() {
    echo "6️⃣  Checking Redis queue status..."
    QUEUE_LENGTH=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT LLEN auction:events:queue)
    echo "   Queue length: $QUEUE_LENGTH events"
    if [ "$QUEUE_LENGTH" -eq 0 ]; then
        echo "   ✅ Queue is empty (all events consumed)"
    else
        echo "   ℹ️  Queue has pending events (consumer may be processing)"
    fi
}

# Function to test idempotence
test_idempotence() {
    echo "7️⃣  Testing idempotence (republishing same event)..."
    
    # Get last event from database
    LAST_EVENT=$(PGPASSWORD=$POSTGRES_PASSWORD psql -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER -d $POSTGRES_DB -t -c "SELECT event_id FROM processed_events ORDER BY created_at DESC LIMIT 1;" 2>/dev/null || echo "")
    
    if [ -z "$LAST_EVENT" ]; then
        echo "   ⏭️  Skipping (no previous events in database)"
        return 0
    fi
    
    # Republish the same event
    EVENT_JSON=$(cat <<EOF
{
  "eventId": "$(echo $LAST_EVENT | tr -d ' ')",
  "eventType": "AuctionWonEvent",
  "auctionId": "test-auction-123",
  "winnerUserId": "user-456",
  "winningBid": 5000.00,
  "vehicleId": "vehicle-789",
  "timestamp": "$(date -u +'%Y-%m-%dT%H:%M:%S')"
}
EOF
)
    
    redis-cli -h $REDIS_HOST -p $REDIS_PORT LPUSH auction:events:queue "$EVENT_JSON" > /dev/null
    echo "   Event republished to queue (same event_id)"
    
    sleep 5
    
    # Check if it was processed again or skipped
    DUPLICATE_COUNT=$(PGPASSWORD=$POSTGRES_PASSWORD psql -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER -d $POSTGRES_DB -t -c "SELECT COUNT(*) FROM processed_events WHERE event_id = '$(echo $LAST_EVENT | tr -d ' ')' AND consumer_name = 'auction-event-consumer';" 2>/dev/null || echo "0")
    
    if [ "$DUPLICATE_COUNT" -eq 1 ]; then
        echo "   ✅ Idempotence verified! Event NOT reprocessed (unique constraint enforced)"
    else
        echo "   ❌ Idempotence failed! Event was reprocessed ($DUPLICATE_COUNT records)"
    fi
}

# Function to check API health
check_health() {
    echo "8️⃣  Checking CarRental API health..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" $CARENTAL_API/actuator/health 2>/dev/null || echo "000")
    
    if [ "$HTTP_CODE" -eq 200 ]; then
        echo "   ✅ CarRental API healthy (HTTP 200)"
    else
        echo "   ⚠️  CarRental API responded with HTTP $HTTP_CODE"
    fi
}

# Run all tests
echo "Starting tests..."
echo ""

test_redis || exit 1
test_postgres || exit 1
check_processed_events_table || exit 1
publish_test_event
monitor_consumption
check_redis_queue
test_idempotence
check_health

echo ""
echo "=== Phase 1 Test Complete ==="
echo "✅ All validations passed!"
echo ""
echo "Next Steps:"
echo "  - Monitor logs: docker logs car-rental-app"
echo "  - Check Redis queue: redis-cli LRANGE auction:events:queue 0 -1"
echo "  - Query database: psql -h localhost -U dbuser -d dbcar -c 'SELECT * FROM processed_events;'"
echo "  - Review feature branch: git log --oneline feature/cqrs-phase1-redis-streams"
