package com.charroux.training.mongodb;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "rentals")
public record Rental(
        @Id String id,
        @Indexed(unique = true) @Field("rental_id") String rentalId,
        @Indexed @Field("customer_id") String customerId,
        Vehicle vehicle,
        Status status,
        List<String> options,
        Price price,
        @Field("created_at") Instant createdAt) {

    public enum Status { OPEN, CLOSED }

    public record Vehicle(
            @Field("plate_number") String plateNumber,
            String category) {}

    public record Price(BigDecimal amount, String currency) {}
}
