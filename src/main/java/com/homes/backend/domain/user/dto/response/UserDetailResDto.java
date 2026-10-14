package com.homes.backend.domain.user.dto.response;

import com.homes.backend.domain.user.entity.User;

import java.time.LocalDateTime;

/**
 * 공인중개사에게 제공할 사용자의 공개 상세 정보를 담습니다.
 *
 * @param userId 사용자 ID
 * @param nickname 닉네임
 * @param isIdentityVerified 본인 인증 여부
 * @param averageReviewScore 평균 리뷰 점수
 * @param reviewCount 리뷰 수
 * @param createdAt 가입 일시
 */
public record UserDetailResDto(
        Long userId,
        String nickname,
        boolean isIdentityVerified,
        Double averageReviewScore,
        long reviewCount,
        LocalDateTime createdAt
) {
    /**
     * 사용자와 리뷰 통계를 공개 상세 정보 응답으로 변환합니다.
     *
     * @param user 변환할 사용자
     * @param averageReviewScore 평균 리뷰 점수
     * @param reviewCount 리뷰 수
     * @return 사용자 공개 상세 정보
     */
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
