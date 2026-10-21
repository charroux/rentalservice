package com.charroux.insuranceService.events;

import com.charroux.insuranceService.entity.InsuranceOffer;
import com.charroux.insuranceService.repository.InsuranceOutboxRepository;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InsuranceOfferEventPublisher {

    public static final String PUBLISHED_QUEUE = "events:simple:published";
    private final InsuranceOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public InsuranceOfferEventPublisher(
            InsuranceOutboxRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void publishProposed(InsuranceOffer offer) {
        publish(new InsuranceOfferEvent(
            "InsuranceOfferProposed",
            1,
            offer.getProposedEventId(),
            offer.getRentalId(),
            offer.getEventId(),
            offer.getVehicle().carId(),
            offer.getPlateNumber(),
            offer.getVehicle().brand(),
            offer.getVehicle().model(),
            offer.getRentalDailyPrice(),
            offer.getOriginalDailyPrice(),
            offer.getDiscount(),
            offer.getDailyPremium(),
            "PROPOSED",
            offer.getCreatedAt()
        ));
    }

    public void publishAccepted(InsuranceOffer offer) {
        publish(new InsuranceOfferEvent(
            "InsuranceOfferAccepted",
            1,
            offer.getAcceptedEventId(),
            offer.getRentalId(),
            offer.getEventId(),
            offer.getVehicle().carId(),
            offer.getPlateNumber(),
            offer.getVehicle().brand(),
            offer.getVehicle().model(),
            offer.getRentalDailyPrice(),
            offer.getOriginalDailyPrice(),
            offer.getDiscount(),
            offer.getDailyPremium(),
            "ACCEPTED",
            offer.getAcceptedAt()
        ));
    }

    private void publish(InsuranceOfferEvent event) {
        if (event.eventId() == null || event.occurredAt() == null) {
            throw new IllegalStateException("Insurance event identity and date must be persisted first");
        }
        try {
            if (!outboxRepository.existsByEventId(event.eventId())) {
                outboxRepository.save(new InsuranceOutboxEvent(
                    event.eventId(),
                    event.eventType(),
                    event.rentalId(),
                    objectMapper.writeValueAsString(event),
                    event.occurredAt(),
                    Instant.now()
                ));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize insurance event", e);
        }
    }

    record InsuranceOfferEvent(
        String eventType,
        int eventVersion,
        String eventId,
        String rentalId,
        String sourceAuctionEventId,
        Long carId,
        String plateNumber,
        String carBrand,
        String carModel,
        Integer rentalDailyPrice,
        Integer originalDailyPrice,
        Integer discount,
        Integer dailyPremium,
        String status,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt
    ) {
    }
}
