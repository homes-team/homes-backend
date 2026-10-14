package com.homes.backend.domain.user.entity;

import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity { //BaseEntity에서 생성일자 처리

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id") //유저 ID
    private Long id;

    @Builder.Default
    @Column(length = 50)
    private String provider = "SELF"; //가입 경로: 자체회원가입이 디폴트

    @Column(name = "provider_id", length = 255) //소셜 제공자 ID
    private String providerId;

    @Column(nullable = false, unique = true, length = 100) //이메일
    private String email;

    @Column(length = 128) //비밀번호 (소셜 가입자: NULL 허용)
    private String password;

    @Column(length = 20) // 휴대폰 번호
    private String phone;

    @Column(length = 30) // 실명
    private String name;

    @Column(unique = true, length = 50) // 닉네임
    private String nickname;

    @Builder.Default
    @Column(name = "is_identity_verified", nullable = false) // 실명 인증 여부 (디폴트 false)
    private boolean isIdentityVerified = false;

    @Builder.Default
    @Column(length = 20)
    private String role = "USER";

    @Column(name = "usage_purpose", length = 50) // 이용 목적 (예: 투자/전세 등, NULL 가능)
    private String usagePurpose;

    @Column(name = "refresh_token", length = 255) // JWT 리프레시 토큰 (NULL 가능)
    private String refreshToken;

    @Column(name = "deleted_at") // NULL이면 활성 계정, 값이 있으면 탈퇴한 계정
    private LocalDateTime deletedAt;

    @Column(name = "withdraw_reason", length = 255) // 관리자가 강제 탈퇴시킨 경우의 사유 (자진 탈퇴는 NULL)
    private String withdrawReason;

    @Builder.Default
    @ColumnDefault("0") // 이미 데이터가 있는 테이블에 컬럼 추가 시 NOT NULL 위반 방지
    @Column(name = "report_count", nullable = false)
    private Integer reportCount = 0;

    @Builder.Default
    @ColumnDefault("false") // 이미 데이터가 있는 테이블에 컬럼 추가 시 NOT NULL 위반 방지
    @Column(name = "is_suspicious", nullable = false)
    private boolean isSuspicious = false; // 신고 5회 누적 시 자동 전환

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void verifyIdentity(String name, String phone) {
        this.name = name;
        this.phone = phone;
        this.isIdentityVerified = true;
    }

    public void updateProfile(String nickname, String usagePurpose) {
        this.nickname = nickname;
        this.usagePurpose = usagePurpose;
    }

    /**
     * 회원 탈퇴: row를 물리적으로 지우지 않고 개인정보만 지운 뒤 탈퇴 처리한다.
     * 매물/리뷰/채팅/알림 등 다른 테이블이 user_id를 참조하고 있어 물리 삭제하면 그 수만큼 FK 위반 위험이 생기고,
     * 신고·거래 기록 같은 감사 데이터도 보존해야 하므로 소프트 삭제로 처리한다.
     * 이메일/닉네임은 UNIQUE라 재가입 시 값이 겹치지 않도록 고유한 값으로 치환한다.
     */
    public void anonymize(String withdrawReason) {
        this.email = "withdrawn-" + this.id + "@deleted.homes";
        this.password = null;
        this.name = null;
        this.phone = null;
        this.nickname = null;
        this.providerId = null;
        this.refreshToken = null;
        this.deletedAt = LocalDateTime.now();
        this.withdrawReason = withdrawReason;
    }
}