package com.charroux.insuranceService.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "insurance_offers")
public class InsuranceOffer {

    @Id
    private String id;

    /** Source AuctionWon identity: the durable idempotence key. */
    @Indexed(name = "ux_insurance_offer_event_id", unique = true)
    @Field("event_id")
    private String eventId;

    /** Business rule: at most one current insurance offer per rental. */
    @Indexed(name = "ux_insurance_offer_rental_id", unique = true)
    @Field("rental_id")
    private String rentalId;

    private Vehicle vehicle;

    @Field("rental_daily_price")
    private Integer rentalDailyPrice;

    @Field("original_daily_price")
    private Integer originalDailyPrice;

    private Integer discount;

    @Field("daily_premium")
    private Integer dailyPremium;

    private InsuranceOfferStatus status;

    @Field("proposed_event_id")
    private String proposedEventId;

    @Field("accepted_event_id")
    private String acceptedEventId;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    @Field("accepted_at")
    private Instant acceptedAt;

    @Version
    private Long version;

    protected InsuranceOffer() {
    }

    public InsuranceOffer(
            String eventId,
            String rentalId,
            Vehicle vehicle,
            Integer rentalDailyPrice,
            Integer originalDailyPrice,
            Integer discount,
            Integer dailyPremium,
            Instant createdAt) {
        this.eventId = eventId;
        this.rentalId = rentalId;
        this.vehicle = vehicle;
        this.rentalDailyPrice = rentalDailyPrice;
        this.originalDailyPrice = originalDailyPrice;
        this.discount = discount;
        this.dailyPremium = dailyPremium;
        this.status = InsuranceOfferStatus.PROPOSED;
        this.proposedEventId = UUID.randomUUID().toString();
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getRentalId() { return rentalId; }
    public Vehicle getVehicle() { return vehicle; }
    public String getPlateNumber() { return vehicle.plateNumber(); }
    public Integer getRentalPrice() { return rentalDailyPrice; }
    public Integer getRentalDailyPrice() { return rentalDailyPrice; }
    public Integer getOriginalDailyPrice() { return originalDailyPrice; }
    public Integer getDiscount() { return discount; }
    public Integer getDailyPremium() { return dailyPremium; }
    public InsuranceOfferStatus getStatus() { return status; }
    public String getProposedEventId() { return proposedEventId; }
    public String getAcceptedEventId() { return acceptedEventId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }

    public boolean accept(Instant when) {
        if (status == InsuranceOfferStatus.ACCEPTED) {
            return false;
        }
        status = InsuranceOfferStatus.ACCEPTED;
        acceptedEventId = UUID.randomUUID().toString();
        acceptedAt = when;
        updatedAt = when;
        return true;
    }

    public record Vehicle(
        @Field("car_id") Long carId,
        @Field("plate_number") String plateNumber,
        String brand,
        String model
    ) {
    }
}
