package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.global.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    @Test
    void usesModeSpecificGridAndQueryTimeout() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AtomicReference<String> sql = new AtomicReference<>();
        AtomicReference<PreparedStatementSetter> setter = new AtomicReference<>();
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    sql.set(invocation.getArgument(0));
                    setter.set(invocation.getArgument(1));
                    return List.of();
                });
        PgRoutingIsochroneProvider target = new PgRoutingIsochroneProvider(jdbcTemplate, new ObjectMapper());
        var center = new GeometryFactory().createPoint(new Coordinate(127.0, 37.5));

        assertThatThrownBy(() -> target.calculate(center, PgRoutingIsochroneProvider.Mode.WALK, 300))
                .isInstanceOf(CustomException.class);

        assertThat(sql.get())
                .contains("ST_Transform(vertex.geom, 3857),\n               50")
                .contains("ST_Area(CAST(ST_Transform(geometry, 4326) AS geography))");

        assertThatThrownBy(() -> target.calculate(center, PgRoutingIsochroneProvider.Mode.DRIVE, 600))
                .isInstanceOf(CustomException.class);
        assertThat(sql.get()).contains("ST_Transform(vertex.geom, 3857),\n               200");

        PreparedStatement statement = mock(PreparedStatement.class);
        setter.get().setValues(statement);
        verify(statement).setQueryTimeout(30);
    }

    @Test
    void convertsQueryTimeoutToRoutingUnavailable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenThrow(new QueryTimeoutException("timed out"));
        PgRoutingIsochroneProvider target = new PgRoutingIsochroneProvider(jdbcTemplate, new ObjectMapper());
        var center = new GeometryFactory().createPoint(new Coordinate(127.0, 37.5));

        assertThatThrownBy(() -> target.calculate(center, PgRoutingIsochroneProvider.Mode.DRIVE, 600))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(PropertyErrorCode.ISOCHRONE_ROUTING_UNAVAILABLE));
    }
}
