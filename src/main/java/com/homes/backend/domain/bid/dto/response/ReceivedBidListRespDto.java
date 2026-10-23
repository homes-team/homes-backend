package com.homes.backend.domain.bid.dto.response;

import com.homes.backend.domain.bid.entity.Bid;
import com.homes.backend.domain.bid.entity.BidStatus;

import java.time.LocalDateTime;

public record ReceivedBidListRespDto(
        Long bidId,
        Long propertyId,
        String propertyTitle,
        String propertyAddress,
        Long agentId,
        String officeName,       // 중개사무소 이름
        String profileImageUrl,  // 중개사 프로필 사진
        Double proposedFee,      // 제안 수수료
        String content,          // 어필 메시지
        BidStatus status,        // 현재 상태
        LocalDateTime createdAt
) {
    public static ReceivedBidListRespDto from(Bid bid) {
        return new ReceivedBidListRespDto(
                bid.getId(),
                bid.getProperty().getId(),
                bid.getProperty().getTitle(),
                bid.getProperty().getAddress(),
                bid.getAgent().getId(),
                bid.getAgent().getOfficeName(),
                bid.getAgent().getProfileImageUrl(),
                bid.getProposedFee(),
                bid.getContent(),
                bid.getStatus(),
                bid.getCreatedAt()
        );
    }
}
