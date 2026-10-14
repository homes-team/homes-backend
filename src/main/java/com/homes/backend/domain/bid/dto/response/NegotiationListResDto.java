package com.homes.backend.domain.bid.dto.response;

import com.homes.backend.domain.bid.entity.Negotiation;

import java.time.LocalDateTime;

public record NegotiationListResDto(
        Long negotiationId,
        String senderRole, // USER 또는 AGENT (누가 보냈는지 구분용)
        Double suggestedFee,
        String message,
        LocalDateTime createdAt
) {
    public static NegotiationListResDto from(Negotiation negotiation) {
        return new NegotiationListResDto(
                negotiation.getId(),
                negotiation.getSenderRole(),
                negotiation.getSuggestedFee(),
                negotiation.getMessage(),
                negotiation.getCreatedAt()
        );
    }
}
