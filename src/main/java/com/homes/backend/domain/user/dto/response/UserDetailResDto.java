package com.homes.backend.domain.user.dto.response;

import com.homes.backend.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserDetailResDto(
        Long userId,
        String nickname,
        boolean isIdentityVerified,
        Double averageReviewScore,
        long reviewCount,
        LocalDateTime createdAt
) {
    public static UserDetailResDto of(User user, Double averageReviewScore, long reviewCount) {
        return new UserDetailResDto(
                user.getId(),
                user.getNickname(),
                user.isIdentityVerified(),
                averageReviewScore,
                reviewCount,
                user.getCreatedAt()
        );
    }
}
