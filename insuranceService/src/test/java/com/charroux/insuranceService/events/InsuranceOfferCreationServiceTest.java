package com.charroux.insuranceService.events;

import com.charroux.insuranceService.entity.InsuranceOffer;
import com.charroux.insuranceService.repository.InsuranceOfferRepository;
import com.charroux.insuranceService.service.InsuranceOfferCreationService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;

class InsuranceOfferCreationServiceTest {
    @Test
    void persistsAggregateAndStagesOutboxEventInOneServiceTransaction() {
        InsuranceOfferRepository repository = mock(InsuranceOfferRepository.class);
        InsuranceOfferEventPublisher publisher = mock(InsuranceOfferEventPublisher.class);
        InsuranceOffer candidate = offer();
        when(repository.findByEventId("auction-1")).thenReturn(Optional.empty());
        when(repository.findByRentalId("RENT-1")).thenReturn(Optional.empty());
        when(repository.save(candidate)).thenReturn(candidate);

        new InsuranceOfferCreationService(repository, publisher).createOrReplay(candidate);

        var order = inOrder(repository, publisher);
        order.verify(repository).save(candidate);
        order.verify(publisher).publishProposed(candidate);
    }

    @Test
    void secondSourceEventForSameRentalKeepsTheSingleCurrentOffer() {
        InsuranceOfferRepository repository = mock(InsuranceOfferRepository.class);
        InsuranceOfferEventPublisher publisher = mock(InsuranceOfferEventPublisher.class);
        InsuranceOffer existing = offer();
        InsuranceOffer candidate = new InsuranceOffer("auction-2", "RENT-1",
            new InsuranceOffer.Vehicle(42L, "AA-123-AA", "Tesla", "Model 3"),
            60, 65, 5, 6, Instant.parse("2026-10-20T10:00:00Z"));
        when(repository.findByEventId("auction-2")).thenReturn(Optional.empty());
        when(repository.findByRentalId("RENT-1")).thenReturn(Optional.of(existing));

        new InsuranceOfferCreationService(repository, publisher).createOrReplay(candidate);

        verify(repository, never()).save(any());
        verify(publisher).publishProposed(existing);
    }

    @Test
    void redeliveryStagesOnlyMissingOutboxIdentityWithoutSecondAggregate() {
        InsuranceOfferRepository repository = mock(InsuranceOfferRepository.class);
        InsuranceOfferEventPublisher publisher = mock(InsuranceOfferEventPublisher.class);
        InsuranceOffer existing = offer();
        when(repository.findByEventId("auction-1")).thenReturn(Optional.of(existing));

        new InsuranceOfferCreationService(repository, publisher).createOrReplay(offer());

        verify(repository, never()).save(any());
        verify(publisher).publishProposed(existing);
    }

    private static InsuranceOffer offer() {
        return new InsuranceOffer("auction-1", "RENT-1",
            new InsuranceOffer.Vehicle(42L, "AA-123-AA", "Tesla", "Model 3"),
            62, 65, 3, 7, Instant.parse("2026-10-20T09:00:00Z"));
    }
}
