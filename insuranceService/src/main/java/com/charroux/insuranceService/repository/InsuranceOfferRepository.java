package com.charroux.insuranceService.repository;

import com.charroux.insuranceService.entity.InsuranceOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsuranceOfferRepository extends JpaRepository<InsuranceOffer, Long> {

    boolean existsByEventId(String eventId);

    Optional<InsuranceOffer> findByRentalId(String rentalId);
}
