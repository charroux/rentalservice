package com.charroux.training.producer;

import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rentals")
public class RentalController {

    private final RentalEventPublisher publisher;

    public RentalController(RentalEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PublicationResponse create(@RequestBody RentalRequest request) {
        RentalConfirmed event = new RentalConfirmed(
            UUID.randomUUID().toString(),
            "RentalConfirmed",
            1,
            Instant.now(),
            request.rentalId(),
            request.vehicleId(),
            request.dailyPrice()
        );
        long queueSize = publisher.publish(event);
        return new PublicationResponse(event.eventId(), queueSize);
    }

    public record PublicationResponse(String eventId, long queueSize) {
    }
}
