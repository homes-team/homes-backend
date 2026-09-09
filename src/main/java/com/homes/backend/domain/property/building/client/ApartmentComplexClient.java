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
    public ApartmentComplexClient(PublicDataApiProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    public Optional<ApartmentComplex> findByAddress(ResolvedAddress address) {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getApartmentListUrl() + "/getLegaldongAptList")
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

    private ApartmentComplex map(JsonNode item) {
        return new ApartmentComplex(
                text(item, "kaptCode"),
                text(item, "kaptName"),
                text(item, "doroJuso", "as1", "address")
        );
    }

    private int matchScore(ApartmentComplex complex, ResolvedAddress address) {
        int score = 0;
        String target = normalize(address.normalizedAddress());
        String candidate = normalize(complex.address());
        if (!target.isBlank() && !candidate.isBlank() && (target.contains(candidate) || candidate.contains(target))) score += 10;
        if (address.buildingName() != null && complex.name() != null
                && normalize(address.buildingName()).equals(normalize(complex.name()))) score += 5;
        return score;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s(),]", "").replace("서울시", "서울특별시");
    }
}
