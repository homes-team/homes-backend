package com.homes.backend.domain.admin.controller;

import com.homes.backend.domain.admin.dto.request.AdminOwnerVerificationUpdateReqDto;
import com.homes.backend.domain.admin.dto.response.AdminOwnerVerificationListResDto;
import com.homes.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "관리자(Admin) API", description = "관리자 전용 API")
public interface AdminVerificationControllerDocs {
    @Operation(summary = "수동 검수 대기 목록 조회", description = "자동 인증에 실패하여 관리자의 수동 검수가 필요한 집주인 인증 내역을 조회합니다.")
    ApiResponse<List<AdminOwnerVerificationListResDto>> getManualReviewList();

    @Operation(summary = "수동 검수 결과 처리", description = "관리자가 서류를 확인하고 승인(true) 또는 반려(false) 처리합니다.")
    ApiResponse<Void> processManualReview(
            @Parameter(description = "인증 내역 ID") @PathVariable Long verificationId,
            @RequestBody AdminOwnerVerificationUpdateReqDto reqDto
    );
}
