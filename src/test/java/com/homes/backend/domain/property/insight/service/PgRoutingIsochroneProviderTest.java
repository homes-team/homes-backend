package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PgRoutingIsochroneProviderTest {
    private final PgRoutingIsochroneProvider provider = new PgRoutingIsochroneProvider(
            mock(JdbcTemplate.class), new ObjectMapper());

    @Test
    void parsesGeoJsonExteriorRingInLongitudeLatitudeOrder() {
        var ring = provider.parseExteriorRing("""
                {"type":"Polygon","coordinates":[[[127.0,37.5],[127.1,37.5],[127.0,37.5]]]}
                """);

        assertThat(ring).containsExactly(
                java.util.List.of(127.0, 37.5),
                java.util.List.of(127.1, 37.5),
                java.util.List.of(127.0, 37.5)
        );
    }

    @Test
    void ignoresMalformedGeoJson() {
        assertThat(provider.parseExteriorRing("not-json")).isEmpty();
    }
}
