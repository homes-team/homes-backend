package com.homes.backend.domain.property.building.controller;

import com.homes.backend.domain.property.building.dto.BuildingInformationRespDto;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "매물 건물정보 API", description = "공공데이터로 매물의 건물정보를 수집하고 조회합니다.")
public interface PropertyBuildingInformationControllerDocs {
    @Operation(summary = "매물 건물정보 조회", description = "수집된 사용승인일, 건축연도, 세대 수 및 외부 식별자를 반환합니다.")
    ApiResponse<BuildingInformationRespDto> get(
            @Parameter(description = "매물 ID") @PathVariable Long propertyId
    );

    @Operation(summary = "매물 건물정보 수집·갱신", description = "매물 주소를 해석하고 건축물대장 및 공동주택 정보를 수집합니다.")
    ApiResponse<BuildingInformationRespDto> resolve(
            @Parameter(description = "매물 ID") @PathVariable Long propertyId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );
}
