package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto.ScoreCalculation;
import com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto.ScoreEvidence;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Finds nearby hospitals, large marts, and cultural facilities through Kakao Local API. */
@Slf4j
@Component
public class KakaoNearbyInfrastructureProvider {
    static final String POLICY_VERSION = "NATIONWIDE_INFRASTRUCTURE_V1";
    private static final String SEARCH_URL = "https://dapi.kakao.com/v2/local/search/category.json";
    private static final int RADIUS_METERS = 1_000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String kakaoRestApiKey;

    @Autowired
    public KakaoNearbyInfrastructureProvider(
            ObjectMapper objectMapper,
            @Value("${kakao.rest-api-key:}") String kakaoRestApiKey
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2_000);
        factory.setReadTimeout(3_000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = objectMapper;
        this.kakaoRestApiKey = kakaoRestApiKey;
    }

    KakaoNearbyInfrastructureProvider(RestTemplate restTemplate, ObjectMapper objectMapper, String kakaoRestApiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.kakaoRestApiKey = kakaoRestApiKey;
    }

    public Optional<Result> evaluate(Point coordinate) {
        if (coordinate == null || !Double.isFinite(coordinate.getX()) || !Double.isFinite(coordinate.getY())
                || !StringUtils.hasText(kakaoRestApiKey)) {
            return Optional.empty();
        }

        List<NearbyCategory> categories = new ArrayList<>();
        int successfulQueries = 0;
        for (InfrastructureType type : InfrastructureType.values()) {
            Optional<NearbyCategory> category = search(coordinate, type);
            if (category.isPresent()) {
                successfulQueries++;
                categories.add(category.get());
            }
        }
        if (successfulQueries == 0) return Optional.empty();

        double score = round1(categories.stream().mapToDouble(this::categoryScore).average().orElse(0));
        List<ScoreEvidence> evidence = categories.stream().map(category -> {
            String value = category.count() + "곳";
            if (category.nearestName() != null) {
                value += " · 가까운 곳 " + category.nearestName() + " "
                        + Math.round(category.nearestDistanceMeters()) + "m";
            }
            return new ScoreEvidence(
                    category.type().name() + "_COUNT_WITHIN_1KM",
                    "1km 내 " + category.type().label(),
                    value,
                    null,
                    "반경 1km",
                    round1(categoryScore(category) / categories.size()),
                    "카카오 로컬 장소 검색"
            );
        }).toList();
        String description = categories.stream()
                .map(category -> category.type().label() + " " + category.count() + "곳")
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return Optional.of(new Result(
                score,
                "반경 1km 안에 " + description + "이 있습니다.",
                evidence,
                new ScoreCalculation("시설별 접근 거리와 반경 1km 내 개수의 평균 = " + score, POLICY_VERSION)
        ));
    }

    private Optional<NearbyCategory> search(Point coordinate, InfrastructureType type) {
        URI uri = UriComponentsBuilder.fromUriString(SEARCH_URL)
                .queryParam("category_group_code", type.categoryCode())
                .queryParam("x", coordinate.getX())
                .queryParam("y", coordinate.getY())
                .queryParam("radius", RADIUS_METERS)
                .queryParam("sort", "distance")
                .queryParam("size", 1)
                .build()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<Void>(headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            int count = root.path("meta").path("total_count").asInt(0);
            JsonNode documents = root.path("documents");
            if (documents.isArray() && !documents.isEmpty()) {
                JsonNode nearest = documents.get(0);
                return Optional.of(new NearbyCategory(type, count,
                        nearest.path("place_name").asText(null),
                        nearest.path("distance").asDouble(Double.NaN)));
            }
            return Optional.of(new NearbyCategory(type, count, null, Double.NaN));
        } catch (Exception exception) {
            log.warn("카카오 인프라 조회 실패: type={}, longitude={}, latitude={}",
                    type, coordinate.getX(), coordinate.getY(), exception);
            return Optional.empty();
        }
    }

    private double categoryScore(NearbyCategory category) {
        double distanceScore;
        if (!Double.isFinite(category.nearestDistanceMeters())) distanceScore = 0;
        else if (category.nearestDistanceMeters() <= 300) distanceScore = 100;
        else if (category.nearestDistanceMeters() <= 600) distanceScore = 80;
        else distanceScore = 60;
        double densityScore = Math.min(100, category.count() * 10.0);
        return distanceScore * 0.7 + densityScore * 0.3;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    enum InfrastructureType {
        HOSPITAL("HP8", "병원"),
        MART("MT1", "대형마트"),
        CULTURE("CT1", "문화시설");

        private final String categoryCode;
        private final String label;

        InfrastructureType(String categoryCode, String label) {
            this.categoryCode = categoryCode;
            this.label = label;
        }

        String categoryCode() { return categoryCode; }
        String label() { return label; }
    }

    record NearbyCategory(
            InfrastructureType type,
            int count,
            String nearestName,
            double nearestDistanceMeters
    ) {}

    public record Result(
            double score,
            String description,
            List<ScoreEvidence> evidence,
            ScoreCalculation calculation
    ) {}
}
