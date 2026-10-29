package com.charroux.training.mongodb;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rentals")
public class RentalController {
    private final RentalRepository repository;

    public RentalController(RentalRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    ResponseEntity<Rental> create(@RequestBody Rental rental) {
        Rental saved = repository.save(new Rental(
                null, rental.rentalId(), rental.customerId(), rental.vehicle(),
                rental.status(), rental.options(), rental.price(), Instant.now()));
        return ResponseEntity.created(URI.create("/rentals/" + saved.rentalId())).body(saved);
    }

    @GetMapping("/{rentalId}")
    ResponseEntity<Rental> findByRentalId(@PathVariable String rentalId) {
        return repository.findByRentalId(rentalId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    List<Rental> findByCustomerAndStatus(
            @RequestParam String customerId,
            @RequestParam Rental.Status status) {
        return repository.findByCustomerIdAndStatus(customerId, status);
    }
}
