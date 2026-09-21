package com.homes.backend.domain.verification.entity;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "owner_verification")
@EntityListeners(AuditingEntityListener.class)
/**
 * 집주인 서류 인증
 */
public class OwnerVerification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "owner_verification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 2000)
    private String documentUrl; // 등기부등본 원본 이미지 URL (관리자 확인용)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status;

    private String registeredOwnerName; // 등기부등본 상 이름 (향후 OCR 추출)
    private Boolean isNameMatched;      // 실명 일치 여부
    private String failReason;          // 실패 사유 (공동명의 등)

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;  // 비동기 처리 완료 시간

    // 비동기 처리가 끝난 후 상태를 업데이트하는 비즈니스 메서드
    public void completeVerification(String ownerName, boolean isMatched, VerificationStatus status, String failReason) {
        this.registeredOwnerName = ownerName;
        this.isNameMatched = isMatched;
        this.status = status;
        this.failReason = failReason;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 관리자 수동 검수 처리
     */
    public void processAdminReview(VerificationStatus newStatus) {
        this.status = newStatus;
        this.processedAt = java.time.LocalDateTime.now(); // 처리 시간 업데이트
    }
}
