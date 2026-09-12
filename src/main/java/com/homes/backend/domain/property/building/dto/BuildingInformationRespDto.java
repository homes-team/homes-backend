package com.homes.backend.domain.property.building.dto;

import com.homes.backend.domain.property.building.entity.PropertyBuildingInformation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(name = "BuildingInformationResponse", description = "AI 매물 평가에 사용하는 건축물대장 및 K-apt 보강 정보")
public record BuildingInformationRespDto(
        @Schema(description = "매물 ID", example = "1") Long propertyId,
        @Schema(description = "수집 상태", allowableValues = {"NOT_COLLECTED", "PARTIAL", "RESOLVED"}, example = "RESOLVED") String status,
        @Schema(description = "주소 매칭용 정규화 주소", example = "서울특별시 도봉구 방학동 123-4") String normalizedAddress,
        @Schema(description = "건축HUB 관리건축물대장 PK", example = "1132010600101230004000001") String buildingRegisterId,
        @Schema(description = "K-apt 단지 코드", example = "A10020507") String kaptCode,
        @Schema(description = "사용승인일", example = "2004-06-18") LocalDate approvalDate,
        @Schema(description = "사용승인일에서 계산한 건축연도", example = "2004") Integer buildingYear,
        @Schema(description = "총괄표제부 또는 K-apt 기준 세대 수", example = "720") Integer householdCount,
        @Schema(description = "총괄표제부 또는 K-apt 기준 건물 동 수", example = "8") Integer buildingCount,
        @Schema(description = "표제부 기준 건물 높이(m)", example = "52.4") Double buildingHeightMeters,
        @Schema(description = "지상층 수", example = "18") Integer groundFloorCount,
        @Schema(description = "지하층 수", example = "2") Integer undergroundFloorCount,
        @Schema(description = "승용·비상용 승강기 합계", example = "10") Integer elevatorCount,
        @Schema(description = "옥내·옥외, 자주식·기계식 주차대수 합계", example = "540") Integer parkingCount,
        @Schema(description = "K-apt 복도 유형", example = "계단식") String corridorType,
        @Schema(description = "K-apt 난방 유형", example = "개별난방") String heatingType,
        @Schema(description = "수집에 사용된 원천", allowableValues = {"BUILDING_REGISTER", "BUILDING_REGISTER,K_APT"}, example = "BUILDING_REGISTER,K_APT") String dataSources,
        @Schema(description = "마지막 수집 시각", example = "2026-09-11T16:30:00") LocalDateTime collectedAt,
        @Schema(description = "추가 수집이 필요한 필드명", example = "[\"kaptCode\"]") List<String> missingFields
) {
    /**
     * Creates the response returned before any building information has been collected.
     */
    public static BuildingInformationRespDto notCollected(Long propertyId) {
        return new BuildingInformationRespDto(propertyId, "NOT_COLLECTED", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                List.of("buildingYear", "buildingRegisterId", "householdCount"));
    }

    /**
     * Maps persisted building information to its API response representation.
     */
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
