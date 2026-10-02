package com.charroux.insuranceService.web;

import com.charroux.insuranceService.service.InsuranceOfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/insurance/offers")
@CrossOrigin(origins = "http://localhost:4200")
public class InsuranceOfferController {

    private final InsuranceOfferService service;

    public InsuranceOfferController(InsuranceOfferService service) {
        this.service = service;
    }

    @GetMapping("/{rentalId}")
    public ResponseEntity<InsuranceOfferResponse> getOffer(@PathVariable String rentalId) {
        return service.findByRentalId(rentalId)
            .map(InsuranceOfferResponse::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{rentalId}/accept")
    public ResponseEntity<InsuranceOfferResponse> acceptOffer(@PathVariable String rentalId) {
        return service.accept(rentalId)
            .map(InsuranceOfferResponse::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
