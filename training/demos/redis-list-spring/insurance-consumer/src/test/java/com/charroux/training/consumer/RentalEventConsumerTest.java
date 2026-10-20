package com.charroux.training.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RentalEventConsumerTest {

    @Test
    void removesOneEventAndCreatesAnOffer() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ListOperations<String, String> lists = mock(ListOperations.class);
        when(redis.opsForList()).thenReturn(lists);

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        RentalConfirmed event = new RentalConfirmed(
            "event-1", "RentalConfirmed", 1, Instant.parse("2026-10-20T10:15:30Z"),
            "RENT-42", "CAR-7", 62
        );
        when(lists.leftPop(RentalEventConsumer.QUEUE))
            .thenReturn(mapper.writeValueAsString(event));

        InsuranceOfferStore store = new InsuranceOfferStore();
        RentalEventConsumer consumer = new RentalEventConsumer(redis, mapper, store);

        consumer.consumeOne();

        assertThat(store.findByRentalId("RENT-42"))
            .contains(new InsuranceOffer("RENT-42", "CAR-7", 7));
    }
}
