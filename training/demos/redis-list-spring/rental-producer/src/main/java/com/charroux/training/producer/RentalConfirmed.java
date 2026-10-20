package com.charroux.training.producer;

import java.time.Instant;

public record RentalConfirmed(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String rentalId,
        String vehicleId,
        int dailyPrice) {
}
