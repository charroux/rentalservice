package com.charroux.insuranceService.events;

import com.charroux.insuranceService.repository.InsuranceOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InsuranceOutboxRelay {
    private static final Logger logger = LoggerFactory.getLogger(InsuranceOutboxRelay.class);
    private final InsuranceOutboxRepository repository;
    private final RedisTemplate<String, String> redisTemplate;

    public InsuranceOutboxRelay(
            InsuranceOutboxRepository repository,
            RedisTemplate<String, String> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(
        fixedDelayString = "${events.outbox.poll-delay-ms:500}",
        initialDelayString = "${events.outbox.initial-delay-ms:1000}"
    )
    public void publishPending() {
        for (InsuranceOutboxEvent event : repository
                .findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(Instant.now())) {
            try {
                Long length = redisTemplate.opsForList().rightPush(
                    InsuranceOfferEventPublisher.PUBLISHED_QUEUE,
                    event.getPayload()
                );
                if (length == null) throw new IllegalStateException("Redis did not confirm publication");
                event.markPublished(Instant.now());
                repository.save(event);
                logger.info("Published outbox event {} for rental {}", event.getEventId(), event.getRentalId());
            } catch (Exception failure) {
                event.markFailure(failure.getMessage(), Instant.now());
                repository.save(event);
                logger.warn("Outbox event {} will be retried", event.getEventId(), failure);
            }
        }
    }
}
