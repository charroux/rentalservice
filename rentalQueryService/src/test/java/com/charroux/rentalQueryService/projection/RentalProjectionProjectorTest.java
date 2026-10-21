package com.charroux.rentalQueryService.projection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RentalProjectionProjectorTest {
    private RentalViewRepository repository;
    private RentalProjectionProjector projector;
    private AtomicReference<RentalView> stored;

    @BeforeEach
    void setUp() {
        repository = mock(RentalViewRepository.class);
        stored = new AtomicReference<>();
        when(repository.findByRentalId("RENT-42")).thenAnswer(i -> Optional.ofNullable(stored.get()));
        when(repository.save(any(RentalView.class))).thenAnswer(i -> {
            RentalView value = i.getArgument(0);
            stored.set(value);
            return value;
        });
        projector = new RentalProjectionProjector(repository, new ObjectMapper());
    }

    @Test
    void acceptedBeforeAuctionAndProposalConvergesWithoutStatusRegression() {
        assertTrue(projector.apply(insurance("InsuranceOfferAccepted", "accepted-1", "ACCEPTED", "2026-10-20T09:20:00Z")));
        assertFalse(stored.get().hasRental());
        assertEquals("ACCEPTED", stored.get().getInsurance().getStatus());

        assertTrue(projector.apply(auctionWon()));
        assertTrue(projector.apply(insurance("InsuranceOfferProposed", "proposed-1", "PROPOSED", "2026-10-20T09:15:00Z")));

        RentalView view = stored.get();
        assertTrue(view.hasRental());
        assertEquals("Tesla", view.getRental().carBrand());
        assertEquals(7, view.getInsurance().getDailyPremium());
        assertEquals("ACCEPTED", view.getInsurance().getStatus());
    }

    private static String auctionWon() {
        return """
            {"eventType":"AuctionWon","eventVersion":1,"eventId":"auction-1","rentalId":"RENT-42",
             "carId":42,"plateNumber":"EDU-042","customerId":"CUST-1","carBrand":"Tesla","carModel":"Model 3",
             "finalPrice":62,"originalPrice":65,"discount":3,
             "rentalStartDate":"2026-10-20T09:00:00","rentalEndDate":"2026-10-25T09:00:00",
             "timestamp":1792486800000}
            """;
    }

    private static String insurance(String type, String id, String status, String occurredAt) {
        return """
            {"eventType":"%s","eventVersion":1,"eventId":"%s","rentalId":"RENT-42",
             "sourceAuctionEventId":"auction-1","carId":42,"plateNumber":"EDU-042",
             "carBrand":"Tesla","carModel":"Model 3","rentalDailyPrice":62,
             "originalDailyPrice":65,"discount":3,"dailyPremium":7,
             "status":"%s","occurredAt":"%s"}
            """.formatted(type, id, status, occurredAt);
    }
}
