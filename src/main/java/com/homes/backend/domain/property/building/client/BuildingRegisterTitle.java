package com.homes.backend.domain.property.building.client;

import java.time.LocalDate;

public record BuildingRegisterTitle(
        String registerId,
        String buildingName,
        String lotAddress,
        String roadAddress,
        LocalDate approvalDate,
        Integer householdCount,
        Integer familyCount,
        Double heightMeters,
        Integer groundFloorCount,
        Integer undergroundFloorCount,
        Integer elevatorCount,
        Integer parkingCount
) {
}
