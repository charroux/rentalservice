package com.charroux.carRental.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionEventPublisherTest {
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ListOperations<String, String> listOperations;
    
    private ObjectMapper objectMapper;
    private AuctionEventPublisher publisher;
    
    @BeforeEach
    void setUp() {
        // Create ObjectMapper with Java 8 date/time support
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        publisher = new AuctionEventPublisher(redisTemplate, objectMapper);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }
    
    @Test
    void testPublishAuctionWonSuccess() {
        // Given
        when(listOperations.rightPush(eq("auction:events:queue"), anyString()))
            .thenReturn(1L);
        when(redisTemplate.expire(eq("auction:events:queue"), eq(24L), eq(TimeUnit.HOURS)))
            .thenReturn(true);
        
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
        
        // When
        boolean result = publisher.publishAuctionWon(event);
        
        // Then
        assertTrue(result);
        verify(listOperations).rightPush(eq("auction:events:queue"), anyString());
        verify(redisTemplate).expire(eq("auction:events:queue"), eq(24L), eq(TimeUnit.HOURS));
    }
    
    @Test
    void testPublishAuctionWonMultipleEvents() {
        // Given
        when(listOperations.rightPush(eq("auction:events:queue"), anyString()))
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L);
        when(redisTemplate.expire(eq("auction:events:queue"), eq(24L), eq(TimeUnit.HOURS)))
            .thenReturn(true);
        
        AuctionWonEvent event1 = AuctionWonEvent.create(
            "RENT-001", 1L, "ABC-123", "CUST-001", "Ferrari", "F8", 850, 1000, 150
        );
        AuctionWonEvent event2 = AuctionWonEvent.create(
            "RENT-002", 2L, "DEF-456", "CUST-002", "Porsche", "911", 650, 900, 250
        );
        AuctionWonEvent event3 = AuctionWonEvent.create(
            "RENT-003", 3L, "GHI-789", "CUST-003", "Tesla", "Model S", 350, 500, 150
        );
        
        // When
        boolean result1 = publisher.publishAuctionWon(event1);
        boolean result2 = publisher.publishAuctionWon(event2);
        boolean result3 = publisher.publishAuctionWon(event3);
        
        // Then
        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
        verify(listOperations, times(3)).rightPush(eq("auction:events:queue"), anyString());
        verify(redisTemplate, times(3)).expire(eq("auction:events:queue"), eq(24L), eq(TimeUnit.HOURS));
    }
    
    @Test
    void testPublishAuctionWonException() {
        // Given
        when(listOperations.rightPush(eq("auction:events:queue"), anyString()))
            .thenThrow(new RuntimeException("Redis connection error"));
        
        AuctionWonEvent event = AuctionWonEvent.create(
            "RENT-003",
            3L,
            "GHI-789",
            "CUST-003",
            "Tesla",
            "Model S",
            350,
            500,
            150
        );
        
        // When
        boolean result = publisher.publishAuctionWon(event);
        
        // Then
        assertFalse(result);
    }
}
