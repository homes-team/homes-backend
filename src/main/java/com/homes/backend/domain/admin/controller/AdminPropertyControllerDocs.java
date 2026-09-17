package com.homes.backend.domain.admin.controller;

import com.homes.backend.domain.admin.dto.request.AdminPropertySuspiciousReqDto;
import com.homes.backend.domain.admin.dto.response.AdminPropertyReportDetailResDto;
import com.homes.backend.domain.admin.dto.response.AdminReportedPropertyResDto;
import com.homes.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "관리자(Admin) API", description = "관리자 전용 API")
public interface AdminPropertyControllerDocs {

    @Operation(summary = "신고된 매물 목록 조회", description = "신고가 1건 이상 접수된 매물 목록을 신고 횟수가 많은 순으로 조회합니다. " +
            "신고 5건 이상 누적되어 자동으로 의심 매물(isSuspicious=true)로 전환된 매물이 자연히 상위에 몰립니다. 이미 삭제 처리된 매물은 제외됩니다.")
    @GetMapping("/reports")
    ApiResponse<List<AdminReportedPropertyResDto>> getReportedProperties();

    @Operation(summary = "매물 신고 상세 내역 조회", description = "특정 매물에 접수된 신고 내역(사유, 신고자, 신고 시각)을 조회합니다. " +
            "의심 매물 여부, 삭제 여부와 무관하게 신고 이력이 있으면 조회할 수 있습니다.")
    @GetMapping("/properties/{propertyId}/reports")
    ApiResponse<List<AdminPropertyReportDetailResDto>> getPropertyReportDetail(
            @Parameter(description = "매물 ID") @PathVariable Long propertyId
    );

    @Operation(summary = "허위 매물 강제 삭제", description = "신고 내역을 검토한 뒤 허위로 판단된 매물을 강제로 삭제합니다. " +
            "일반 유저의 매물 삭제와 동일하게 소프트 삭제(상태를 DELETED로 변경)로 처리되며, 소유자가 누구든 관계없이 삭제할 수 있습니다.")
    @DeleteMapping("/properties/{propertyId}")
    ApiResponse<Void> deleteProperty(
            @Parameter(description = "매물 ID") @PathVariable Long propertyId
    );

    @Operation(summary = "의심 매물 여부 수동 지정", description = "신고 누적(5건)이나 규칙 기반 자동탐지(동일 호실 중복 등록)를 거치지 않고, " +
            "관리자가 직접 특정 매물을 의심 매물로 지정하거나 해제합니다. 삭제까지는 아니지만 주의가 필요한 경우에 사용합니다.")
    @PatchMapping("/properties/{propertyId}/suspicious")
    ApiResponse<Void> updateSuspiciousStatus(
            @Parameter(description = "매물 ID") @PathVariable Long propertyId,
            @RequestBody @Valid AdminPropertySuspiciousReqDto reqDto
    );
}
