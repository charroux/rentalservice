package com.charroux.carRental.events;

import com.charroux.carRental.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisListCommands.Direction;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consumes auction events from Redis queue.
 * 
 * Phase 1: Simple consumer with idempotence tracking.
 * Phase 2: Will upgrade to Redis Streams with consumer groups.
 * 
 * Purpose: Consume AuctionWon events and coordinate with other services.
 * Currently Phase 1: Just logs events - Phase 2 will trigger real business logic.
 */
@Component
@Slf4j
public class AuctionEventConsumer {
    
    static final String READY_QUEUE = "auction:events:simple:rental-service:ready";
    static final String PROCESSING_QUEUE =
        "auction:events:simple:rental-service:processing";
    private static final String CONSUMER_NAME = "rental-service";
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    
    public AuctionEventConsumer(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            ProcessedEventRepository processedEventRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
    }
    
    /**
     * Poll and process auction events from Redis queue.
     * Runs every 1 second.
     */
    @Scheduled(
        fixedDelayString = "${events.simple.poll-delay-ms:1000}",
        initialDelayString = "${events.simple.initial-delay-ms:1000}"
    )
    public void consumeEvents() {
        String eventJson = null;
        try {
            ListOperations<String, String> lists = redisTemplate.opsForList();

            // LMOVE is atomic: a message is never removed without first being
            // made visible in the processing queue.
            eventJson = lists.move(
                READY_QUEUE,
                Direction.LEFT,
                PROCESSING_QUEUE,
                Direction.RIGHT
            );
            
            if (eventJson == null) {
                return;  // Queue is empty, wait for next poll
            }
            
            processEvent(eventJson);

            // Acknowledge only after the database idempotence record commits.
            Long removed = lists.remove(PROCESSING_QUEUE, 1, eventJson);
            if (removed == null || removed != 1) {
                log.warn("Event processed but could not be acknowledged: {}", eventIdForLog(eventJson));
            }
            
        } catch (Exception e) {
            // The event remains in the processing queue and will be retried.
            log.error("Error in event consumer loop; event remains recoverable: {}",
                e.getMessage(), e);
        }
    }

    /**
     * Periodically moves unacknowledged events back to the head of the ready
     * queue. Phase 1 deliberately runs one logical consumer instance; Streams
     * will later provide per-consumer pending-entry ownership.
     */
    @Scheduled(
        fixedDelayString = "${events.simple.retry-delay-ms:30000}",
        initialDelayString = "${events.simple.retry-delay-ms:30000}"
    )
    public void retryUnacknowledgedEvents() {
        recoverProcessingQueue();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        recoverProcessingQueue();
    }

    void recoverProcessingQueue() {
        try {
            ListOperations<String, String> lists = redisTemplate.opsForList();
            int recovered = 0;
            String event;
            // RIGHT -> LEFT preserves FIFO order when several events remain.
            while ((event = lists.move(
                    PROCESSING_QUEUE,
                    Direction.RIGHT,
                    READY_QUEUE,
                    Direction.LEFT)) != null) {
                recovered++;
            }
            if (recovered > 0) {
                log.warn("Recovered {} unacknowledged event(s)", recovered);
            }
        } catch (Exception e) {
            log.error("Could not recover the processing queue: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process a single auction event.
     */
    private void processEvent(String eventJson) {
        try {
            JsonNode event = objectMapper.readTree(eventJson);
            String eventId = requiredText(event, "eventId");
            String eventType = event.path("eventType").asText("AuctionWon");
            
            // Check idempotence: has this event already been processed?
            boolean alreadyProcessed = processedEventRepository
                .existsByEventIdAndConsumerName(eventId, CONSUMER_NAME);
            
            if (alreadyProcessed) {
                log.debug("⏭️  Event {} already processed by {}, skipping",
                    eventId, CONSUMER_NAME);
                return;
            }
            
            // Phase 1: log and record receipt. Business projections will be
            // introduced independently on the future Stream/CQRS path.
            log.info("Auction event received and processed: {}",
                eventJson.substring(0, Math.min(100, eventJson.length())));
            
            // Record as processed for idempotence
            processedEventRepository.recordProcessed(eventId, CONSUMER_NAME, eventType);
            
        } catch (Exception e) {
            throw new IllegalStateException("Failed to process auction event", e);
        }
    }
    
    /**
     * Extract event ID from JSON string (simple JSON parsing for Phase 1).
     */
    private String requiredText(JsonNode event, String fieldName) {
        String value = event.path(fieldName).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }
        return value;
    }

    private String eventIdForLog(String eventJson) {
        try {
            return objectMapper.readTree(eventJson).path("eventId").asText("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
