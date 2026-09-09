package com.homes.backend.domain.property.building.dto;

import com.homes.backend.domain.property.building.entity.PropertyBuildingInformation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record BuildingInformationRespDto(
        Long propertyId,
        String status,
        String normalizedAddress,
        String buildingRegisterId,
        String kaptCode,
        LocalDate approvalDate,
        Integer buildingYear,
        Integer householdCount,
        Integer buildingCount,
        Double buildingHeightMeters,
        Integer groundFloorCount,
        Integer undergroundFloorCount,
        Integer elevatorCount,
        Integer parkingCount,
        String corridorType,
        String heatingType,
        String dataSources,
        LocalDateTime collectedAt,
        List<String> missingFields
) {
    public static BuildingInformationRespDto notCollected(Long propertyId) {
        return new BuildingInformationRespDto(propertyId, "NOT_COLLECTED", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                List.of("buildingYear", "buildingRegisterId", "householdCount"));
    }

    public static BuildingInformationRespDto from(PropertyBuildingInformation information) {
        List<String> missing = new ArrayList<>();
        if (information.getBuildingYear() == null) missing.add("buildingYear");
        if (information.getBuildingRegisterId() == null) missing.add("buildingRegisterId");
        if (information.getHouseholdCount() == null) missing.add("householdCount");
        if (information.getKaptCode() == null) missing.add("kaptCode");
        return new BuildingInformationRespDto(
                information.getPropertyId(), information.getStatus().name(), information.getNormalizedAddress(),
                information.getBuildingRegisterId(), information.getKaptCode(), information.getApprovalDate(),
                information.getBuildingYear(), information.getHouseholdCount(), information.getBuildingCount(),
                information.getBuildingHeightMeters(), information.getGroundFloorCount(), information.getUndergroundFloorCount(),
                information.getElevatorCount(), information.getParkingCount(), information.getCorridorType(),
                information.getHeatingType(), information.getDataSources(), information.getCollectedAt(), List.copyOf(missing)
        );
    }
}
