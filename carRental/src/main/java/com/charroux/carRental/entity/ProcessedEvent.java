package com.charroux.carRental.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks processed events to ensure idempotent handling.
 *
 * When multiple consumers or retries occur, this table prevents:
 * - Duplicate processing of the same event
 * - Race conditions in distributed systems
 * - Data corruption from concurrent event handlers
 *
 * Pattern: Event ID + Consumer Name = Unique constraint
 * Ensures each consumer processes each event exactly once (idempotence)
 */
@Entity
@Table(
    name = "processed_events",
    indexes = {
        @Index(name = "idx_event_id", columnList = "event_id"),
        @Index(name = "idx_consumer_name", columnList = "consumer_name"),
        @Index(name = "idx_processed_at", columnList = "processed_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uc_event_consumer",
            columnNames = {"event_id", "consumer_name"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Event ID from the event (UUID string).
     * Globally unique event identifier for deduplication.
     */
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;
    
    /**
     * Consumer name that processed this event.
     * Example: "RentalEventConsumer", "InsuranceEventConsumer"
     * Ensures different services can process same event independently.
     */
    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;
    
    /**
     * Event type (for debugging and auditing).
     * Example: "AuctionWon", "AuctionLost", "AuctionExtended"
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    /**
     * Timestamp when this event was processed.
     * Useful for tracking event lag and processing delays.
     */
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
    
    /**
     * Retry count if event was reprocessed after failure.
     * Default = 0 (first attempt successful)
     * Used for monitoring and alerting on high retry rates.
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    /**
     * Error message if processing failed (before DLQ).
     * NULL if processing successful.
     * Populated when event moved to DLQ after MAX_RETRIES exceeded.
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    // Constructor for successful processing (no error)
    public ProcessedEvent(String eventId, String consumerName, String eventType, Integer retryCount) {
        this.eventId = eventId;
        this.consumerName = consumerName;
        this.eventType = eventType;
        this.processedAt = LocalDateTime.now();
        this.retryCount = retryCount;
    }
}
