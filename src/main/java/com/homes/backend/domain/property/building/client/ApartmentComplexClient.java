package com.homes.backend.domain.property.building.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.building.config.PublicDataApiProperties;
import com.homes.backend.global.geocoding.ResolvedAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Optional;

@Slf4j
@Component
public class ApartmentComplexClient extends PublicDataClientSupport {
    /**
     * Creates a client for the K-apt complex-list endpoint.
     */
    public ApartmentComplexClient(PublicDataApiProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    /**
     * Finds the best K-apt complex that matches the resolved property address.
     */
    public Optional<ApartmentComplex> findByAddress(ResolvedAddress address) {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getApartmentListUrl() + "/getLegaldongAptList4")
                .queryParam("serviceKey", serviceKey())
                .queryParam("bjdCode", address.legalDongCode())
                .queryParam("numOfRows", 100)
                .queryParam("pageNo", 1)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
        try {
            return items(restTemplate.getForObject(uri, String.class)).stream()
                    .map(this::map)
                    .filter(complex -> complex.kaptCode() != null)
                    .filter(complex -> matchScore(complex, address) > 0)
                    .max(Comparator.comparingInt(complex -> matchScore(complex, address)));
        } catch (Exception exception) {
            log.warn("공동주택 단지 목록 조회 실패: legalDongCode={}", address.legalDongCode(), exception);
            return Optional.empty();
        }
    }

    /**
     * Maps a complex-list response item to a candidate complex.
     */
    private ApartmentComplex map(JsonNode item) {
        return new ApartmentComplex(
                text(item, "kaptCode"),
                text(item, "kaptName"),
                joinAddress(item)
        );
    }

    /**
     * Scores an address candidate, favoring exact addresses and matching building names.
     */
    private int matchScore(ApartmentComplex complex, ResolvedAddress address) {
        int score = 0;
        String target = normalize(address.normalizedAddress());
        String candidate = normalize(complex.address());
        if (!target.isBlank() && target.equals(candidate)) score += 10;
        if (address.buildingName() != null && complex.name() != null
                && matchesBuildingName(address.buildingName(), complex.name())) score += 20;
        return score;
    }

    /**
     * Joins the address fragments returned by the K-apt list API.
     */
    private String joinAddress(JsonNode item) {
        StringBuilder address = new StringBuilder();
        for (String field : new String[]{"as1", "as2", "as3", "as4"}) {
            String part = text(item, field);
            if (part != null) {
                if (!address.isEmpty()) address.append(' ');
                address.append(part);
            }
        }
        return address.isEmpty() ? text(item, "doroJuso", "address") : address.toString();
    }

    /**
     * Determines whether two apartment names describe the same complex.
     */
    private boolean matchesBuildingName(String target, String candidate) {
        String normalizedTarget = normalizeBuildingName(target);
        String normalizedCandidate = normalizeBuildingName(candidate);
        return !normalizedTarget.isBlank() && !normalizedCandidate.isBlank()
                && (normalizedTarget.equals(normalizedCandidate)
                || normalizedTarget.contains(normalizedCandidate)
                || normalizedCandidate.contains(normalizedTarget));
    }

    /**
     * Normalizes an apartment name while ignoring its common suffix.
     */
    private static String normalizeBuildingName(String value) {
        return normalize(value).replace("아파트", "");
    }

    /**
     * Normalizes address and name text for deterministic matching.
     */
    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s(),]", "").replace("서울시", "서울특별시");
    }
}
