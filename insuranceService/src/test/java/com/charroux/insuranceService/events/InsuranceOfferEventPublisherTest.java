package com.charroux.insuranceService.events;

import com.charroux.insuranceService.entity.InsuranceOffer;
import com.charroux.insuranceService.repository.InsuranceOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class InsuranceOfferEventPublisherTest {
    @Test
    void stagesVersionedEnvelopeWithPersistedIdentityAndOccurredAt() throws Exception {
        InsuranceOutboxRepository repository = mock(InsuranceOutboxRepository.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        InsuranceOffer offer = new InsuranceOffer(
            "auction-1", "RENT-1",
            new InsuranceOffer.Vehicle(42L, "AA-123-AA", "Tesla", "Model 3"),
            62, 65, 3, 7, Instant.parse("2026-10-20T09:00:00Z"));

        new InsuranceOfferEventPublisher(repository, mapper).publishProposed(offer);

        ArgumentCaptor<InsuranceOutboxEvent> outbox = ArgumentCaptor.forClass(InsuranceOutboxEvent.class);
        verify(repository).save(outbox.capture());
        var event = mapper.readTree(outbox.getValue().getPayload());
        assertEquals("InsuranceOfferProposed", event.path("eventType").asText());
        assertEquals(1, event.path("eventVersion").asInt());
        assertEquals(offer.getProposedEventId(), event.path("eventId").asText());
        assertEquals("2026-10-20T09:00:00Z", event.path("occurredAt").asText());
    }
}
