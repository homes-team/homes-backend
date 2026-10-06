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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/** Finds nearby parks through Kakao Local keyword search. */
@Slf4j
@Component
public class KakaoNearbyNatureProvider {
    static final String POLICY_VERSION = "NATIONWIDE_NATURE_V1";
    private static final String SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final int RADIUS_METERS = 2_000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String kakaoRestApiKey;

    @Autowired
    public KakaoNearbyNatureProvider(
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

    KakaoNearbyNatureProvider(RestTemplate restTemplate, ObjectMapper objectMapper, String kakaoRestApiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.kakaoRestApiKey = kakaoRestApiKey;
    }

    public Optional<Result> evaluate(Point coordinate) {
        if (coordinate == null || !Double.isFinite(coordinate.getX()) || !Double.isFinite(coordinate.getY())
                || !StringUtils.hasText(kakaoRestApiKey)) {
            return Optional.empty();
        }

        URI uri = UriComponentsBuilder.fromUriString(SEARCH_URL)
                .queryParam("query", "공원")
                .queryParam("x", coordinate.getX())
                .queryParam("y", coordinate.getY())
                .queryParam("radius", RADIUS_METERS)
                .queryParam("sort", "distance")
                .queryParam("size", 1)
                .encode(StandardCharsets.UTF_8)
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
            String nearestName = null;
            double nearestDistance = Double.NaN;
            if (documents.isArray() && !documents.isEmpty()) {
                JsonNode nearest = documents.get(0);
                nearestName = nearest.path("place_name").asText(null);
                nearestDistance = nearest.path("distance").asDouble(Double.NaN);
            }

            double score = score(count, nearestDistance);
            String nearestValue = nearestName == null || !Double.isFinite(nearestDistance)
                    ? "2km 내 검색 결과 없음"
                    : nearestName + " · " + Math.round(nearestDistance) + "m";
            List<ScoreEvidence> evidence = List.of(
                    new ScoreEvidence("NEAREST_PARK_DISTANCE", "가장 가까운 공원", nearestValue,
                            null, distanceCriterion(nearestDistance),
                            round1(distanceScore(nearestDistance) * 0.7), "카카오 로컬 공원 검색"),
                    new ScoreEvidence("PARK_COUNT_WITHIN_2KM", "2km 내 공원", count + "곳",
                            null, "공원 밀도", round1(densityScore(count) * 0.3), "카카오 로컬 공원 검색")
            );
            String description = nearestName == null || !Double.isFinite(nearestDistance)
                    ? "반경 2km에서 확인된 공원이 없습니다."
                    : "가장 가까운 공원은 " + nearestName + "이며 약 "
                            + Math.round(nearestDistance) + "m 거리입니다. 반경 2km 내 공원은 " + count + "곳입니다.";
            ScoreCalculation calculation = new ScoreCalculation(
                    "최근접 공원 거리 점수×70% + 2km 내 공원 수 점수×30% = " + score,
                    POLICY_VERSION);
            return Optional.of(new Result(score, description, evidence, calculation));
        } catch (Exception exception) {
            log.warn("카카오 주변 공원 조회 실패: longitude={}, latitude={}",
                    coordinate.getX(), coordinate.getY(), exception);
            return Optional.empty();
        }
    }

    private double score(int count, double distanceMeters) {
        return round1(distanceScore(distanceMeters) * 0.7 + densityScore(count) * 0.3);
    }

    private double distanceScore(double distanceMeters) {
        if (!Double.isFinite(distanceMeters)) return 20.0;
        if (distanceMeters <= 300) return 100.0;
        if (distanceMeters <= 700) return 85.0;
        if (distanceMeters <= 1_200) return 70.0;
        return 50.0;
    }

    private double densityScore(int count) {
        return Math.min(100.0, Math.max(0, count) * 10.0);
    }

    private String distanceCriterion(double distanceMeters) {
        if (!Double.isFinite(distanceMeters)) return "2km 내 검색 결과 없음";
        if (distanceMeters <= 300) return "300m 이하";
        if (distanceMeters <= 700) return "700m 이하";
        if (distanceMeters <= 1_200) return "1.2km 이하";
        return "2km 이하";
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record Result(
            double score,
            String description,
            List<ScoreEvidence> evidence,
            ScoreCalculation calculation
    ) {
    }
}
