package com.homes.backend.domain.property.insight.service;

import com.homes.backend.domain.property.entity.PropertyDirection;
import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyEvaluationScorePolicyTest {
    private final PropertyEvaluationScorePolicy policy = new PropertyEvaluationScorePolicy();

    /**
     * Verifies direction baselines and relative-floor adjustments.
     */
    @Test
    void calculatesSunlightScoreFromDirectionAndFloorRatio() {
        assertThat(policy.calculateSunlightScore(PropertyDirection.SOUTH, 8, 10)).isEqualTo(100.0);
        assertThat(policy.calculateSunlightScore(PropertyDirection.EAST, 1, 10)).isEqualTo(70.0);
        assertThat(policy.calculateSunlightScore(PropertyDirection.NORTH, 5, 10)).isEqualTo(50.0);
    }

    /**
     * Verifies that absent direction data leaves the sunlight score pending.
     */
    @Test
    void keepsSunlightPendingWhenDirectionIsUnknown() {
        assertThat(policy.calculateSunlightScore(PropertyDirection.UNKNOWN, 8, 10)).isNull();
        assertThat(policy.calculateSunlightScore(null, 8, 10)).isNull();
    }

    /**
     * Verifies the construction and remodeling year weighting policy.
     */
    @Test
    void combinesBuildingAndRemodelingYears() {
        int currentYear = Year.now().getValue();
        assertThat(policy.calculateBuildingConditionScore(currentYear - 20, null)).isEqualTo(60.0);
        assertThat(policy.calculateBuildingConditionScore(currentYear - 20, currentYear)).isEqualTo(74.0);
    }

    /**
     * Verifies the lower, construction-year, and current-year boundaries.
     */
    @Test
    void validatesRemodelingYearBoundaries() {
        int currentYear = Year.now().getValue();
        assertThat(policy.isValidRemodelingYear(null, 2000)).isTrue();
        assertThat(policy.isValidRemodelingYear(1999, 2000)).isFalse();
        assertThat(policy.isValidRemodelingYear(currentYear + 1, 2000)).isFalse();
        assertThat(policy.isValidRemodelingYear(currentYear, 2000)).isTrue();
    }
}
