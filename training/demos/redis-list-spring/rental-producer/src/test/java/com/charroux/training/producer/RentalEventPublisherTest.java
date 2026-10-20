package com.charroux.training.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RentalEventPublisherTest {

    @Test
    void appendsJsonToTheTutorialQueue() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ListOperations<String, String> lists = mock(ListOperations.class);
        when(redis.opsForList()).thenReturn(lists);
        when(lists.rightPush(eq(RentalEventPublisher.QUEUE), anyString())).thenReturn(1L);

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        RentalEventPublisher publisher = new RentalEventPublisher(redis, mapper);
        RentalConfirmed event = new RentalConfirmed(
            "event-1", "RentalConfirmed", 1, Instant.parse("2026-10-20T10:15:30Z"),
            "RENT-42", "CAR-7", 62
        );

        long size = publisher.publish(event);

        assertThat(size).isEqualTo(1);
        verify(lists).rightPush(
            RentalEventPublisher.QUEUE,
            mapper.writeValueAsString(event)
        );
    }
}
