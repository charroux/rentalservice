package com.charroux.rentalQueryService.projection;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDateTime;

@Document(collection = "rental_views")
public class RentalView {
    @Id private String id;
    @Indexed(name = "ux_rental_view_rental_id", unique = true)
    @Field("rental_id") private String rentalId;

    private Rental rental;
    private Insurance insurance;

    @Field("updated_at") private Instant updatedAt;
    @Version private Long version;

    protected RentalView() {}

    public RentalView(String rentalId) {
        this.rentalId = rentalId;
    }

    public void applyAuction(
            String eventId, Instant occurredAt, Long carId, String plateNumber,
            String customerId, String carBrand, String carModel, Integer finalPrice,
            Integer originalPrice, Integer discount, LocalDateTime start, LocalDateTime end) {
        if (rental == null || !occurredAt.isBefore(rental.occurredAt)) {
            rental = new Rental(eventId, occurredAt, carId, plateNumber, customerId,
                carBrand, carModel, finalPrice, originalPrice, discount, start, end);
        }
        record(eventId, occurredAt);
    }

    public void applyInsurance(
            String eventId, Instant occurredAt, String sourceAuctionEventId,
            Long carId, String plateNumber, String carBrand, String carModel,
            Integer rentalDailyPrice, Integer originalDailyPrice, Integer discount,
            Integer dailyPremium, String status) {
        if (insurance == null) {
            insurance = new Insurance();
        }
        insurance.sourceAuctionEventId = sourceAuctionEventId;
        insurance.carId = carId;
        insurance.plateNumber = plateNumber;
        insurance.carBrand = carBrand;
        insurance.carModel = carModel;
        insurance.rentalDailyPrice = rentalDailyPrice;
        insurance.originalDailyPrice = originalDailyPrice;
        insurance.discount = discount;
        insurance.dailyPremium = dailyPremium;
        if ("ACCEPTED".equals(status)) {
            if (insurance.acceptedAt == null || !occurredAt.isBefore(insurance.acceptedAt)) {
                insurance.status = "ACCEPTED";
                insurance.acceptedEventId = eventId;
                insurance.acceptedAt = occurredAt;
            }
        } else {
            if (insurance.proposedAt == null || !occurredAt.isBefore(insurance.proposedAt)) {
                insurance.proposedEventId = eventId;
                insurance.proposedAt = occurredAt;
            }
            if (!"ACCEPTED".equals(insurance.status)) {
                insurance.status = "PROPOSED";
            }
        }
        record(eventId, occurredAt);
    }

    private void record(String eventId, Instant occurredAt) {
        if (updatedAt == null || occurredAt.isAfter(updatedAt)) {
            updatedAt = occurredAt;
        }
    }

    public String getId() { return id; }
    public String getRentalId() { return rentalId; }
    public Rental getRental() { return rental; }
    public Insurance getInsurance() { return insurance; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean hasRental() { return rental != null; }

    public record Rental(
        String eventId, Instant occurredAt, Long carId, String plateNumber,
        String customerId, String carBrand, String carModel, Integer finalPrice,
        Integer originalPrice, Integer discount, LocalDateTime rentalStartDate,
        LocalDateTime rentalEndDate
    ) {}

    public static class Insurance {
        private String sourceAuctionEventId;
        private Long carId;
        private String plateNumber;
        private String carBrand;
        private String carModel;
        private Integer rentalDailyPrice;
        private Integer originalDailyPrice;
        private Integer discount;
        private Integer dailyPremium;
        private String status;
        private String proposedEventId;
        private String acceptedEventId;
        private Instant proposedAt;
        private Instant acceptedAt;

        public String getSourceAuctionEventId() { return sourceAuctionEventId; }
        public Long getCarId() { return carId; }
        public String getPlateNumber() { return plateNumber; }
        public String getCarBrand() { return carBrand; }
        public String getCarModel() { return carModel; }
        public Integer getRentalDailyPrice() { return rentalDailyPrice; }
        public Integer getOriginalDailyPrice() { return originalDailyPrice; }
        public Integer getDiscount() { return discount; }
        public Integer getDailyPremium() { return dailyPremium; }
        public String getStatus() { return status; }
        public String getProposedEventId() { return proposedEventId; }
        public String getAcceptedEventId() { return acceptedEventId; }
        public Instant getProposedAt() { return proposedAt; }
        public Instant getAcceptedAt() { return acceptedAt; }
    }
}
