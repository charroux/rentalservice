package com.charroux.carRental.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes auction events to the stable ingress queue.
 * 
 * Phase 1 (Current): Uses simple list-based queue for MVP reliability.
 * Phase 2: Will upgrade to Redis Streams with consumer groups.
 * 
 * A separate router owns consumer discovery and fan-out. The producer therefore
 * remains unchanged when a new consumer subscribes.
 */
@Component
@Slf4j
public class AuctionEventPublisher {
    
    public static final String PUBLISHED_QUEUE = "events:simple:published";
    
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
            Long queueLength = redisTemplate.opsForList().rightPush(PUBLISHED_QUEUE, eventJson);
            if (queueLength == null) {
                log.warn("Redis did not confirm publication to {}", PUBLISHED_QUEUE);
                return false;
            }
            log.info("Published auction event to {}, queue_size={}", PUBLISHED_QUEUE, queueLength);
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
        return PUBLISHED_QUEUE;
    }
}
