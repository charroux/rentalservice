package com.charroux.insuranceService.web;

import com.charroux.insuranceService.entity.InsuranceOffer;

public record InsuranceOfferResponse(
    String rentalId,
    String plateNumber,
    Integer dailyPremium,
    String status
) {
    public static InsuranceOfferResponse from(InsuranceOffer offer) {
        return new InsuranceOfferResponse(
            offer.getRentalId(),
            offer.getPlateNumber(),
            offer.getDailyPremium(),
            offer.getStatus().name()
        );
    }
}
