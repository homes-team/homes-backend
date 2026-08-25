package com.homes.backend.domain.property.insight.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DobongAiDataset {
    private static final String RESOURCE_NAME = "dobong_houses_llm_input.jsonl";

    private final ObjectMapper objectMapper;
    private Map<String, Entry> entriesByAddress = Map.of();
    private LocalDateTime loadedAt;

    @PostConstruct
    void load() {
        Map<String, Entry> loaded = new HashMap<>();
        ClassPathResource resource = new ClassPathResource(RESOURCE_NAME);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                Entry entry = parse(objectMapper.readTree(line));
                loaded.merge(normalizeAddress(entry.address()), entry, Entry::mergeSameAddress);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("도봉구 AI 평가 데이터셋을 불러오지 못했습니다.", exception);
        }
        entriesByAddress = Map.copyOf(loaded);
        loadedAt = LocalDateTime.now();
    }

    public Optional<Entry> findByAddress(String address) {
        if (address == null || address.isBlank()) return Optional.empty();
        return Optional.ofNullable(entriesByAddress.get(normalizeAddress(address)));
    }

    public LocalDateTime loadedAt() {
        return loadedAt;
    }

    static String normalizeAddress(String address) {
        return address
                .trim()
                .replace("서울시", "서울특별시")
                .replaceAll("\\s+", "")
                .replaceAll("[,()]", "");
    }

    private Entry parse(JsonNode root) {
        JsonNode basic = root.path("basic_info");
        JsonNode scores = root.path("scores");
        JsonNode details = root.path("geospatial_details");
        JsonNode education = details.path("education");
        JsonNode transportation = details.path("transportation");
        JsonNode environment = details.path("environment");
        return new Entry(
                basic.path("address").asText(),
                nullableInt(basic.get("build_year")),
                scores.path("education").asDouble(),
                scores.path("transportation").asDouble(),
                scores.path("hospital").asDouble(),
                scores.path("mart").asDouble(),
                scores.path("culture").asDouble(),
                scores.path("park").asDouble(),
                education.path("nearest_school_dist_m").asDouble(),
                education.path("elementary_count_1km").asInt(),
                education.path("middle_count_1km").asInt(),
                education.path("high_count_1km").asInt(),
                transportation.path("nearest_subway").asText(),
                transportation.path("subway_dist_m").asDouble(),
                transportation.path("nearest_bus").asText(),
                transportation.path("bus_dist_m").asDouble(),
                environment.path("nearest_park").asText(),
                environment.path("park_dist_m").asDouble(),
                environment.path("nearest_hospital").asText(),
                environment.path("hospital_dist_m").asDouble()
        );
    }

    private Integer nullableInt(JsonNode node) {
        return node == null || node.isNull() ? null : node.asInt();
    }

    public record Entry(
            String address,
            Integer buildYear,
            double educationScore,
            double transportationScore,
            double hospitalScore,
            double martScore,
            double cultureScore,
            double parkScore,
            double nearestSchoolDistanceMeters,
            int elementaryCountWithin1km,
            int middleCountWithin1km,
            int highCountWithin1km,
            String nearestSubway,
            double subwayDistanceMeters,
            String nearestBus,
            double busDistanceMeters,
            String nearestPark,
            double parkDistanceMeters,
            String nearestHospital,
            double hospitalDistanceMeters
    ) {
        static Entry mergeSameAddress(Entry first, Entry second) {
            Integer mergedBuildYear = first.buildYear != null && first.buildYear.equals(second.buildYear)
                    ? first.buildYear
                    : null;
            return new Entry(
                    first.address, mergedBuildYear,
                    first.educationScore, first.transportationScore,
                    first.hospitalScore, first.martScore, first.cultureScore, first.parkScore,
                    first.nearestSchoolDistanceMeters, first.elementaryCountWithin1km,
                    first.middleCountWithin1km, first.highCountWithin1km,
                    first.nearestSubway, first.subwayDistanceMeters,
                    first.nearestBus, first.busDistanceMeters,
                    first.nearestPark, first.parkDistanceMeters,
                    first.nearestHospital, first.hospitalDistanceMeters
            );
        }
    }
}
