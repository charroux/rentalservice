package com.charroux.training.consumer;

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
