package com.charroux.insuranceService.repository;

import com.charroux.insuranceService.entity.InsuranceOffer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface InsuranceOfferRepository extends MongoRepository<InsuranceOffer, String> {

    boolean existsByEventId(String eventId);

    Optional<InsuranceOffer> findByRentalId(String rentalId);

    Optional<InsuranceOffer> findByEventId(String eventId);
}
