package com.charroux.training.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RentalEventConsumer {

    public static final String QUEUE = "tutorial:v1:rental-events";
    private static final Logger logger = LoggerFactory.getLogger(RentalEventConsumer.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final InsuranceOfferStore store;

    public RentalEventConsumer(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            InsuranceOfferStore store) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.store = store;
    }

    @Scheduled(fixedDelayString = "${tutorial.consumer.poll-delay-ms:500}")
    public void consumeOne() {
        String json = redis.opsForList().leftPop(QUEUE);
        if (json == null) {
            return;
        }

        try {
            RentalConfirmed event = objectMapper.readValue(json, RentalConfirmed.class);
            int premium = Math.max(5, (int) Math.ceil(event.dailyPrice() * 0.10));
            InsuranceOffer offer = new InsuranceOffer(
                event.rentalId(), event.vehicleId(), premium
            );
            store.save(offer);
            logger.info("Insurance offer created: {}", offer);
        } catch (JsonProcessingException exception) {
            logger.error("Invalid event removed from the Redis List: {}", json, exception);
        }
    }
}
