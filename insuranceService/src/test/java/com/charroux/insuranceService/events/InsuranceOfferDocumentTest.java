package com.charroux.insuranceService.events;

import com.charroux.insuranceService.entity.InsuranceOffer;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InsuranceOfferDocumentTest {
    @Test
    void definesUniqueEventAndRentalIndexes() throws Exception {
        assertTrue(InsuranceOffer.class.getDeclaredField("eventId").getAnnotation(Indexed.class).unique());
        assertTrue(InsuranceOffer.class.getDeclaredField("rentalId").getAnnotation(Indexed.class).unique());
        assertTrue(InsuranceOutboxEvent.class.getDeclaredField("eventId").getAnnotation(Indexed.class).unique());
    }

    @Test
    void acceptanceIsAnIdempotentAggregateTransition() {
        InsuranceOffer offer = new InsuranceOffer(
            "auction-1", "RENT-1",
            new InsuranceOffer.Vehicle(42L, "AA-123-AA", "Tesla", "Model 3"),
            62, 65, 3, 7, Instant.parse("2026-10-20T09:00:00Z"));

        assertTrue(offer.accept(Instant.parse("2026-10-20T09:05:00Z")));
        String acceptedEventId = offer.getAcceptedEventId();
        assertFalse(offer.accept(Instant.parse("2026-10-20T09:06:00Z")));
        assertEquals(acceptedEventId, offer.getAcceptedEventId());
    }
}
