package com.charroux.insuranceService.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class InsuranceDeadLetterPublisher {
    public static final String DEAD_LETTER_QUEUE = "auction:events:simple:insurance-service:dead-letter";
    private static final DefaultRedisScript<Long> MOVE_TO_DLQ = new DefaultRedisScript<>(
        "redis.call('RPUSH', KEYS[2], ARGV[2]); return redis.call('LREM', KEYS[1], 1, ARGV[1])",
        Long.class
    );

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public InsuranceDeadLetterPublisher(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void moveInvalid(String rawEvent, Exception failure) {
        try {
            String record = objectMapper.writeValueAsString(new DeadLetterRecord(
                InsuranceEventConsumer.PROCESSING_QUEUE,
                rawEvent,
                failure.getClass().getSimpleName(),
                failure.getMessage(),
                Instant.now()
            ));
            Long removed = redisTemplate.execute(
                MOVE_TO_DLQ,
                List.of(InsuranceEventConsumer.PROCESSING_QUEUE, DEAD_LETTER_QUEUE),
                rawEvent,
                record
            );
            if (removed == null || removed != 1) throw new IllegalStateException("Could not atomically move invalid event to DLQ");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize dead-letter record", e);
        }
    }

    record DeadLetterRecord(
        String sourceQueue, String payload, String failureType, String error, Instant failedAt
    ) {}
}
