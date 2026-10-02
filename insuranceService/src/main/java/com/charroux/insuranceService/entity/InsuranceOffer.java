package com.charroux.insuranceService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "insurance_offers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_insurance_event", columnNames = "event_id"),
        @UniqueConstraint(name = "uc_insurance_rental", columnNames = "rental_id")
    }
)
public class InsuranceOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "rental_id", nullable = false, length = 100)
    private String rentalId;

    @Column(name = "plate_number", nullable = false, length = 30)
    private String plateNumber;

    @Column(name = "rental_price", nullable = false)
    private Integer rentalPrice;

    @Column(name = "daily_premium", nullable = false)
    private Integer dailyPremium;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InsuranceOfferStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected InsuranceOffer() {
    }

    public InsuranceOffer(
            String eventId,
            String rentalId,
            String plateNumber,
            Integer rentalPrice,
            Integer dailyPremium) {
        this.eventId = eventId;
        this.rentalId = rentalId;
        this.plateNumber = plateNumber;
        this.rentalPrice = rentalPrice;
        this.dailyPremium = dailyPremium;
        this.status = InsuranceOfferStatus.PROPOSED;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getRentalId() { return rentalId; }
    public String getPlateNumber() { return plateNumber; }
    public Integer getRentalPrice() { return rentalPrice; }
    public Integer getDailyPremium() { return dailyPremium; }
    public InsuranceOfferStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void accept() {
        this.status = InsuranceOfferStatus.ACCEPTED;
    }
}
