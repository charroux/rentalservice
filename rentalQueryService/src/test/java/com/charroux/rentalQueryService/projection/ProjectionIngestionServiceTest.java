package com.charroux.rentalQueryService.projection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectionIngestionServiceTest {
    private static final String EVENT = """
        {"eventType":"AuctionWon","eventVersion":1,"eventId":"auction-1","rentalId":"RENT-42",
         "timestamp":1792486800000}
        """;

    @Test
    void archivesBeforeProjectingAndMarksEventApplied() {
        ProjectionEventArchiveRepository archive = mock(ProjectionEventArchiveRepository.class);
        RentalProjectionProjector projector = mock(RentalProjectionProjector.class);
        when(archive.findByEventId("auction-1")).thenReturn(Optional.empty());
        when(archive.save(any(ProjectionEventArchive.class))).thenAnswer(i -> i.getArgument(0));
        ProjectionIngestionService service = new ProjectionIngestionService(archive, projector, new ObjectMapper());

        assertTrue(service.ingest(EVENT));

        var order = inOrder(archive, projector);
        order.verify(archive).save(any(ProjectionEventArchive.class));
        order.verify(projector).apply(EVENT);
        order.verify(archive).save(argThat(event -> event.getAppliedAt() != null));
    }

    @Test
    void duplicateArchiveIdentitySkipsProjection() {
        ProjectionEventArchiveRepository archive = mock(ProjectionEventArchiveRepository.class);
        RentalProjectionProjector projector = mock(RentalProjectionProjector.class);
        ProjectionEventArchive existing = new ProjectionEventArchive(
            "auction-1", "AuctionWon", 1, "RENT-42", EVENT,
            java.time.Instant.parse("2026-10-20T09:00:00Z"),
            java.time.Instant.parse("2026-10-20T09:00:01Z"));
        when(archive.findByEventId("auction-1")).thenReturn(Optional.of(existing));
        ProjectionIngestionService service = new ProjectionIngestionService(archive, projector, new ObjectMapper());

        assertFalse(service.ingest(EVENT));

        verify(projector, never()).apply(anyString());
        verify(archive, never()).save(any());
    }

    @Test
    void invalidDateIsClassifiedAsAnInvalidEvent() {
        ProjectionEventArchiveRepository archive = mock(ProjectionEventArchiveRepository.class);
        RentalProjectionProjector projector = mock(RentalProjectionProjector.class);
        ProjectionIngestionService service = new ProjectionIngestionService(archive, projector, new ObjectMapper());
        String invalid = """
            {"eventType":"InsuranceOfferProposed","eventVersion":1,"eventId":"offer-1",
             "rentalId":"RENT-42","occurredAt":"not-a-date"}
            """;

        assertThrows(IllegalArgumentException.class, () -> service.ingest(invalid));
        verify(archive, never()).save(any());
        verify(projector, never()).apply(anyString());
    }
}
