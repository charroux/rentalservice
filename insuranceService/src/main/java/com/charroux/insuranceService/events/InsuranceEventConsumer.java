package com.charroux.insuranceService.events;

import com.charroux.insuranceService.entity.InsuranceOffer;
import com.charroux.insuranceService.repository.InsuranceOfferRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisListCommands.Direction;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InsuranceEventConsumer {

    public static final String READY_QUEUE =
        "auction:events:simple:insurance-service:ready";
    public static final String PROCESSING_QUEUE =
        "auction:events:simple:insurance-service:processing";

    private static final Logger logger = LoggerFactory.getLogger(InsuranceEventConsumer.class);
    private static final double PREMIUM_RATE = 0.10;
    private static final int MINIMUM_DAILY_PREMIUM = 5;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final InsuranceOfferRepository repository;

    public InsuranceEventConsumer(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            InsuranceOfferRepository repository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    @Scheduled(
        fixedDelayString = "${events.simple.poll-delay-ms:1000}",
        initialDelayString = "${events.simple.initial-delay-ms:1000}"
    )
    public void consumeEvents() {
        String eventJson = null;
        try {
            ListOperations<String, String> lists = redisTemplate.opsForList();
            eventJson = lists.move(
                READY_QUEUE,
                Direction.LEFT,
                PROCESSING_QUEUE,
                Direction.RIGHT
            );
            if (eventJson == null) {
                return;
            }

            processEvent(eventJson);

            Long removed = lists.remove(PROCESSING_QUEUE, 1, eventJson);
            if (removed == null || removed != 1) {
                logger.warn("Insurance event processed but not acknowledged");
            }
        } catch (Exception e) {
            logger.error("Insurance event remains recoverable after processing failure", e);
        }
    }

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
            while (lists.move(
                    PROCESSING_QUEUE,
                    Direction.RIGHT,
                    READY_QUEUE,
                    Direction.LEFT) != null) {
                recovered++;
            }
            if (recovered > 0) {
                logger.warn("Recovered {} unacknowledged insurance event(s)", recovered);
            }
        } catch (Exception e) {
            logger.error("Could not recover insurance processing queue", e);
        }
    }

    private void processEvent(String eventJson) {
        try {
            JsonNode event = objectMapper.readTree(eventJson);
            String eventId = requiredText(event, "eventId");
            if (repository.existsByEventId(eventId)) {
                logger.debug("Insurance event {} already processed", eventId);
                return;
            }

            String rentalId = requiredText(event, "rentalId");
            String plateNumber = requiredText(event, "plateNumber");
            int rentalPrice = requiredInteger(event, "finalPrice");
            int dailyPremium = Math.max(
                MINIMUM_DAILY_PREMIUM,
                (int) Math.ceil(rentalPrice * PREMIUM_RATE)
            );

            repository.save(new InsuranceOffer(
                eventId,
                rentalId,
                plateNumber,
                rentalPrice,
                dailyPremium
            ));

            logger.info(
                "Insurance offer created for rental {}, premium={} EUR/day",
                rentalId,
                dailyPremium
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to process insurance event", e);
        }
    }

    private String requiredText(JsonNode event, String fieldName) {
        String value = event.path(fieldName).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }
        return value;
    }

    private int requiredInteger(JsonNode event, String fieldName) {
        JsonNode value = event.path(fieldName);
        if (!value.isIntegralNumber()) {
            throw new IllegalArgumentException("Missing integer field: " + fieldName);
        }
        return value.asInt();
    }
}
