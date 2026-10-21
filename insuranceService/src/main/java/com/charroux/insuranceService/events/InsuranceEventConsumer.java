package com.charroux.insuranceService.events;

import com.charroux.insuranceService.entity.InsuranceOffer;
import com.charroux.insuranceService.service.InsuranceOfferCreationService;
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
    private final InsuranceOfferCreationService creationService;
    private final InsuranceDeadLetterPublisher deadLetterPublisher;

    public InsuranceEventConsumer(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            InsuranceOfferCreationService creationService,
            InsuranceDeadLetterPublisher deadLetterPublisher) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.creationService = creationService;
        this.deadLetterPublisher = deadLetterPublisher;
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
        } catch (IllegalArgumentException invalid) {
            try {
                deadLetterPublisher.moveInvalid(eventJson, invalid);
                logger.error("Invalid insurance event moved to dead-letter queue", invalid);
            } catch (Exception dlqFailure) {
                logger.error("Invalid insurance event remains recoverable because DLQ transfer failed", dlqFailure);
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
            String eventType = requiredText(event, "eventType");
            int eventVersion = requiredInteger(event, "eventVersion");
            if (!"AuctionWon".equals(eventType) || eventVersion != 1) {
                throw new IllegalArgumentException(
                    "Unsupported event contract: " + eventType + " v" + eventVersion
                );
            }
            String eventId = requiredText(event, "eventId");
            String rentalId = requiredText(event, "rentalId");
            String plateNumber = requiredText(event, "plateNumber");
            int rentalPrice = requiredInteger(event, "finalPrice");
            int originalPrice = requiredInteger(event, "originalPrice");
            int discount = requiredInteger(event, "discount");
            int dailyPremium = Math.max(
                MINIMUM_DAILY_PREMIUM,
                (int) Math.ceil(rentalPrice * PREMIUM_RATE)
            );

            InsuranceOffer offer = creationService.createOrReplay(new InsuranceOffer(
                eventId,
                rentalId,
                new InsuranceOffer.Vehicle(
                    requiredLong(event, "carId"),
                    plateNumber,
                    requiredText(event, "carBrand"),
                    requiredText(event, "carModel")
                ),
                rentalPrice,
                originalPrice,
                discount,
                dailyPremium,
                java.time.Instant.now()
            ));

            logger.info(
                "Insurance offer created for rental {}, premium={} EUR/day",
                rentalId,
                dailyPremium
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid event JSON", e);
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

    private long requiredLong(JsonNode event, String fieldName) {
        JsonNode value = event.path(fieldName);
        if (!value.isIntegralNumber()) {
            throw new IllegalArgumentException("Missing integer field: " + fieldName);
        }
        return value.asLong();
    }
}
