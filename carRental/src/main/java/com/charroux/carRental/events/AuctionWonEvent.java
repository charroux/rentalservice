package com.charroux.carRental.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when an auction is won and a car is assigned to a rental company.
 * This event is published to Redis Streams for consumption by partner services.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionWonEvent {
    
    private String eventId;              // Unique event identifier
    private String rentalId;             // Unique rental identifier
    private Long carId;                  // Database ID of the car
    private String plateNumber;          // License plate of the car
    private String customerId;           // Customer who won the auction
    private String carBrand;             // Car brand (e.g., Ferrari)
    private String carModel;             // Car model (e.g., F8)
    private Integer finalPrice;          // Final price after auction
    private Integer originalPrice;       // Original price before discount
    private Integer discount;            // Discount amount
    private LocalDateTime rentalStartDate;  // Rental start date
    private LocalDateTime rentalEndDate;    // Rental end date
    private Long timestamp;              // Event timestamp in milliseconds
    
    /**
     * Create a new AuctionWonEvent with a generated event ID
     */
    public static AuctionWonEvent create(
            String rentalId,
            Long carId,
            String plateNumber,
            String customerId,
            String carBrand,
            String carModel,
            Integer finalPrice,
            Integer originalPrice,
            Integer discount) {
        
        AuctionWonEvent event = new AuctionWonEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setRentalId(rentalId);
        event.setCarId(carId);
        event.setPlateNumber(plateNumber);
        event.setCustomerId(customerId);
        event.setCarBrand(carBrand);
        event.setCarModel(carModel);
        event.setFinalPrice(finalPrice);
        event.setOriginalPrice(originalPrice);
        event.setDiscount(discount);
        event.setTimestamp(System.currentTimeMillis());
        event.setRentalStartDate(LocalDateTime.now());
        event.setRentalEndDate(LocalDateTime.now().plusDays(5));
        
        return event;
    }
}
