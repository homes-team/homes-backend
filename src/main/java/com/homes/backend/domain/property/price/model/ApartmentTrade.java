package com.homes.backend.domain.property.price.model;

import java.time.LocalDate;

/** A normalized apartment sale transaction returned by the public provider. */
public record ApartmentTrade(
        String apartmentName,
        String legalDongName,
        String lotNumber,
        double areaSquareMeters,
        int floor,
        Integer buildYear,
        long priceTenThousandWon,
        LocalDate contractDate
) {
}
