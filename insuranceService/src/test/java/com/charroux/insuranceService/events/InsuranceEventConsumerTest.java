package com.charroux.insuranceService.events;

import com.charroux.insuranceService.entity.InsuranceOffer;
import com.charroux.insuranceService.service.InsuranceOfferCreationService;
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
        {"eventType":"AuctionWon","eventVersion":1,"eventId":"event-1","rentalId":"RENT-1",
         "carId":42,"plateNumber":"AA-123-AA","carBrand":"Tesla","carModel":"Model 3",
         "finalPrice":62,"originalPrice":65,"discount":3}
        """;

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ListOperations<String, String> lists;
    @Mock
    private InsuranceOfferCreationService creationService;
    @Mock
    private InsuranceDeadLetterPublisher deadLetterPublisher;

    private InsuranceEventConsumer consumer;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(lists);
        consumer = new InsuranceEventConsumer(redisTemplate, new ObjectMapper(), creationService, deadLetterPublisher);
    }

    @Test
    void createsAndAcknowledgesInsuranceOffer() {
        when(lists.move(
            InsuranceEventConsumer.READY_QUEUE,
            Direction.LEFT,
            InsuranceEventConsumer.PROCESSING_QUEUE,
            Direction.RIGHT
        )).thenReturn(EVENT);
        when(creationService.createOrReplay(any(InsuranceOffer.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(lists.remove(InsuranceEventConsumer.PROCESSING_QUEUE, 1, EVENT)).thenReturn(1L);

        consumer.consumeEvents();

        ArgumentCaptor<InsuranceOffer> offer = ArgumentCaptor.forClass(InsuranceOffer.class);
        verify(creationService).createOrReplay(offer.capture());
        assertEquals("RENT-1", offer.getValue().getRentalId());
        assertEquals(7, offer.getValue().getDailyPremium());
        assertEquals("Tesla", offer.getValue().getVehicle().brand());
        verify(lists).remove(InsuranceEventConsumer.PROCESSING_QUEUE, 1, EVENT);
    }

    @Test
    void acknowledgesRedeliveredEventAfterTransactionalServiceHandlesIt() {
        when(lists.move(
            InsuranceEventConsumer.READY_QUEUE,
            Direction.LEFT,
            InsuranceEventConsumer.PROCESSING_QUEUE,
            Direction.RIGHT
        )).thenReturn(EVENT);
        when(creationService.createOrReplay(any(InsuranceOffer.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(lists.remove(InsuranceEventConsumer.PROCESSING_QUEUE, 1, EVENT)).thenReturn(1L);

        consumer.consumeEvents();

        verify(creationService).createOrReplay(any(InsuranceOffer.class));
        verify(lists).remove(InsuranceEventConsumer.PROCESSING_QUEUE, 1, EVENT);
    }

    @Test
    void movesInvalidEventToDeadLetterQueue() {
        String invalid = "{\"eventId\":\"event-2\"}";
        when(lists.move(
            InsuranceEventConsumer.READY_QUEUE,
            Direction.LEFT,
            InsuranceEventConsumer.PROCESSING_QUEUE,
            Direction.RIGHT
        )).thenReturn(invalid);

        consumer.consumeEvents();

        verify(deadLetterPublisher).moveInvalid(eq(invalid), any(IllegalArgumentException.class));
        verify(creationService, never()).createOrReplay(any());
    }
}
