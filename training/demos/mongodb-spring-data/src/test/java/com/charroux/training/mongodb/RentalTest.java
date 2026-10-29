package com.charroux.training.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

class RentalTest {
    @Test
    void declaresTheCollectionAndUniqueBusinessKey() throws Exception {
        assertThat(Rental.class.getAnnotation(Document.class).collection()).isEqualTo("rentals");
        Indexed indexed = Rental.class.getDeclaredField("rentalId").getAnnotation(Indexed.class);
        assertThat(indexed).isNotNull();
        assertThat(indexed.unique()).isTrue();
    }

    @Test
    void keepsTheVehicleInsideTheAggregate() {
        Rental rental = new Rental(null, "RENT-42", "CUSTOMER-7",
                new Rental.Vehicle("EDU-042", "SUV"), Rental.Status.OPEN,
                List.of("GPS"), new Rental.Price(new BigDecimal("149.90"), "EUR"),
                Instant.parse("2026-10-21T08:00:00Z"));

        assertThat(rental.vehicle().plateNumber()).isEqualTo("EDU-042");
        assertThat(rental.price().amount()).isEqualByComparingTo("149.90");
    }
}
