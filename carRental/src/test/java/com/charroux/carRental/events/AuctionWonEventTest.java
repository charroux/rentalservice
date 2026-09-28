package com.charroux.carRental.events;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuctionWonEventTest {
    
    @Test
    void testAuctionWonEventCreation() {
        // When
        AuctionWonEvent event = AuctionWonEvent.create(
            "RENT-001",
            1L,
            "ABC-123",
            "CUST-001",
            "Ferrari",
            "F8",
            850,
            1000,
            150
        );
        
        // Then
        assertNotNull(event.getEventId());
        assertEquals("RENT-001", event.getRentalId());
        assertEquals(1L, event.getCarId());
        assertEquals("ABC-123", event.getPlateNumber());
        assertEquals("CUST-001", event.getCustomerId());
        assertEquals("Ferrari", event.getCarBrand());
        assertEquals("F8", event.getCarModel());
        assertEquals(850, event.getFinalPrice());
        assertEquals(1000, event.getOriginalPrice());
        assertEquals(150, event.getDiscount());
        assertNotNull(event.getTimestamp());
        assertNotNull(event.getRentalStartDate());
        assertNotNull(event.getRentalEndDate());
    }
    
    @Test
    void testAuctionWonEventRentalDates() {
        // When
        AuctionWonEvent event = AuctionWonEvent.create(
            "RENT-002",
            2L,
            "DEF-456",
            "CUST-002",
            "Porsche",
            "911",
            650,
            900,
            250
        );
        
        // Then
        LocalDateTime startDate = event.getRentalStartDate();
        LocalDateTime endDate = event.getRentalEndDate();
        
        assertNotNull(startDate);
        assertNotNull(endDate);
        assertTrue(endDate.isAfter(startDate));
        
        // Use ChronoUnit for robust date difference calculation
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        assertEquals(5L, daysBetween);
    }
    
    @Test
    void testAuctionWonEventConstructors() {
        // Given
        AuctionWonEvent event1 = new AuctionWonEvent();
        assertNull(event1.getEventId());
        
        // When
        event1.setEventId("TEST-123");
        event1.setRentalId("RENT-003");
        event1.setCarBrand("Tesla");
        
        // Then
        assertEquals("TEST-123", event1.getEventId());
        assertEquals("RENT-003", event1.getRentalId());
        assertEquals("Tesla", event1.getCarBrand());
    }
}
