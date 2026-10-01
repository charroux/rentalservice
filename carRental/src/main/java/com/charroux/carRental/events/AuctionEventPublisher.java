package com.charroux.carRental.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes auction events to Redis queue.
 * 
 * Phase 1 (Current): Uses simple list-based queue for MVP reliability.
 * Phase 2: Will upgrade to Redis Streams with consumer groups.
 * 
 * Purpose: Notify other services (RentalService, InsuranceService) of auction events.
 */
@Component
@Slf4j
public class AuctionEventPublisher {
    
    private static final String AUCTION_EVENTS_QUEUE = "auction:events:queue";
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public AuctionEventPublisher(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publishes a serialized auction event to Redis queue.
     * 
     * @param eventJson The serialized event JSON
     * @return true if published successfully
     */
    public boolean publishAuctionEvent(String eventJson) {
        try {
            // Push to Redis queue (LPUSH semantics)
            Long queueLength = redisTemplate.opsForList()
                .rightPush(AUCTION_EVENTS_QUEUE, eventJson);
            
            log.info("✓ Published auction event to queue, queue_size={}", queueLength);
            
            return true;
            
        } catch (Exception e) {
            log.error("❌ Failed to publish auction event: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Publishes an AuctionWonEvent to Redis queue for other services to consume.
     * 
     * Phase 1: Simple queue for MVP
     * 
     * @param event The auction event to publish (must have eventId, auctionId, customerId fields)
     * @return true if published successfully
     */
    public boolean publishAuctionWon(Object event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            return publishAuctionEvent(eventJson);
            
        } catch (Exception e) {
            log.error("❌ Failed to publish AuctionWon event: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Gets the queue name for testing/monitoring.
     */
    public String getQueueName() {
        return AUCTION_EVENTS_QUEUE;
    }
}
