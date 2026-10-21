package com.charroux.insuranceService.repository;

import com.charroux.insuranceService.events.InsuranceOutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface InsuranceOutboxRepository extends MongoRepository<InsuranceOutboxEvent, String> {
    boolean existsByEventId(String eventId);
    List<InsuranceOutboxEvent> findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(Instant now);
}
