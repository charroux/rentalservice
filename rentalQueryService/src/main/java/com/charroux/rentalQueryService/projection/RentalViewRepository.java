package com.charroux.rentalQueryService.projection;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface RentalViewRepository extends MongoRepository<RentalView, String> {
    Optional<RentalView> findByRentalId(String rentalId);
}
