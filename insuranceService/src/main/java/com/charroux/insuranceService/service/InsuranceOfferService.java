package com.charroux.insuranceService.service;

import com.charroux.insuranceService.entity.InsuranceOffer;
import com.charroux.insuranceService.events.InsuranceOfferEventPublisher;
import com.charroux.insuranceService.repository.InsuranceOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class InsuranceOfferService {

    private final InsuranceOfferRepository repository;
    private final InsuranceOfferEventPublisher eventPublisher;

    public InsuranceOfferService(
            InsuranceOfferRepository repository,
            InsuranceOfferEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public Optional<InsuranceOffer> findByRentalId(String rentalId) {
        return repository.findByRentalId(rentalId);
    }

    @Transactional
    public Optional<InsuranceOffer> accept(String rentalId) {
        return repository.findByRentalId(rentalId).map(offer -> {
            if (offer.accept(Instant.now())) {
                offer = repository.save(offer);
            }
            // Repeated commands stage the same stable outbox identity. The unique
            // outbox index makes this call safe and preserves the original event.
            eventPublisher.publishAccepted(offer);
            return offer;
        });
    }
}
