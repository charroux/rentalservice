package com.charroux.delivery.consumer;

import com.charroux.delivery.event.CarRentedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for CarRentedEvent
 * Listens for rental confirmations and processes delivery requests
 */
@Service
public class CarRentedEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(CarRentedEventConsumer.class);
    
    @KafkaListener(topics = "car-rented-events", groupId = "delivery-service-group")
    public void consumeCarRentedEvent(CarRentedEvent event) {
        logger.info("========================================");
        logger.info("Received CarRentedEvent from Kafka:");
        logger.info("  Car: {} {} (Plate: {})", event.getBrand(), event.getModel(), event.getPlateNumber());
        logger.info("  Customer: {} {} ({})", event.getCustomerFirstName(), event.getCustomerLastName(), event.getCustomerEmail());
        logger.info("  Pricing: Original={}€, Final={}€, Discount={}€, Applied={}", 
                event.getOriginalPrice(), event.getFinalPrice(), event.getDiscountAmount(), event.getDiscountApplied());
        logger.info("  Rental confirmed at: {}", event.getRentalConfirmedAt());
        logger.info("========================================");
        
        // TODO: Implement delivery logic here
        // - Schedule car preparation
        // - Send confirmation email to customer
        // - Update delivery tracking system
        // - etc.
        
        logger.info("Delivery process initiated for car {}", event.getPlateNumber());
    }
}
