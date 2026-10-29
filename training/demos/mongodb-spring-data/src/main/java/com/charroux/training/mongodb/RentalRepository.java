package com.charroux.training.mongodb;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RentalRepository extends MongoRepository<Rental, String> {
    Optional<Rental> findByRentalId(String rentalId);
    List<Rental> findByCustomerIdAndStatus(String customerId, Rental.Status status);
}
