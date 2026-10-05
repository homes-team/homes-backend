package com.homes.backend.domain.property.price.model;

public record PricePredictionTarget(
        double areaSquareMeters,
        int floor,
        Integer buildYear,
        String apartmentName,
        String legalDongName,
        String lotNumber
) {
}
