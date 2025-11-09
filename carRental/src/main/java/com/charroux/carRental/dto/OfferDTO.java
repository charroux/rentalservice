package com.charroux.carRental.dto;

import java.math.BigDecimal;

public record OfferDTO(
    Long carModelId,
    String brand,
    String model,
    String photo,
    BigDecimal rentalPrice
) {
}