package com.charroux.rentalQueryService.projection;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "projection_event_archive")
public class ProjectionEventArchive {
    @Id private String id;
    @Indexed(name = "ux_projection_archive_event_id", unique = true)
    @Field("event_id") private String eventId;
    @Field("event_type") private String eventType;
    @Field("event_version") private int eventVersion;
    @Indexed(name = "ix_projection_archive_rental_id")
    @Field("rental_id") private String rentalId;
    private String payload;
    @Indexed(name = "ix_projection_archive_occurred_at")
    @Field("occurred_at") private Instant occurredAt;
    @Field("received_at") private Instant receivedAt;
    @Field("applied_at") private Instant appliedAt;

    protected ProjectionEventArchive() {}

    public ProjectionEventArchive(
            String eventId, String eventType, int eventVersion, String rentalId,
            String payload, Instant occurredAt, Instant receivedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.rentalId = rentalId;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
    }

    public String getEventId() { return eventId; }
    public String getPayload() { return payload; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getAppliedAt() { return appliedAt; }
    public void markApplied(Instant when) { appliedAt = when; }
}
