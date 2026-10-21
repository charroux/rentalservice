package com.charroux.insuranceService.service;

import com.charroux.insuranceService.entity.InsuranceOffer;
import com.charroux.insuranceService.events.InsuranceOfferEventPublisher;
import com.charroux.insuranceService.repository.InsuranceOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsuranceOfferCreationService {
    private final InsuranceOfferRepository repository;
    private final InsuranceOfferEventPublisher eventPublisher;

    public InsuranceOfferCreationService(
            InsuranceOfferRepository repository,
            InsuranceOfferEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public InsuranceOffer createOrReplay(InsuranceOffer candidate) {
        InsuranceOffer offer = repository.findByEventId(candidate.getEventId())
            .or(() -> repository.findByRentalId(candidate.getRentalId()))
            .orElseGet(() -> repository.save(candidate));
        eventPublisher.publishProposed(offer);
        return offer;
    }
}
