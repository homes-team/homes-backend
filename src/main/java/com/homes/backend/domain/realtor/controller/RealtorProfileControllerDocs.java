package com.homes.backend.domain.realtor.controller;

import com.homes.backend.domain.realtor.dto.response.RealtorPublicProfileResDto;
import com.homes.backend.domain.review.dto.request.ReviewCreateReqDto;
import com.homes.backend.domain.review.dto.response.ReviewListRespDto;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "중개사(Realtor) API", description = "유저가 보는 중개사 상세 프로필/리뷰를 담당하는 API")
public interface RealtorProfileControllerDocs {

    @Operation(summary = "중개사 상세 프로필 조회", description = "특정 중개사의 상세 프로필(사무소 정보, 인증 여부, 성사율, 평균 리뷰 평점/건수)을 조회합니다.")
    @GetMapping("/{realtorId}")
    ApiResponse<RealtorPublicProfileResDto> getRealtorProfile(
            @PathVariable Long realtorId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "중개사 리뷰 목록 조회", description = "특정 중개사가 받은 리뷰 목록을 최신순으로 조회합니다.")
    @GetMapping("/{realtorId}/reviews")
    ApiResponse<List<ReviewListRespDto>> getRealtorReviews(
            @PathVariable Long realtorId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "중개사 리뷰 작성", description = "특정 중개사에 대한 평점/리뷰를 작성합니다. 본인에게는 작성할 수 없고, 한 중개사당 1회만 작성할 수 있습니다. " +
            "중개사끼리는 서로 거래 관계가 없으므로, role이 AGENT인 계정은 리뷰를 작성할 수 없습니다(회원만 작성 가능).")
    @PostMapping("/{realtorId}/reviews")
    ApiResponse<Void> createRealtorReview(
            @PathVariable Long realtorId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid ReviewCreateReqDto request
    );
}
