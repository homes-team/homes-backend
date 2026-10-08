package com.homes.backend.domain.bid.controller;

import com.homes.backend.domain.bid.dto.request.BidCreateReqDto;
import com.homes.backend.domain.bid.dto.request.NegotiationReqDto;
import com.homes.backend.domain.bid.dto.response.BidListRespDto;
import com.homes.backend.domain.bid.dto.response.NegotiationListResDto;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "입찰(Bid) API", description = "매물에 대한 중개사 입찰 제안서 및 수수료 역제안 관련 API")
public interface BidControllerDocs {

    @Operation(summary = "입찰 제안서 제출", description = "중개사가 특정 매물에 대해 본인의 수수료와 어필 내용을 담아 입찰서를 제출합니다.")
    ApiResponse<Void> createBid(
            @PathVariable Long propertyId,
            @RequestBody BidCreateReqDto reqDto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(
            summary = "매물 입찰 제안서 목록 조회",
            description = "집주인이 자신의 매물에 달린 중개사들의 입찰 제안서 목록을 최신순으로 조회합니다."
    )
    ApiResponse<List<BidListRespDto>> getPropertyBids(
            @PathVariable Long propertyId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "역제안 내역 조회", description = "집주인과 중개사가 주고받은 수수료 조율 기록을 반환합니다.")
    ApiResponse<List<NegotiationListResDto>> getNegotiationList(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "수수료 역제안 전송(1:1)", description = "집주인 또는 중개사가 새로운 수수료를 담아 역제안을 보냅니다.")
    ApiResponse<Void> createNegotiation(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @RequestBody NegotiationReqDto reqDto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "중개사 선택 및 매칭 확정", description = "사용자가 특정 제안서를 보고 선택했을 때, 상태를 '매칭 완료'로 업데이트합니다.")
    ApiResponse<Void> acceptBid(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "매칭 취소", description = "매칭 확정된 제안서를 취소하고 매물을 다시 입찰 가능(AVAILABLE) 상태로 되돌립니다. " +
            "채팅 중 거래가 불발될 수 있으므로 집주인 또는 매칭된 중개사 둘 다 요청할 수 있습니다.")
    ApiResponse<Void> cancelBid(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "거래완료 처리", description = "매칭 확정된 거래를 최종 완료 처리합니다. " +
            "중개사가 스스로 완료 처리하면 본인의 성사율/통계를 조작할 수 있으므로 매물을 등록한 집주인만 요청할 수 있습니다.")
    ApiResponse<Void> completeBid(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "제안서 철회", description = "중개사가 아직 대기 중(PENDING)인 입찰 제안서를 스스로 철회(WITHDRAWN)합니다.")
    ApiResponse<Void> withdrawBid(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "제안서 거절", description = "집주인이 대기 중(PENDING)인 입찰 제안서를 명시적으로 거절(REJECTED)합니다.")
    ApiResponse<Void> rejectBid(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );
}
