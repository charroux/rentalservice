package com.charroux.carRental.entity;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RentalAgreementRepository extends CrudRepository<RentalAgreement, Long> {

    List<RentalAgreement> findByCustomerId(long customerId);

}
