package com.homes.backend.domain.bid.controller;

import com.homes.backend.domain.bid.dto.request.BidCreateReqDto;
import com.homes.backend.domain.bid.dto.request.NegotiationReqDto;
import com.homes.backend.domain.bid.dto.response.BidListRespDto;
import com.homes.backend.domain.bid.dto.response.NegotiationListResDto;
import com.homes.backend.domain.bid.service.BidService;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/properties/{propertyId}/bids")
public class BidController implements BidControllerDocs{

    private final BidService bidService;

    @Override
    @PreAuthorize("hasRole('AGENT')") // AGENT 권한만 접근 가능(중개사만 가능)
    @PostMapping
    public ApiResponse<Void> createBid(
            @PathVariable Long propertyId,
            @Valid @RequestBody BidCreateReqDto reqDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        bidService.createBid(propertyId, reqDto, userPrincipal.getId());
        return ApiResponse.onSuccess(null);
    }

    @Override
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ApiResponse<List<BidListRespDto>> getPropertyBids(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<BidListRespDto> response = bidService.getPropertyBids(propertyId, userPrincipal.getId());
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @GetMapping("/{bidId}/negotiations")
    public ApiResponse<List<NegotiationListResDto>> getNegotiationList(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<NegotiationListResDto> response = bidService.getNegotiationList(propertyId, bidId, userPrincipal.getId(), userPrincipal.getRole());
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @PostMapping("/{bidId}/negotiations")
    public ApiResponse<Void> createNegotiation(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @Valid @RequestBody NegotiationReqDto reqDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        bidService.createNegotiation(propertyId, bidId, reqDto, userPrincipal.getId(), userPrincipal.getRole());
        return ApiResponse.onSuccess(null);
    }

    @Override
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{bidId}/accept")
    public ApiResponse<Void> acceptBid(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        bidService.acceptBid(propertyId, bidId, userPrincipal.getId());
        return ApiResponse.onSuccess(null);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @PostMapping("/{bidId}/cancel")
    public ApiResponse<Void> cancelBid(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        bidService.cancelBid(propertyId, bidId, userPrincipal.getId(), userPrincipal.getRole());
        return ApiResponse.onSuccess(null);
    }

    @Override
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{bidId}/complete")
    public ApiResponse<Void> completeBid(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        bidService.completeBid(propertyId, bidId, userPrincipal.getId());
        return ApiResponse.onSuccess(null);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    @PatchMapping("/{bidId}/withdraw")
    public ApiResponse<Void> withdrawBid(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        bidService.withdrawBid(propertyId, bidId, userPrincipal.getId());
        return ApiResponse.onSuccess(null);
    }

    @Override
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/{bidId}/reject")
    public ApiResponse<Void> rejectBid(
            @PathVariable Long propertyId,
            @PathVariable Long bidId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        bidService.rejectBid(propertyId, bidId, userPrincipal.getId());
        return ApiResponse.onSuccess(null);
    }
}
