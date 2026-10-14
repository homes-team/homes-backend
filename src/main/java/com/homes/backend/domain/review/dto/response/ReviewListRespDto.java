package com.homes.backend.domain.review.dto.response;

import com.homes.backend.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewListRespDto(
        Long reviewId,
        Float score,
        String content,
        String reviewerNickname,
        LocalDateTime createdAt
) {
    public static ReviewListRespDto from(Review review) {
        return new ReviewListRespDto(
                review.getId(),
                review.getScore(),
                review.getContent(),
                review.getReviewer().getNickname(),
                review.getCreatedAt()
        );
    }
}
