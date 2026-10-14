package com.homes.backend.domain.property.price.model;

import java.time.LocalDate;

public record ComparableTradeSummary(
        String apartmentName,
        String legalDongName,
        double areaSquareMeters,
        int floor,
        Integer buildYear,
        long priceTenThousandWon,
        LocalDate contractDate
) {
    public static ComparableTradeSummary from(ApartmentTrade trade) {
        return new ComparableTradeSummary(trade.apartmentName(), trade.legalDongName(),
                trade.areaSquareMeters(), trade.floor(), trade.buildYear(),
                trade.priceTenThousandWon(), trade.contractDate());
    }
}
