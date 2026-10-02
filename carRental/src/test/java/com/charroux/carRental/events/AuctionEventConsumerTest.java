package com.charroux.carRental.events;

import com.charroux.carRental.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisListCommands.Direction;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionEventConsumerTest {

    private static final String EVENT_JSON =
        "{\"eventId\":\"event-1\",\"eventType\":\"AuctionWon\"}";

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private AuctionEventConsumer consumer;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        consumer = new AuctionEventConsumer(
            redisTemplate,
            new ObjectMapper(),
            processedEventRepository
        );
    }

    @Test
    void processesAndAcknowledgesEvent() {
        when(listOperations.move(
            AuctionEventConsumer.READY_QUEUE,
            Direction.LEFT,
            AuctionEventConsumer.PROCESSING_QUEUE,
            Direction.RIGHT
        )).thenReturn(EVENT_JSON);
        when(processedEventRepository.existsByEventIdAndConsumerName(
            "event-1", "rental-service"
        )).thenReturn(false);
        when(listOperations.remove(
            AuctionEventConsumer.PROCESSING_QUEUE, 1, EVENT_JSON
        )).thenReturn(1L);

        consumer.consumeEvents();

        verify(processedEventRepository).recordProcessed(
            "event-1", "rental-service", "AuctionWon"
        );
        verify(listOperations).remove(
            AuctionEventConsumer.PROCESSING_QUEUE, 1, EVENT_JSON
        );
    }

    @Test
    void acknowledgesAlreadyProcessedEventWithoutProcessingItAgain() {
        when(listOperations.move(
            AuctionEventConsumer.READY_QUEUE,
            Direction.LEFT,
            AuctionEventConsumer.PROCESSING_QUEUE,
            Direction.RIGHT
        )).thenReturn(EVENT_JSON);
        when(processedEventRepository.existsByEventIdAndConsumerName(
            "event-1", "rental-service"
        )).thenReturn(true);
        when(listOperations.remove(
            AuctionEventConsumer.PROCESSING_QUEUE, 1, EVENT_JSON
        )).thenReturn(1L);

        consumer.consumeEvents();

        verify(processedEventRepository, never()).recordProcessed(
            anyString(), anyString(), anyString()
        );
        verify(listOperations).remove(
            AuctionEventConsumer.PROCESSING_QUEUE, 1, EVENT_JSON
        );
    }

    @Test
    void leavesInvalidEventInProcessingQueueForRecovery() {
        String invalidEvent = "{\"eventType\":\"AuctionWon\"}";
        when(listOperations.move(
            AuctionEventConsumer.READY_QUEUE,
            Direction.LEFT,
            AuctionEventConsumer.PROCESSING_QUEUE,
            Direction.RIGHT
        )).thenReturn(invalidEvent);

        consumer.consumeEvents();

        verify(listOperations, never()).remove(
            eq(AuctionEventConsumer.PROCESSING_QUEUE), anyLong(), anyString()
        );
        verifyNoInteractions(processedEventRepository);
    }

    @Test
    void recoversUnacknowledgedEventsInFifoOrder() {
        when(listOperations.move(
            AuctionEventConsumer.PROCESSING_QUEUE,
            Direction.RIGHT,
            AuctionEventConsumer.READY_QUEUE,
            Direction.LEFT
        )).thenReturn("event-2").thenReturn("event-1").thenReturn(null);

        consumer.recoverProcessingQueue();

        verify(listOperations, times(3)).move(
            AuctionEventConsumer.PROCESSING_QUEUE,
            Direction.RIGHT,
            AuctionEventConsumer.READY_QUEUE,
            Direction.LEFT
        );
    }
}
