package com.homes.backend.domain.property.building.controller;

import com.homes.backend.domain.property.building.dto.BuildingInformationRespDto;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "매물 건물정보 API", description = "공공데이터로 매물의 건물정보를 수집하고 조회합니다.")
public interface PropertyBuildingInformationControllerDocs {
    /**
     * Documents the endpoint that returns a property's stored building information.
     */
    @Operation(summary = "매물 건물정보 조회", description = "저장된 건축물대장·K-apt 건물정보를 조회합니다. 아직 수집하지 않은 매물은 NOT_COLLECTED 상태와 누락 필드를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 매물",
                    content = @Content(schema = @Schema(implementation = com.homes.backend.global.response.ApiResponse.class)))
    })
    ApiResponse<BuildingInformationRespDto> get(
            @Parameter(description = "매물 ID", example = "1", required = true) @PathVariable Long propertyId
    );

    /**
     * Documents the owner-only endpoint that collects and refreshes building information.
     */
    @Operation(summary = "매물 건물정보 수집·갱신", description = "매물 주소를 법정동·지번으로 해석한 뒤 건축물대장 표제부·총괄표제부와 K-apt 단지·기본정보를 수집합니다. 매물 소유자만 실행할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수집 또는 갱신 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = com.homes.backend.global.response.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "매물 소유자가 아닌 사용자",
                    content = @Content(schema = @Schema(implementation = com.homes.backend.global.response.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 매물",
                    content = @Content(schema = @Schema(implementation = com.homes.backend.global.response.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "주소 해석 실패 또는 해당 주소의 건물정보 없음",
                    content = @Content(schema = @Schema(implementation = com.homes.backend.global.response.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "건축물대장 제공기관 연결 실패",
                    content = @Content(schema = @Schema(implementation = com.homes.backend.global.response.ApiResponse.class)))
    })
    ApiResponse<BuildingInformationRespDto> resolve(
            @Parameter(description = "매물 ID", example = "1", required = true) @PathVariable Long propertyId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );
}
