package com.homes.backend.domain.bid.dto.response;

import com.homes.backend.domain.bid.entity.Bid;
import com.homes.backend.domain.bid.entity.BidStatus;

import java.time.LocalDateTime;

public record MyBidListRespDto(
        Long bidId,
        Long propertyId,
        String propertyTitle,
        String propertyAddress,
        Double proposedFee,    // 최초 제안 수수료
        Double currentFee,     // 역제안으로 조정된 최신 수수료 (역제안이 없으면 최초 제안가와 동일)
        BidStatus status,
        LocalDateTime createdAt
) {
    public static MyBidListRespDto of(Bid bid, Double currentFee) {
        return new MyBidListRespDto(
                bid.getId(),
                bid.getProperty().getId(),
                bid.getProperty().getTitle(),
                bid.getProperty().getAddress(),
                bid.getProposedFee(),
                currentFee,
                bid.getStatus(),
                bid.getCreatedAt()
        );
    }
}
