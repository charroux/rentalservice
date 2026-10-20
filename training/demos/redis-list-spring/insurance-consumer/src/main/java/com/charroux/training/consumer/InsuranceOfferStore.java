package com.charroux.training.consumer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InsuranceOfferStore {

    private final Map<String, InsuranceOffer> offers = new ConcurrentHashMap<>();

    public void save(InsuranceOffer offer) {
        offers.put(offer.rentalId(), offer);
    }

    public Optional<InsuranceOffer> findByRentalId(String rentalId) {
        return Optional.ofNullable(offers.get(rentalId));
    }
}
