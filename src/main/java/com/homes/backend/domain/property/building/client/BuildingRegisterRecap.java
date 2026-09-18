package com.homes.backend.domain.property.building.client;

import java.time.LocalDate;

/**
 * 건축물대장 총괄표제부에서 사용하는 단지 단위 건물정보입니다.
 */
public record BuildingRegisterRecap(
        String registerId,
        LocalDate approvalDate,
        Integer householdCount,
        Integer familyCount,
        Integer buildingCount,
        Integer parkingCount
) {
}
