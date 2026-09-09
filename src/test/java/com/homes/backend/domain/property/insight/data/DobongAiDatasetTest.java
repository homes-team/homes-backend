package com.homes.backend.domain.property.insight.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DobongAiDatasetTest {
    /**
     * Verifies that the bundled dataset loads and supports normalized address lookup.
     */
    @Test
    void loadsBundledDatasetAndMatchesNormalizedAddress() {
        DobongAiDataset dataset = new DobongAiDataset(new ObjectMapper());
        dataset.load();

        var entry = dataset.findByAddress("서울시 도봉구 방학동 방학로11길 36");

        assertThat(entry).isPresent();
        assertThat(entry.orElseThrow().educationScore()).isEqualTo(70.0);
        assertThat(entry.orElseThrow().transportationScore()).isEqualTo(13.3);
        assertThat(entry.orElseThrow().nearestSubway()).isNotBlank();
    }

    /**
     * Verifies that equivalent Seoul address formats normalize to the same key.
     */
    @Test
    void normalizesWhitespaceAndSeoulAbbreviation() {
        assertThat(DobongAiDataset.normalizeAddress(" 서울시 도봉구 방학동 "))
                .isEqualTo(DobongAiDataset.normalizeAddress("서울특별시도봉구방학동"));
    }
}
