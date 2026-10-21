package com.charroux.insuranceService.events;

import com.charroux.insuranceService.repository.InsuranceOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InsuranceOutboxRelayTest {
    @Test
    @SuppressWarnings("unchecked")
    void marksAnEventPublishedOnlyAfterRedisConfirmsTheAppend() {
        InsuranceOutboxRepository repository = mock(InsuranceOutboxRepository.class);
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        ListOperations<String, String> lists = mock(ListOperations.class);
        InsuranceOutboxEvent event = event();
        when(repository.findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(any()))
            .thenReturn(List.of(event));
        when(redis.opsForList()).thenReturn(lists);
        when(lists.rightPush(InsuranceOfferEventPublisher.PUBLISHED_QUEUE, event.getPayload())).thenReturn(1L);

        new InsuranceOutboxRelay(repository, redis).publishPending();

        assertNotNull(event.getPublishedAt());
        verify(repository).save(event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void schedulesExponentialRetryWhenRedisIsUnavailable() {
        InsuranceOutboxRepository repository = mock(InsuranceOutboxRepository.class);
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        ListOperations<String, String> lists = mock(ListOperations.class);
        InsuranceOutboxEvent event = event();
        when(repository.findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(any()))
            .thenReturn(List.of(event));
        when(redis.opsForList()).thenReturn(lists);
        when(lists.rightPush(anyString(), anyString())).thenThrow(new IllegalStateException("offline"));

        new InsuranceOutboxRelay(repository, redis).publishPending();

        assertEquals(1, event.getAttempts());
        verify(repository).save(event);
    }

    private static InsuranceOutboxEvent event() {
        Instant now = Instant.parse("2026-10-20T09:00:00Z");
        return new InsuranceOutboxEvent(
            "proposed-1", "InsuranceOfferProposed", "RENT-1", "{\"eventId\":\"proposed-1\"}", now, now
        );
    }
}
