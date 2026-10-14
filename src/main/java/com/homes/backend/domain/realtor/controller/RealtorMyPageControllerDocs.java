package com.homes.backend.domain.realtor.controller;

import com.homes.backend.domain.bid.dto.response.MyBidListRespDto;
import com.homes.backend.domain.realtor.dto.request.AgentUpdateProfileReqDto;
import com.homes.backend.domain.realtor.dto.response.AgentDashboardStatsResDto;
import com.homes.backend.domain.realtor.dto.response.AgentProfileResDto;
import com.homes.backend.domain.realtor.dto.response.NearbyPropertyResDto;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "중개사(Realtor) API", description = "공인중개사 마이페이지를 담당하는 API")
public interface RealtorMyPageControllerDocs {

    @Operation(summary = "중개사 마이페이지 - 내 프로필 조회", description = "로그인한 중개사 본인의 프로필(사무소명, 주소, 사업자등록번호, 인증 여부 등)을 조회합니다.")
    @GetMapping("/me")
    ApiResponse<AgentProfileResDto> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "중개사 마이페이지 - 사무소 기준 거리순 신규 매물 조회", description = "중개사무소 위치를 기준으로, 거래가능 상태인 매물을 가까운 순으로 정렬해 조회합니다. " +
            "회원가입 시 사무소 주소/좌표를 등록하지 않은 경우 조회할 수 없습니다.")
    @GetMapping("/me/properties/nearby")
    ApiResponse<List<NearbyPropertyResDto>> getNearbyAvailableProperties(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "중개사 마이페이지 - 내 프로필 수정", description = "중개사무소 이름/주소/좌표를 수정합니다. " +
            "값을 보내지 않은 필드는 기존 값이 유지됩니다(부분 수정). " +
            "사업자등록번호, 인증 서류, 인증 여부는 관리자 승인과 직결되므로 이 API로는 수정할 수 없습니다.")
    @PatchMapping("/me")
    ApiResponse<AgentProfileResDto> updateMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid AgentUpdateProfileReqDto request
    );

    @Operation(summary = "입찰가능 매물 목록 조회", description = "담당 지역(사무소 기준 거리순) 매물 중, 거래가능 상태이면서 " +
            "아직 본인이 입찰을 넣지 않은 매물만 조회합니다. 사무소 좌표를 등록하지 않은 경우 조회할 수 없습니다.")
    @GetMapping("/me/bids/available")
    ApiResponse<List<NearbyPropertyResDto>> getBiddableProperties(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "중개사 마이페이지 - 내가 보낸 입찰 제안서 목록 조회", description = "본인이 보낸 입찰 제안서 전체 이력을 최신순으로 조회합니다. " +
            "제안서 1건당 1행으로 보여주며, 역제안으로 가격을 낮춰 다시 보낸 경우 currentFee에 최신 역제안 금액이 반영됩니다 " +
            "(역제안이 없으면 최초 제안 금액과 동일).")
    @GetMapping("/me/bids")
    ApiResponse<List<MyBidListRespDto>> getMyBids(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "중개사 마이페이지 - 대시보드 통계 조회", description = "이번 달 거래(수락 확정 + 거래완료) 건수와, " +
            "매물 종류별 평균 확정 수수료(전체 기간)를 조회합니다.")
    @GetMapping("/me/stats")
    ApiResponse<AgentDashboardStatsResDto> getMyDashboardStats(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );
}
