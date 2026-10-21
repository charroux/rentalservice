package com.charroux.insuranceService.events;

import com.charroux.insuranceService.entity.InsuranceOffer;
import com.charroux.insuranceService.repository.InsuranceOfferRepository;
import com.charroux.insuranceService.service.InsuranceOfferService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class InsuranceOfferServiceTest {
    @Test
    void repeatedAcceptancePersistsOnceAndRepublishesStableEvent() {
        InsuranceOfferRepository repository = mock(InsuranceOfferRepository.class);
        InsuranceOfferEventPublisher publisher = mock(InsuranceOfferEventPublisher.class);
        InsuranceOffer offer = new InsuranceOffer(
            "auction-1", "RENT-1",
            new InsuranceOffer.Vehicle(42L, "AA-123-AA", "Tesla", "Model 3"),
            62, 65, 3, 7, Instant.parse("2026-10-20T09:00:00Z"));
        when(repository.findByRentalId("RENT-1")).thenReturn(Optional.of(offer));
        when(repository.save(offer)).thenReturn(offer);
        InsuranceOfferService service = new InsuranceOfferService(repository, publisher);

        service.accept("RENT-1");
        String acceptedEventId = offer.getAcceptedEventId();
        service.accept("RENT-1");

        verify(repository, times(1)).save(offer);
        verify(publisher, times(2)).publishAccepted(offer);
        assertEquals(acceptedEventId, offer.getAcceptedEventId());
    }
}
