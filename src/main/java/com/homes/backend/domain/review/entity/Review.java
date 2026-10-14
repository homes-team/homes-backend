package com.homes.backend.domain.review.entity;

import com.homes.backend.domain.user.entity.User;
import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reviewer_target",
                        columnNames = {"reviewer_id", "target_user_id"} // 중복 리뷰 방지
                )
        }
)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(nullable = false)
    private Float score; // 평점 (0.0 ~ 5.0)

    @Column(length = 500)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser; // 리뷰 대상 (중개사의 User)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer; // 작성자

    @Builder
    public Review(Float score, String content, User targetUser, User reviewer) {
        this.score = score;
        this.content = content;
        this.targetUser = targetUser;
        this.reviewer = reviewer;
    }
}
