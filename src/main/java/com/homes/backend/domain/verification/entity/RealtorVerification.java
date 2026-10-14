package com.homes.backend.domain.verification.entity;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.realtor.entity.Agent;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "realtor_verification")
@EntityListeners(AuditingEntityListener.class)
/**
 * 중개사 현장 인증
 */
public class RealtorVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "realtor_verification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    private String photoUrl; // 현장에서 찍은 사진 (Presigned URL 연동)

    @Column(columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point location; // 현장에서 전송한 GPS 좌표

    private Double distanceMeter; // 실제 매물 좌표와의 오차 반경 (m)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime requestedAt;
}

