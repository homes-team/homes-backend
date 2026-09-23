package com.homes.backend.domain.property.insight.service;

import com.homes.backend.domain.property.entity.PropertyDirection;
import org.springframework.stereotype.Component;

import java.time.Year;

/**
 * 매물 자체 입력값으로 산정하는 AI 다면평가 점수 정책.
 *
 * <p>외부 입지 데이터와 분리해 방향·층수·건축연도·리모델링 연도의 가중치를 한 곳에서 관리한다.</p>
 */
@Component
public class PropertyEvaluationScorePolicy {
    private static final int MIN_REASONABLE_YEAR = 1800;
    private static final double BUILDING_YEAR_WEIGHT = 0.65;
    private static final double REMODELING_YEAR_WEIGHT = 0.35;

    /**
     * 주실 방향의 기본 점수에 전체 층수 대비 현재 층수 보정을 더한다.
     */
    public Double calculateSunlightScore(PropertyDirection direction, Integer currentFloor, Integer totalFloors) {
        if (direction == null || direction == PropertyDirection.UNKNOWN) {
            return null;
        }

        double directionScore = switch (direction) {
            case SOUTH -> 90.0;
            case SOUTHEAST, SOUTHWEST -> 85.0;
            case EAST -> 75.0;
            case WEST -> 70.0;
            case NORTHEAST, NORTHWEST -> 60.0;
            case NORTH -> 45.0;
            case UNKNOWN -> throw new IllegalStateException("UNKNOWN direction was handled before scoring");
        };

        double floorAdjustment = calculateFloorAdjustment(currentFloor, totalFloors);
        return round1(clamp(directionScore + floorAdjustment));
    }

    /**
     * 건축연도 점수에 리모델링 연도 점수를 일부 반영한다.
     * 구조적 노후도 비중을 유지하기 위해 건축연도 65%, 리모델링 연도 35%로 계산한다.
     */
    public Double calculateBuildingConditionScore(Integer buildingYear, Integer remodelingYear) {
        if (buildingYear == null) {
            return null;
        }

        double buildingScore = scoreYear(buildingYear);
        if (remodelingYear == null) {
            return round1(buildingScore);
        }

        double remodelingScore = scoreYear(remodelingYear);
        return round1(clamp(buildingScore * BUILDING_YEAR_WEIGHT
                + remodelingScore * REMODELING_YEAR_WEIGHT));
    }

    /**
     * 입력 가능한 연도 범위와 건축연도 이후인지 검증한다.
     */
    public boolean isValidRemodelingYear(Integer remodelingYear, Integer buildingYear) {
        if (remodelingYear == null) {
            return true;
        }
        int currentYear = Year.now().getValue();
        if (remodelingYear < MIN_REASONABLE_YEAR || remodelingYear > currentYear) {
            return false;
        }
        return buildingYear == null || remodelingYear >= buildingYear;
    }

    /**
     * Calculates the sunlight adjustment from a property's relative floor height.
     */
    private double calculateFloorAdjustment(Integer currentFloor, Integer totalFloors) {
        if (currentFloor == null || totalFloors == null || currentFloor <= 0 || totalFloors <= 0) {
            return 0.0;
        }
        double floorRatio = Math.min(1.0, currentFloor / (double) totalFloors);
        if (floorRatio >= 0.7) return 10.0;
        if (floorRatio >= 0.4) return 5.0;
        return currentFloor <= 2 ? -5.0 : 0.0;
    }

    /**
     * Converts a construction or remodeling year to the bounded condition scale.
     */
    private double scoreYear(int year) {
        int age = Math.max(0, Year.now().getValue() - year);
        return clamp(100.0 - age * 2.0);
    }

    /**
     * Restricts a rule score to the policy's 20-to-100 range.
     */
    private double clamp(double score) {
        return Math.max(20.0, Math.min(100.0, score));
    }

    /**
     * Rounds a score to one decimal place.
     */
    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
