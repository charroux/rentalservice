package com.charroux.training.producer;

public record RentalRequest(String rentalId, String vehicleId, int dailyPrice) {
}
