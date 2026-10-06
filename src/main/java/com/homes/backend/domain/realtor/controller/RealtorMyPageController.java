package com.homes.backend.domain.realtor.controller;

import com.homes.backend.domain.bid.dto.response.MyBidListRespDto;
import com.homes.backend.domain.realtor.dto.request.AgentUpdateProfileReqDto;
import com.homes.backend.domain.realtor.dto.response.AgentDashboardStatsResDto;
import com.homes.backend.domain.realtor.dto.response.AgentProfileResDto;
import com.homes.backend.domain.realtor.dto.response.NearbyPropertyResDto;
import com.homes.backend.domain.realtor.service.RealtorService;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/realtors")
@RequiredArgsConstructor
public class RealtorMyPageController implements RealtorMyPageControllerDocs {

    private final RealtorService realtorService;

    @Override
    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/me")
    public ApiResponse<AgentProfileResDto> getMyProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        AgentProfileResDto response = realtorService.getMyProfile(userPrincipal.getId());
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/me/properties/nearby")
    public ApiResponse<List<NearbyPropertyResDto>> getNearbyAvailableProperties(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<NearbyPropertyResDto> response = realtorService.getNearbyAvailableProperties(userPrincipal.getId());
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    @PatchMapping("/me")
    public ApiResponse<AgentProfileResDto> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid AgentUpdateProfileReqDto request
    ) {
        AgentProfileResDto response = realtorService.updateMyProfile(userPrincipal.getId(), request);
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/me/bids/available")
    public ApiResponse<List<NearbyPropertyResDto>> getBiddableProperties(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<NearbyPropertyResDto> response = realtorService.getBiddableProperties(userPrincipal.getId());
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/me/bids")
    public ApiResponse<List<MyBidListRespDto>> getMyBids(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<MyBidListRespDto> response = realtorService.getMyBids(userPrincipal.getId());
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    @GetMapping("/me/stats")
    public ApiResponse<AgentDashboardStatsResDto> getMyDashboardStats(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        AgentDashboardStatsResDto response = realtorService.getMyDashboardStats(userPrincipal.getId());
        return ApiResponse.onSuccess(response);
    }
}
