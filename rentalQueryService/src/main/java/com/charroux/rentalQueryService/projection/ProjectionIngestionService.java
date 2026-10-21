package com.charroux.rentalQueryService.projection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.DateTimeException;

@Service
public class ProjectionIngestionService {
    private final ProjectionEventArchiveRepository archiveRepository;
    private final RentalProjectionProjector projector;
    private final ObjectMapper objectMapper;

    public ProjectionIngestionService(
            ProjectionEventArchiveRepository archiveRepository,
            RentalProjectionProjector projector,
            ObjectMapper objectMapper) {
        this.archiveRepository = archiveRepository;
        this.projector = projector;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean ingest(String rawEvent) {
        EventMetadata metadata = metadata(rawEvent);
        if (archiveRepository.findByEventId(metadata.eventId()).isPresent()) return false;

        ProjectionEventArchive archived = archiveRepository.save(new ProjectionEventArchive(
            metadata.eventId(), metadata.eventType(), metadata.eventVersion(), metadata.rentalId(),
            rawEvent, metadata.occurredAt(), Instant.now()
        ));
        projector.apply(rawEvent);
        archived.markApplied(Instant.now());
        archiveRepository.save(archived);
        return true;
    }

    private EventMetadata metadata(String rawEvent) {
        try {
            JsonNode event = objectMapper.readTree(rawEvent);
            String eventId = requiredText(event, "eventId");
            String eventType = requiredText(event, "eventType");
            int version = requiredInt(event, "eventVersion");
            String rentalId = requiredText(event, "rentalId");
            Instant occurredAt = "AuctionWon".equals(eventType)
                ? Instant.ofEpochMilli(requiredLong(event, "timestamp"))
                : Instant.parse(requiredText(event, "occurredAt"));
            return new EventMetadata(eventId, eventType, version, rentalId, occurredAt);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid event JSON", e);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid event date", e);
        }
    }

    private static String requiredText(JsonNode event, String field) {
        String value = event.path(field).asText(null);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing required field: " + field);
        return value;
    }

    private static int requiredInt(JsonNode event, String field) {
        JsonNode value = event.path(field);
        if (!value.isIntegralNumber()) throw new IllegalArgumentException("Missing integer field: " + field);
        return value.asInt();
    }

    private static long requiredLong(JsonNode event, String field) {
        JsonNode value = event.path(field);
        if (!value.isIntegralNumber()) throw new IllegalArgumentException("Missing integer field: " + field);
        return value.asLong();
    }

    private record EventMetadata(
        String eventId, String eventType, int eventVersion, String rentalId, Instant occurredAt
    ) {}
}
