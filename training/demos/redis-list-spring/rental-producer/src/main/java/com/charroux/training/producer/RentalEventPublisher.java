package com.charroux.training.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RentalEventPublisher {

    public static final String QUEUE = "tutorial:v1:rental-events";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RentalEventPublisher(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public long publish(RentalConfirmed event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            Long size = redis.opsForList().rightPush(QUEUE, json);
            if (size == null) {
                throw new IllegalStateException("Redis did not return the queue size");
            }
            return size;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("The rental event cannot be serialized", exception);
        }
    }
}
