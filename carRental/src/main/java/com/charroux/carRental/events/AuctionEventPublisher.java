package com.charroux.carRental.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Publishes auction events to Redis for consumption by partner services.
 * Uses RedisTemplate with a list-based queue for simplicity in the prototype.
 * Later can be migrated to full Redis Streams or Kafka without code changes in consumers.
 */
@Service
@Slf4j
public class AuctionEventPublisher {
    
    private static final String AUCTION_EVENTS_QUEUE = "auction:events:queue";
    private static final long EVENT_TTL_HOURS = 24;
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public AuctionEventPublisher(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publishes an AuctionWonEvent to the Redis queue for consumption by partner services.
     * 
     * @param event The auction won event to publish
     * @return true if the event was successfully published, false otherwise
     */
    public boolean publishAuctionWon(AuctionWonEvent event) {
        try {
            // Serialize event to JSON with metadata
            String eventJson = objectMapper.writeValueAsString(event);
            
            String messageWithMetadata = String.format(
                "{\"eventType\":\"AuctionWon\",\"eventId\":\"%s\",\"timestamp\":%d,\"data\":%s}",
                event.getEventId(),
                event.getTimestamp(),
                eventJson
            );
            
            // Push event to Redis list (acts as queue for consumers)
            Long result = redisTemplate.opsForList().rightPush(AUCTION_EVENTS_QUEUE, messageWithMetadata);
            
            if (result != null && result > 0) {
                // Set TTL on the queue to prevent unbounded growth
                redisTemplate.expire(AUCTION_EVENTS_QUEUE, EVENT_TTL_HOURS, TimeUnit.HOURS);
                
                log.info("✓ AuctionWonEvent published successfully - eventId: {} | rentalId: {} | carBrand: {} | carModel: {} | plateNumber: {}",
                    event.getEventId(),
                    event.getRentalId(),
                    event.getCarBrand(),
                    event.getCarModel(),
                    event.getPlateNumber());
                
                return true;
            } else {
                log.warn("❌ Failed to publish AuctionWonEvent to Redis queue - eventId: {}", event.getEventId());
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ Error publishing AuctionWonEvent to Redis: eventId={}, error={}", 
                event.getEventId(), 
                e.getMessage(), 
                e);
            return false;
        }
    }
}
