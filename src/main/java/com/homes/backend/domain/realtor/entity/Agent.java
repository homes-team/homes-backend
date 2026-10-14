package com.homes.backend.domain.realtor.entity;

import com.homes.backend.domain.user.entity.User;
import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "agent")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Agent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agent_id") // 프로필 ID
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(name = "is_verified", nullable = false) // 중개사 자격 인증 여부(디폴트 false)
    private boolean isVerified = false;

    @Column(name = "business_num", nullable = false, unique = true, length = 50) // 사업자 등록번호
    private String businessNum;

    @Column(name = "profile_image_url", length = 255) // 중개사 프로필 사진 경로
    private String profileImageUrl;

    @Column(name = "business_cert_url", length = 255) // 사업자등록증 서류 이미지 경로
    private String businessCertUrl;

    @Column(name = "agent_cert_url", length = 255) // 중개사무소 등록증 서류 이미지 경로
    private String agentCertUrl;

    @Column(name = "office_name", length = 50) // 중개사무소 이름
    private String officeName;

    @Column(name = "office_address", length = 255) // 중개사무소 주소
    private String officeAddress;

    @Column(name = "office_latitude") // 중개사무소 위도
    private Double officeLatitude;

    @Column(name = "office_longitude") // 중개사무소 경도
    private Double officeLongitude;

    @Builder.Default
    @ColumnDefault("36.5")
    @Column(name = "reputation_score", nullable = false) // 평판 점수(매너온도 방식, 기본값 36.5)
    private Float reputationScore = 36.5f;

    /**
     * 마이페이지 회원정보(사무소 정보) 수정.
     * 사업자등록번호/인증서류/인증여부는 관리자 승인과 직결되므로 여기서 다루지 않는다.
     */
    public void updateOfficeProfile(String officeName, String officeAddress, Double officeLatitude, Double officeLongitude) {
        this.officeName = officeName;
        this.officeAddress = officeAddress;
        this.officeLatitude = officeLatitude;
        this.officeLongitude = officeLongitude;
    }

    /**
     * 리뷰 점수(0.0~5.0)를 매너온도 증감치로 환산. 중립점(2.5)보다 높으면 오르고 낮으면 내려간다.
     * 실제 반영은 동시 리뷰 간 lost update를 막기 위해 DB 원자적 UPDATE(AgentRepository.addToReputationScore)로 수행한다.
     */
    public static float calculateReputationDelta(float reviewScore) {
        return (reviewScore - 2.5f) * 0.1f;
    }
}
