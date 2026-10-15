package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.insight.dto.NearbyFacilitiesRespDto.FacilityType;
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

/** Provides coordinate-bearing Kakao Local results for the infrastructure map. */
@Slf4j
@Component
public class KakaoNearbyFacilityProvider {
    private static final String CATEGORY_URL = "https://dapi.kakao.com/v2/local/search/category.json";
    private static final String KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String kakaoRestApiKey;

    @Autowired
    public KakaoNearbyFacilityProvider(
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

    KakaoNearbyFacilityProvider(RestTemplate restTemplate, ObjectMapper objectMapper, String kakaoRestApiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.kakaoRestApiKey = kakaoRestApiKey;
    }

    public List<Place> findNearby(Point center, FacilityType type, int radiusMeters, int limit) {
        if (!hasValidCoordinate(center) || !StringUtils.hasText(kakaoRestApiKey)) {
            return List.of();
        }

        URI uri = buildUri(center, type, radiusMeters, limit);
        if (uri == null) return List.of();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<Void>(headers), String.class);
            JsonNode documents = objectMapper.readTree(response.getBody()).path("documents");
            if (!documents.isArray()) return List.of();

            List<Place> places = new ArrayList<>();
            for (JsonNode document : documents) {
                double longitude = document.path("x").asDouble(Double.NaN);
                double latitude = document.path("y").asDouble(Double.NaN);
                String name = document.path("place_name").asText(null);
                if (!Double.isFinite(longitude) || !Double.isFinite(latitude) || !StringUtils.hasText(name)) {
                    continue;
                }
                double distance = document.path("distance").asDouble(Double.NaN);
                if (!Double.isFinite(distance)) {
                    distance = haversineMeters(center.getY(), center.getX(), latitude, longitude);
                }
                String address = document.path("road_address_name").asText("");
                if (!StringUtils.hasText(address)) address = document.path("address_name").asText(null);
                String id = document.path("id").asText(name + "@" + latitude + "," + longitude);
                places.add(new Place(id, name, latitude, longitude, Math.round(distance), address));
            }
            return List.copyOf(places);
        } catch (Exception exception) {
            log.warn("카카오 지도 시설 조회 실패: type={}, longitude={}, latitude={}",
                    type, center.getX(), center.getY(), exception);
            return List.of();
        }
    }

    private URI buildUri(Point center, FacilityType type, int radiusMeters, int limit) {
        String categoryCode = switch (type) {
            case SCHOOL -> "SC4";
            case HOSPITAL -> "HP8";
            case CULTURE -> "CT1";
            case MART -> "MT1";
            default -> null;
        };
        UriComponentsBuilder builder;
        if (type == FacilityType.PARK) {
            builder = UriComponentsBuilder.fromUriString(KEYWORD_URL).queryParam("query", "공원");
        } else if (categoryCode != null) {
            builder = UriComponentsBuilder.fromUriString(CATEGORY_URL)
                    .queryParam("category_group_code", categoryCode);
        } else {
            return null;
        }
        return builder
                .queryParam("x", center.getX())
                .queryParam("y", center.getY())
                .queryParam("radius", radiusMeters)
                .queryParam("sort", "distance")
                .queryParam("size", limit)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
    }

    private boolean hasValidCoordinate(Point coordinate) {
        return coordinate != null && Double.isFinite(coordinate.getX()) && Double.isFinite(coordinate.getY());
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6_371_000.0;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public record Place(
            String id,
            String name,
            double latitude,
            double longitude,
            long distanceMeters,
            String address
    ) {}
}
