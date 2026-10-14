package com.homes.backend.domain.realtor.controller;

import com.homes.backend.domain.realtor.dto.response.RealtorPublicProfileResDto;
import com.homes.backend.domain.realtor.service.RealtorService;
import com.homes.backend.domain.review.dto.request.ReviewCreateReqDto;
import com.homes.backend.domain.review.dto.response.ReviewListRespDto;
import com.homes.backend.domain.review.service.ReviewService;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/realtors")
@RequiredArgsConstructor
public class RealtorProfileController implements RealtorProfileControllerDocs {

    private final RealtorService realtorService;
    private final ReviewService reviewService;

    @Override
    @GetMapping("/{realtorId}")
    public ApiResponse<RealtorPublicProfileResDto> getRealtorProfile(
            @PathVariable Long realtorId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        RealtorPublicProfileResDto response = realtorService.getPublicProfile(realtorId);
        return ApiResponse.onSuccess(response);
    }

    @Override
    @GetMapping("/{realtorId}/reviews")
    public ApiResponse<List<ReviewListRespDto>> getRealtorReviews(
            @PathVariable Long realtorId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        Long targetUserId = realtorService.resolveUserIdByAgentId(realtorId);
        List<ReviewListRespDto> response = reviewService.getReviews(targetUserId);
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PostMapping("/{realtorId}/reviews")
    public ApiResponse<Void> createRealtorReview(
            @PathVariable Long realtorId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid ReviewCreateReqDto request
    ) {
        if (userPrincipal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }
        // 중개사끼리는 서로 거래 관계가 없으므로, 회원(집주인)만 중개사에게 리뷰를 남길 수 있다
        if ("AGENT".equals(userPrincipal.getRole())) {
            throw new CustomException(GlobalErrorCode.FORBIDDEN);
        }

        Long targetUserId = realtorService.resolveUserIdByAgentId(realtorId);
        reviewService.createReview(targetUserId, userPrincipal.getId(), request);
        return ApiResponse.onSuccess();
    }
}
