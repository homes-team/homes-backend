package com.homes.backend.domain.property.controller;

import com.homes.backend.domain.property.dto.request.ReportCreateReqDto;
import com.homes.backend.domain.property.dto.response.PropertyReportSummaryResDto;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "매물 신고(Report) API", description = "허위 매물 및 부적절한 게시글 신고 접수 API")
public interface PropertyReportControllerDocs {
    @Operation(summary = "매물 신고 접수", description = "문제가 있는 매물을 신고합니다. 한 유저는 같은 매물을 중복 신고할 수 없으며, 누적 신고 시 의심 매물로 전환됩니다.")
    ApiResponse<Void> reportProperty(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "신고할 매물의 ID", required = true) @PathVariable("propertyId") Long propertyId,
            @RequestBody @Valid ReportCreateReqDto reqDto
    );

    /**
     * 특정 매물의 누적 신고 횟수와 의심 매물 여부를 조회합니다.
     *
     * @param propertyId 매물 ID
     * @return 매물 신고 요약
     */
    @Operation(summary = "누적 신고 횟수 조회", description = "특정 매물에 몇 번의 신고가 누적되었는지, 의심 매물로 전환됐는지 확인합니다.")
    ApiResponse<PropertyReportSummaryResDto> getReportSummary(
            @Parameter(description = "조회할 매물의 ID", required = true) @PathVariable("propertyId") Long propertyId
    );
}
