package com.homes.backend.domain.admin.controller;

import com.homes.backend.domain.admin.dto.response.AdminRealtorDetailResDto;
import com.homes.backend.domain.admin.dto.response.AdminRealtorSummaryResDto;
import com.homes.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Tag(name = "관리자(Admin) API", description = "관리자 전용 API")
public interface AdminControllerDocs {

    @Operation(summary = "승인 대기 중개사 목록 조회", description = "아직 승인되지 않은(isVerified=false) 중개사 목록을 오래 기다린 순으로 조회합니다.")
    @GetMapping
    ApiResponse<List<AdminRealtorSummaryResDto>> getRealtors();

    @Operation(summary = "중개사 상세 조회", description = "특정 중개사의 상세 정보(제출한 사업자등록증/중개사무소 등록증 이미지 포함)를 조회합니다. 승인 여부 판단에 사용합니다.")
    @GetMapping("/{realtorId}")
    ApiResponse<AdminRealtorDetailResDto> getRealtorDetail(
            @Parameter(description = "Agent(중개사 프로필) ID") @PathVariable Long realtorId
    );

    @Operation(summary = "중개사 승인", description = "제출된 서류를 검토한 뒤 해당 중개사의 가입을 승인합니다. 승인 시 isVerified가 true로 바뀌며, 이후 입찰 등 중개사 전용 기능을 사용할 수 있습니다.")
    @PostMapping("/{realtorId}/approve")
    ApiResponse<Void> approveRealtor(
            @Parameter(description = "Agent(중개사 프로필) ID") @PathVariable Long realtorId
    );
}
