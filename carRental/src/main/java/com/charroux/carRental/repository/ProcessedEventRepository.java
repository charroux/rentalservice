package com.charroux.carRental.repository;

import com.charroux.carRental.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ProcessedEvent entities.
 *
 * Provides methods for:
 * - Checking if an event was already processed by a consumer (idempotence)
 * - Recording successful event processing
 * - Retrieving retry history for debugging
 * - Cleaning up old processed event records
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    
    /**
     * Check if an event was already processed by a consumer.
     * Used to make repeated delivery idempotent.
     *
     * @param eventId The unique event identifier
     * @param consumerName The consumer that should have processed it
     * @return true if event was previously processed, false otherwise
     */
    boolean existsByEventIdAndConsumerName(String eventId, String consumerName);
    
    /**
     * Get the processing record for an event by a specific consumer.
     *
     * @param eventId The unique event identifier
     * @param consumerName The consumer name
     * @return Optional containing the ProcessedEvent if found
     */
    Optional<ProcessedEvent> findByEventIdAndConsumerName(String eventId, String consumerName);
    
    /**
     * Get all processing records for a specific event (across all consumers).
     * Useful for audit trails.
     *
     * @param eventId The unique event identifier
     * @return List of ProcessedEvent records for this event
     */
    List<ProcessedEvent> findByEventId(String eventId);
    
    /**
     * Get all processing records for a specific consumer.
     * Useful for consumer-specific debugging/monitoring.
     *
     * @param consumerName The consumer name
     * @return List of ProcessedEvent records by this consumer
     */
    List<ProcessedEvent> findByConsumerName(String consumerName);
    
    /**
     * Get high-retry events that have been retried more than a threshold.
     * Used for alerting on consumer issues.
     *
     * @param retryCountThreshold The minimum retry count
     * @return List of ProcessedEvent records with high retry counts
     */
    @Query("SELECT pe FROM ProcessedEvent pe WHERE pe.retryCount > :threshold ORDER BY pe.retryCount DESC")
    List<ProcessedEvent> findHighRetryEvents(@Param("threshold") Integer retryCountThreshold);
    
    /**
     * Get events with errors (that ended up in DLQ).
     * Used for monitoring and manual intervention.
     *
     * @return List of ProcessedEvent records with error messages
     */
    @Query("SELECT pe FROM ProcessedEvent pe WHERE pe.errorMessage IS NOT NULL ORDER BY pe.processedAt DESC")
    List<ProcessedEvent> findEventsWithErrors();
    
    /**
     * Clean up old processed event records (data retention policy).
     * Called periodically to maintain reasonable table size.
     * Keeps records for N days, then deletes for events that were processed successfully.
     *
     * @param olderThan LocalDateTime threshold (e.g., 30 days ago)
     * @return Number of records deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ProcessedEvent pe WHERE pe.processedAt < :olderThan AND pe.errorMessage IS NULL")
    long deleteProcessedEventsBefore(@Param("olderThan") LocalDateTime olderThan);
    
    /**
     * Record a successfully processed event (convenience method).
     * 
     * @param eventId The unique event identifier
     * @param consumerName The name of the consumer that processed it
     * @param eventType The type of event for audit purposes
     */
    default void recordProcessed(String eventId, String consumerName, String eventType) {
        ProcessedEvent event = new ProcessedEvent();
        event.setEventId(eventId);
        event.setConsumerName(consumerName);
        event.setEventType(eventType);
        event.setProcessedAt(LocalDateTime.now());
        event.setRetryCount(0);
        event.setErrorMessage(null);
        this.save(event);
    }
}
