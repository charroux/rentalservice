package com.charroux.carRental.dto;

import java.math.BigDecimal;

public record AuctionResultDTO(
    String plateNumber,
    BigDecimal finalCustomerPrice,  // Prix que l'utilisateur paiera
    BigDecimal originalPrice,       // Prix de base (avant remise)
    BigDecimal discountAmount,      // Montant de la remise
    boolean discountApplied         // Si une remise a été appliquée
) {
}