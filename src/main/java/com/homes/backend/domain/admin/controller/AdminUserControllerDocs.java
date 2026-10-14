package com.homes.backend.domain.admin.controller;

import com.homes.backend.domain.admin.dto.request.AdminUserWithdrawReqDto;
import com.homes.backend.domain.admin.dto.response.AdminReportedUserResDto;
import com.homes.backend.domain.admin.dto.response.AdminUserReportDetailResDto;
import com.homes.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "관리자(Admin) API", description = "관리자 전용 API")
public interface AdminUserControllerDocs {

    @Operation(summary = "신고된 유저 목록 조회", description = "신고가 1건 이상 접수된 유저 목록을 신고 횟수가 많은 순으로 조회합니다. " +
            "신고 5건 이상 누적되어 자동으로 의심 유저(isSuspicious=true)로 전환된 유저가 자연히 상위에 몰립니다. 이미 탈퇴 처리된 유저는 제외됩니다.")
    @GetMapping("/reports")
    ApiResponse<List<AdminReportedUserResDto>> getReportedUsers();

    @Operation(summary = "유저 신고 상세 내역 조회", description = "특정 유저에게 접수된 신고 내역(사유, 신고자, 채팅방 맥락)을 조회합니다. " +
            "의심 유저 여부, 탈퇴 여부와 무관하게 신고 이력이 있으면 조회할 수 있습니다.")
    @GetMapping("/{userId}/reports")
    ApiResponse<List<AdminUserReportDetailResDto>> getUserReportDetail(
            @Parameter(description = "유저 ID") @PathVariable Long userId
    );

    @Operation(summary = "악성 유저 강제 탈퇴", description = "특정 유저를 강제로 탈퇴 처리합니다. 본인 탈퇴와 동일하게 개인정보를 익명화하는 " +
            "소프트 삭제로 처리되며(신고·거래 기록은 보존), 사유를 함께 기록합니다.")
    @DeleteMapping("/{userId}")
    ApiResponse<Void> withdrawUser(
            @Parameter(description = "강제 탈퇴시킬 유저 ID") @PathVariable Long userId,
            @RequestBody @Valid AdminUserWithdrawReqDto request
    );
}
