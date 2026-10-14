package com.homes.backend.domain.property.building.client;

import java.time.LocalDate;

public record ApartmentBasicInformation(
        String kaptCode,
        String name,
        String roadAddress,
        LocalDate approvalDate,
        Integer householdCount,
        Integer buildingCount,
        Integer highestFloor,
        Integer parkingCount,
        String corridorType,
        String heatingType
) {
}
