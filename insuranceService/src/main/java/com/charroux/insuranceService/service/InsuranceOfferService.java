package com.charroux.insuranceService.service;

import com.charroux.insuranceService.entity.InsuranceOffer;
import com.charroux.insuranceService.repository.InsuranceOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class InsuranceOfferService {

    private final InsuranceOfferRepository repository;

    public InsuranceOfferService(InsuranceOfferRepository repository) {
        this.repository = repository;
    }

    public Optional<InsuranceOffer> findByRentalId(String rentalId) {
        return repository.findByRentalId(rentalId);
    }

    @Transactional
    public Optional<InsuranceOffer> accept(String rentalId) {
        return repository.findByRentalId(rentalId).map(offer -> {
            offer.accept();
            return offer;
        });
    }
}
