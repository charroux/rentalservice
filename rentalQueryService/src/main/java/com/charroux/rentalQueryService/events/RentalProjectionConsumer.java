package com.charroux.rentalQueryService.events;

import com.charroux.rentalQueryService.projection.ProjectionIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisListCommands.Direction;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "events.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class RentalProjectionConsumer {
    public static final String READY_QUEUE = "auction:events:simple:rental-query-service:ready";
    public static final String PROCESSING_QUEUE = "auction:events:simple:rental-query-service:processing";
    private static final Logger logger = LoggerFactory.getLogger(RentalProjectionConsumer.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ProjectionIngestionService ingestionService;
    private final ProjectionDeadLetterPublisher deadLetterPublisher;

    public RentalProjectionConsumer(
            RedisTemplate<String, String> redisTemplate,
            ProjectionIngestionService ingestionService,
            ProjectionDeadLetterPublisher deadLetterPublisher) {
        this.redisTemplate = redisTemplate;
        this.ingestionService = ingestionService;
        this.deadLetterPublisher = deadLetterPublisher;
    }

    @Scheduled(fixedDelayString = "${events.simple.poll-delay-ms:1000}",
        initialDelayString = "${events.simple.initial-delay-ms:1000}")
    public void consumeEvents() {
        String raw = null;
        try {
            ListOperations<String, String> lists = redisTemplate.opsForList();
            raw = lists.move(READY_QUEUE, Direction.LEFT, PROCESSING_QUEUE, Direction.RIGHT);
            if (raw == null) return;
            ingestionService.ingest(raw);
            Long removed = lists.remove(PROCESSING_QUEUE, 1, raw);
            if (removed == null || removed != 1) logger.warn("Projection event applied but not acknowledged");
        } catch (IllegalArgumentException invalid) {
            try {
                deadLetterPublisher.moveInvalid(raw, invalid);
                logger.error("Invalid projection event moved to dead-letter queue", invalid);
            } catch (Exception dlqFailure) {
                logger.error("Invalid projection event remains recoverable because DLQ transfer failed", dlqFailure);
            }
        } catch (Exception e) {
            logger.error("Projection event remains recoverable after processing failure", e);
        }
    }

    @Scheduled(fixedDelayString = "${events.simple.retry-delay-ms:30000}",
        initialDelayString = "${events.simple.retry-delay-ms:30000}")
    public void retryUnacknowledgedEvents() { recoverProcessingQueue(); }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() { recoverProcessingQueue(); }

    void recoverProcessingQueue() {
        try {
            ListOperations<String, String> lists = redisTemplate.opsForList();
            int recovered = 0;
            while (lists.move(PROCESSING_QUEUE, Direction.RIGHT, READY_QUEUE, Direction.LEFT) != null) recovered++;
            if (recovered > 0) logger.warn("Recovered {} projection event(s)", recovered);
        } catch (Exception e) {
            logger.error("Could not recover projection processing queue", e);
        }
    }
}
