package com.homes.backend.domain.property.insight.entity;

import com.homes.backend.domain.property.entity.Property;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "property_ai_evaluations")
public class PropertyAiEvaluation {
    @Id
    @Column(name = "property_id")
    private Long propertyId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    private Double schoolScore;
    private Double transportScore;
    private Double natureScore;
    private Double sunlightScore;
    private Double buildingConditionScore;
    private Double infrastructureScore;

    @Column(nullable = false)
    private String scoreVersion;

    @Column(nullable = false)
    private LocalDateTime generatedAt;
}
