package com.homes.backend.domain.property.insight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "property_ai_reports")
public class PropertyAiReport {
    @Id
    @Column(name = "property_id")
    private Long propertyId;

    @Column(nullable = false, length = 64)
    private String inputHash;

    @Column(nullable = false, length = 100)
    private String modelVersion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String weaknessesJson;

    @Column(length = 500)
    private String notice;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    public PropertyAiReport(
            Long propertyId,
            String inputHash,
            String modelVersion,
            String summary,
            String strengthsJson,
            String weaknessesJson,
            String notice,
            LocalDateTime generatedAt
    ) {
        this.propertyId = propertyId;
        update(inputHash, modelVersion, summary, strengthsJson, weaknessesJson, notice, generatedAt);
    }

    public void update(
            String inputHash,
            String modelVersion,
            String summary,
            String strengthsJson,
            String weaknessesJson,
            String notice,
            LocalDateTime generatedAt
    ) {
        this.inputHash = inputHash;
        this.modelVersion = modelVersion;
        this.summary = summary;
        this.strengthsJson = strengthsJson;
        this.weaknessesJson = weaknessesJson;
        this.notice = notice;
        this.generatedAt = generatedAt;
    }
}
