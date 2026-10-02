package com.charroux.insuranceService.events;

import com.charroux.insuranceService.entity.InsuranceOffer;
import com.charroux.insuranceService.repository.InsuranceOfferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisListCommands.Direction;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsuranceEventConsumerTest {

    private static final String EVENT = """
        {"eventId":"event-1","rentalId":"RENT-1","plateNumber":"AA-123-AA","finalPrice":62}
        """;

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ListOperations<String, String> lists;
    @Mock
    private InsuranceOfferRepository repository;

    private InsuranceEventConsumer consumer;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(lists);
        consumer = new InsuranceEventConsumer(redisTemplate, new ObjectMapper(), repository);
    }

    @Test
    void createsAndAcknowledgesInsuranceOffer() {
        when(lists.move(
            InsuranceEventConsumer.READY_QUEUE,
            Direction.LEFT,
            InsuranceEventConsumer.PROCESSING_QUEUE,
            Direction.RIGHT
        )).thenReturn(EVENT);
        when(repository.existsByEventId("event-1")).thenReturn(false);
        when(repository.save(any(InsuranceOffer.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(lists.remove(InsuranceEventConsumer.PROCESSING_QUEUE, 1, EVENT)).thenReturn(1L);

        consumer.consumeEvents();

        ArgumentCaptor<InsuranceOffer> offer = ArgumentCaptor.forClass(InsuranceOffer.class);
        verify(repository).save(offer.capture());
        assertEquals("RENT-1", offer.getValue().getRentalId());
        assertEquals(7, offer.getValue().getDailyPremium());
        verify(lists).remove(InsuranceEventConsumer.PROCESSING_QUEUE, 1, EVENT);
    }

    @Test
    void acknowledgesDuplicateWithoutCreatingAnotherOffer() {
        when(lists.move(
            InsuranceEventConsumer.READY_QUEUE,
            Direction.LEFT,
            InsuranceEventConsumer.PROCESSING_QUEUE,
            Direction.RIGHT
        )).thenReturn(EVENT);
        when(repository.existsByEventId("event-1")).thenReturn(true);
        when(lists.remove(InsuranceEventConsumer.PROCESSING_QUEUE, 1, EVENT)).thenReturn(1L);

        consumer.consumeEvents();

        verify(repository, never()).save(any());
        verify(lists).remove(InsuranceEventConsumer.PROCESSING_QUEUE, 1, EVENT);
    }

    @Test
    void leavesInvalidEventRecoverable() {
        String invalid = "{\"eventId\":\"event-2\"}";
        when(lists.move(
            InsuranceEventConsumer.READY_QUEUE,
            Direction.LEFT,
            InsuranceEventConsumer.PROCESSING_QUEUE,
            Direction.RIGHT
        )).thenReturn(invalid);

        consumer.consumeEvents();

        verify(lists, never()).remove(anyString(), anyLong(), anyString());
        verify(repository, never()).save(any());
    }
}
