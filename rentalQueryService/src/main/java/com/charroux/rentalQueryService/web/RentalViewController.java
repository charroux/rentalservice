package com.charroux.rentalQueryService.web;

import com.charroux.rentalQueryService.projection.RentalView;
import com.charroux.rentalQueryService.projection.RentalViewRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/rental-views")
@CrossOrigin(origins = "http://localhost:4200")
public class RentalViewController {
    private final RentalViewRepository repository;

    public RentalViewController(RentalViewRepository repository) { this.repository = repository; }

    @GetMapping("/{rentalId}")
    public ResponseEntity<RentalViewResponse> find(@PathVariable String rentalId) {
        return repository.findByRentalId(rentalId)
            .map(RentalViewResponse::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record RentalViewResponse(
        String rentalId,
        String projectionState,
        RentalView.Rental rental,
        RentalView.Insurance insurance,
        Instant updatedAt
    ) {
        static RentalViewResponse from(RentalView view) {
            return new RentalViewResponse(view.getRentalId(), view.hasRental() ? "COMPLETE" : "PARTIAL",
                view.getRental(), view.getInsurance(), view.getUpdatedAt());
        }
    }
}
