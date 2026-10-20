package com.charroux.training.consumer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/insurance/offers")
public class InsuranceController {

    private final InsuranceOfferStore store;

    public InsuranceController(InsuranceOfferStore store) {
        this.store = store;
    }

    @GetMapping("/{rentalId}")
    public ResponseEntity<InsuranceOffer> find(@PathVariable String rentalId) {
        return store.findByRentalId(rentalId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
