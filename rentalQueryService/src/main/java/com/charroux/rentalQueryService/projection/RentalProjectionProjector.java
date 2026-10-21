package com.charroux.rentalQueryService.projection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.DateTimeException;
import java.time.LocalDateTime;

@Service
public class RentalProjectionProjector {
    private final RentalViewRepository repository;
    private final ObjectMapper objectMapper;

    public RentalProjectionProjector(RentalViewRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public synchronized boolean apply(String rawEvent) {
        try {
            JsonNode event = objectMapper.readTree(rawEvent);
            String eventId = requiredText(event, "eventId");
            String eventType = requiredText(event, "eventType");
            int eventVersion = requiredInt(event, "eventVersion");
            if (eventVersion != 1) {
                throw new IllegalArgumentException("Unsupported " + eventType + " version " + eventVersion);
            }
            String rentalId = requiredText(event, "rentalId");
            RentalView view = repository.findByRentalId(rentalId).orElseGet(() -> new RentalView(rentalId));
            applyKnownEvent(view, eventType, eventId, event);
            repository.save(view);
            return true;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid event JSON", e);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid event date", e);
        }
    }

    private void applyKnownEvent(RentalView view, String eventType, String eventId, JsonNode event) {
        switch (eventType) {
            case "AuctionWon" -> view.applyAuction(
                eventId, Instant.ofEpochMilli(requiredLong(event, "timestamp")),
                requiredLong(event, "carId"), requiredText(event, "plateNumber"),
                requiredText(event, "customerId"), requiredText(event, "carBrand"),
                requiredText(event, "carModel"), requiredInt(event, "finalPrice"),
                requiredInt(event, "originalPrice"), requiredInt(event, "discount"),
                LocalDateTime.parse(requiredText(event, "rentalStartDate")),
                LocalDateTime.parse(requiredText(event, "rentalEndDate"))
            );
            case "InsuranceOfferProposed", "InsuranceOfferAccepted" -> {
                String status = requiredText(event, "status");
                String expectedStatus = "InsuranceOfferAccepted".equals(eventType) ? "ACCEPTED" : "PROPOSED";
                if (!expectedStatus.equals(status)) {
                    throw new IllegalArgumentException(
                        eventType + " requires status " + expectedStatus
                    );
                }
                view.applyInsurance(
                    eventId, Instant.parse(requiredText(event, "occurredAt")),
                    requiredText(event, "sourceAuctionEventId"), requiredLong(event, "carId"),
                    requiredText(event, "plateNumber"), requiredText(event, "carBrand"),
                    requiredText(event, "carModel"), requiredInt(event, "rentalDailyPrice"),
                    requiredInt(event, "originalDailyPrice"), requiredInt(event, "discount"),
                    requiredInt(event, "dailyPremium"), status
                );
            }
            default -> throw new IllegalArgumentException("Unsupported event type: " + eventType);
        }
    }

    private static String requiredText(JsonNode event, String field) {
        String value = event.path(field).asText(null);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing required field: " + field);
        return value;
    }

    private static int requiredInt(JsonNode event, String field) {
        JsonNode value = event.path(field);
        if (!value.isIntegralNumber()) throw new IllegalArgumentException("Missing integer field: " + field);
        return value.asInt();
    }

    private static long requiredLong(JsonNode event, String field) {
        JsonNode value = event.path(field);
        if (!value.isIntegralNumber()) throw new IllegalArgumentException("Missing integer field: " + field);
        return value.asLong();
    }
}
