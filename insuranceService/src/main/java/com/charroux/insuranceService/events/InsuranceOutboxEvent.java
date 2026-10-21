package com.charroux.insuranceService.events;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "insurance_outbox")
public class InsuranceOutboxEvent {
    @Id private String id;
    @Indexed(name = "ux_insurance_outbox_event_id", unique = true)
    @Field("event_id") private String eventId;
    @Field("event_type") private String eventType;
    @Field("rental_id") private String rentalId;
    private String payload;
    @Field("occurred_at") private Instant occurredAt;
    @Field("created_at") private Instant createdAt;
    @Field("published_at") private Instant publishedAt;
    @Field("next_attempt_at") private Instant nextAttemptAt;
    private int attempts;
    @Field("last_error") private String lastError;

    protected InsuranceOutboxEvent() {}

    public InsuranceOutboxEvent(
            String eventId, String eventType, String rentalId,
            String payload, Instant occurredAt, Instant createdAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.rentalId = rentalId;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
        this.nextAttemptAt = createdAt;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getRentalId() { return rentalId; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public int getAttempts() { return attempts; }

    public void markPublished(Instant when) {
        publishedAt = when;
        lastError = null;
    }

    public void markFailure(String error, Instant when) {
        attempts++;
        lastError = error == null ? "unknown publication failure" : error.substring(0, Math.min(1000, error.length()));
        long delaySeconds = Math.min(300, 1L << Math.min(attempts, 8));
        nextAttemptAt = when.plusSeconds(delaySeconds);
    }
}
