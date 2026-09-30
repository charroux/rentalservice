package com.charroux.carRental.events;

import com.charroux.carRental.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
    
    private static final String AUCTION_EVENTS_QUEUE = "auction:events:queue";
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
    @Scheduled(fixedRate = 1000)
    public void consumeEvents() {
        try {
            // Pop from left (oldest first)
            Optional<String> eventJson = Optional.ofNullable(
                redisTemplate.opsForList().leftPop(AUCTION_EVENTS_QUEUE)
            );
            
            if (eventJson.isEmpty()) {
                return;  // Queue is empty, wait for next poll
            }
            
            processEvent(eventJson.get());
            
        } catch (Exception e) {
            log.error("❌ Error in event consumer loop: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process a single auction event.
     */
    private void processEvent(String eventJson) {
        try {
            // For Phase 1, we just process the JSON string
            // Phase 2 will deserialize to specific event types
            
            // Extract event ID from JSON for idempotence check
            String eventId = extractEventId(eventJson);
            
            if (eventId == null) {
                log.warn("⚠️  Could not extract eventId from JSON: {}", eventJson);
                return;
            }
            
            // Check idempotence: has this event already been processed?
            boolean alreadyProcessed = processedEventRepository
                .existsByEventIdAndConsumerName(eventId, CONSUMER_NAME);
            
            if (alreadyProcessed) {
                log.debug("⏭️  Event {} already processed by {}, skipping",
                    eventId, CONSUMER_NAME);
                return;
            }
            
            // Phase 1: Just log the event
            // Phase 2: Trigger rental updates, insurance notifications, etc.
            log.info("✓ Auction event received and processed: {}", 
                eventJson.substring(0, Math.min(100, eventJson.length())));
            
            // Record as processed for idempotence
            processedEventRepository.recordProcessed(eventId, CONSUMER_NAME, "AuctionEvent");
            
        } catch (Exception e) {
            log.error("❌ Failed to process auction event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Extract event ID from JSON string (simple JSON parsing for Phase 1).
     */
    private String extractEventId(String eventJson) {
        try {
            // Very simple extraction: look for "eventId" field
            int startIdx = eventJson.indexOf("\"eventId\"");
            if (startIdx == -1) {
                return null;
            }
            int colonIdx = eventJson.indexOf(":", startIdx);
            int quoteIdx = eventJson.indexOf("\"", colonIdx);
            int endQuoteIdx = eventJson.indexOf("\"", quoteIdx + 1);
            return eventJson.substring(quoteIdx + 1, endQuoteIdx);
        } catch (Exception e) {
            log.debug("Could not extract eventId from JSON: {}", e.getMessage());
            return null;
        }
    }
}
