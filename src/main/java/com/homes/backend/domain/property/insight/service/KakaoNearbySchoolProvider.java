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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Finds the nearest elementary, middle, and high school around a property through Kakao Local API.
 */
@Slf4j
@Component
public class KakaoNearbySchoolProvider {
    static final String POLICY_VERSION = "NATIONWIDE_SCHOOL_V1";
    private static final String SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final String CATEGORY_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/category.json";
    private static final int SEARCH_RADIUS_METERS = 20_000;
    private static final int COUNT_RADIUS_METERS = 1_000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String kakaoRestApiKey;

    @Autowired
    public KakaoNearbySchoolProvider(
            ObjectMapper objectMapper,
            @Value("${kakao.rest-api-key:}") String kakaoRestApiKey
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(3_000);
        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = objectMapper;
        this.kakaoRestApiKey = kakaoRestApiKey;
    }

    KakaoNearbySchoolProvider(RestTemplate restTemplate, ObjectMapper objectMapper, String kakaoRestApiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.kakaoRestApiKey = kakaoRestApiKey;
    }

    public Optional<Result> evaluate(Point coordinate) {
        if (!hasValidCoordinate(coordinate) || !StringUtils.hasText(kakaoRestApiKey)) {
            return Optional.empty();
        }

        List<SchoolPlace> schools = new ArrayList<>();
        findNearest(coordinate, SchoolLevel.ELEMENTARY).ifPresent(schools::add);
        findNearest(coordinate, SchoolLevel.MIDDLE).ifPresent(schools::add);
        findNearest(coordinate, SchoolLevel.HIGH).ifPresent(schools::add);
        if (schools.isEmpty()) {
            return Optional.empty();
        }

        double score = round1(schools.stream()
                .mapToDouble(school -> distanceScore(school.distanceMeters()))
                .average()
                .orElse(0.0));
        List<ScoreEvidence> evidence = new ArrayList<>(schools.stream()
                .map(school -> new ScoreEvidence(
                        "NEAREST_" + school.level().name() + "_SCHOOL",
                        "가장 가까운 " + school.level().label(),
                        school.name() + " · " + Math.round(school.distanceMeters()) + "m",
                        null,
                        distanceCriterion(school.distanceMeters()),
                        round1(distanceScore(school.distanceMeters()) / schools.size()),
                        "카카오 로컬 학교 검색"
                ))
                .toList());
        countSchoolsWithinOneKilometer(coordinate).ifPresent(count -> evidence.add(new ScoreEvidence(
                "SCHOOL_COUNT_WITHIN_1KM",
                "1km 내 학교",
                count + "곳",
                null,
                "초·중·고교 합계",
                null,
                "카카오 로컬 학교 검색"
        )));
        String description = schools.stream()
                .map(school -> school.level().shortLabel() + " " + school.name()
                        + "(약 " + Math.round(school.distanceMeters()) + "m)")
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        ScoreCalculation calculation = new ScoreCalculation(
                "초·중·고 최근접 학교 거리 점수 평균 = " + score,
                POLICY_VERSION
        );
        return Optional.of(new Result(score, "가장 가까운 학교는 " + description + "입니다.",
                List.copyOf(evidence), calculation));
    }

    private Optional<Integer> countSchoolsWithinOneKilometer(Point coordinate) {
        URI uri = UriComponentsBuilder.fromUriString(CATEGORY_SEARCH_URL)
                .queryParam("category_group_code", "SC4")
                .queryParam("x", coordinate.getX())
                .queryParam("y", coordinate.getY())
                .queryParam("radius", COUNT_RADIUS_METERS)
                .queryParam("sort", "distance")
                .queryParam("size", 1)
                .build()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<Void>(headers), String.class);
            JsonNode meta = objectMapper.readTree(response.getBody()).path("meta");
            return meta.has("total_count") ? Optional.of(meta.path("total_count").asInt()) : Optional.empty();
        } catch (Exception exception) {
            log.warn("카카오 반경 내 학교 수 조회 실패: longitude={}, latitude={}",
                    coordinate.getX(), coordinate.getY(), exception);
            return Optional.empty();
        }
    }

    private Optional<SchoolPlace> findNearest(Point coordinate, SchoolLevel level) {
        URI uri = UriComponentsBuilder.fromUriString(SEARCH_URL)
                .queryParam("query", level.searchKeyword())
                .queryParam("category_group_code", "SC4")
                .queryParam("x", coordinate.getX())
                .queryParam("y", coordinate.getY())
                .queryParam("radius", SEARCH_RADIUS_METERS)
                .queryParam("sort", "distance")
                .queryParam("size", 15)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<Void>(headers), String.class);
            JsonNode documents = objectMapper.readTree(response.getBody()).path("documents");
            if (!documents.isArray()) return Optional.empty();
            for (JsonNode document : documents) {
                if (!document.path("category_name").asText().contains(level.categoryMarker())) continue;
                String name = document.path("place_name").asText();
                double distance = document.path("distance").asDouble(Double.NaN);
                if (StringUtils.hasText(name) && Double.isFinite(distance)) {
                    return Optional.of(new SchoolPlace(level, name, distance));
                }
            }
        } catch (Exception exception) {
            log.warn("카카오 최근접 학교 조회 실패: level={}, longitude={}, latitude={}",
                    level, coordinate.getX(), coordinate.getY(), exception);
        }
        return Optional.empty();
    }

    private boolean hasValidCoordinate(Point coordinate) {
        return coordinate != null && Double.isFinite(coordinate.getX()) && Double.isFinite(coordinate.getY());
    }

    private double distanceScore(double distanceMeters) {
        if (distanceMeters <= 500) return 100.0;
        if (distanceMeters <= 1_000) return 80.0;
        if (distanceMeters <= 2_000) return 60.0;
        if (distanceMeters <= 5_000) return 40.0;
        return 20.0;
    }

    private String distanceCriterion(double distanceMeters) {
        if (distanceMeters <= 500) return "500m 이하";
        if (distanceMeters <= 1_000) return "1km 이하";
        if (distanceMeters <= 2_000) return "2km 이하";
        if (distanceMeters <= 5_000) return "5km 이하";
        return "5km 초과";
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    enum SchoolLevel {
        ELEMENTARY("초등학교", "초등학교", "초"),
        MIDDLE("중학교", "중학교", "중"),
        HIGH("고등학교", "고등학교", "고");

        private final String searchKeyword;
        private final String categoryMarker;
        private final String shortLabel;

        SchoolLevel(String searchKeyword, String categoryMarker, String shortLabel) {
            this.searchKeyword = searchKeyword;
            this.categoryMarker = categoryMarker;
            this.shortLabel = shortLabel;
        }

        String searchKeyword() { return searchKeyword; }
        String categoryMarker() { return categoryMarker; }
        String label() { return searchKeyword; }
        String shortLabel() { return shortLabel; }
    }

    record SchoolPlace(SchoolLevel level, String name, double distanceMeters) {}

    public record Result(
            double score,
            String description,
            List<ScoreEvidence> evidence,
            ScoreCalculation calculation
    ) {}
}
