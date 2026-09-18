package com.homes.backend.domain.property.registry.entity;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "property_registry_risks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PropertyRegistryRisk extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registry_risk_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", unique = true, nullable = false)
    private Property property;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "mortgage_count", nullable = false)
    private Integer mortgageCount = 0; // 저당권 건수

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "seizure_count", nullable = false)
    private Integer seizureCount = 0; // 가압류 건수

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RegistryRiskLevel riskLevel; // Property.status와 같은 방식(문자열 저장)

    @Column(name = "summary", nullable = false, length = 255)
    private String summary; // 스캔 요약
}
