package com.charroux.rentalQueryService.events;

import com.charroux.rentalQueryService.projection.ProjectionIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisListCommands.Direction;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.*;

class RentalProjectionConsumerTest {
    private RedisTemplate<String, String> redis;
    private ListOperations<String, String> lists;
    private ProjectionIngestionService ingestionService;
    private ProjectionDeadLetterPublisher deadLetterPublisher;
    private RentalProjectionConsumer consumer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(RedisTemplate.class);
        lists = mock(ListOperations.class);
        ingestionService = mock(ProjectionIngestionService.class);
        deadLetterPublisher = mock(ProjectionDeadLetterPublisher.class);
        when(redis.opsForList()).thenReturn(lists);
        consumer = new RentalProjectionConsumer(redis, ingestionService, deadLetterPublisher);
    }

    @Test
    void acknowledgesOnlyAfterProjectionWasApplied() {
        when(lists.move(RentalProjectionConsumer.READY_QUEUE, Direction.LEFT,
            RentalProjectionConsumer.PROCESSING_QUEUE, Direction.RIGHT)).thenReturn("event");
        when(lists.remove(RentalProjectionConsumer.PROCESSING_QUEUE, 1, "event")).thenReturn(1L);

        consumer.consumeEvents();

        var order = inOrder(ingestionService, lists);
        order.verify(ingestionService).ingest("event");
        order.verify(lists).remove(RentalProjectionConsumer.PROCESSING_QUEUE, 1, "event");
    }

    @Test
    void invalidProjectionMovesToDeadLetterQueue() {
        when(lists.move(RentalProjectionConsumer.READY_QUEUE, Direction.LEFT,
            RentalProjectionConsumer.PROCESSING_QUEUE, Direction.RIGHT)).thenReturn("bad");
        when(ingestionService.ingest("bad")).thenThrow(new IllegalArgumentException("bad"));

        consumer.consumeEvents();

        verify(deadLetterPublisher).moveInvalid(eq("bad"), any(IllegalArgumentException.class));
        verify(lists, never()).remove(anyString(), anyLong(), anyString());
    }

    @Test
    void infrastructureFailureRemainsRecoverable() {
        when(lists.move(RentalProjectionConsumer.READY_QUEUE, Direction.LEFT,
            RentalProjectionConsumer.PROCESSING_QUEUE, Direction.RIGHT)).thenReturn("event");
        when(ingestionService.ingest("event")).thenThrow(new IllegalStateException("mongo unavailable"));

        consumer.consumeEvents();

        verify(deadLetterPublisher, never()).moveInvalid(anyString(), any());
        verify(lists, never()).remove(anyString(), anyLong(), anyString());
    }
}
