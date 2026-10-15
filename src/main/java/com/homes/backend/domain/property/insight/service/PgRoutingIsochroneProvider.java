package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgRoutingIsochroneProvider {
    private static final String WALK_EDGE_SQL = """
            SELECT gid AS id, source, target,
                   CASE WHEN class_id IN (101, 102, 104, 105) THEN -1 ELSE length_m / 1.4 END AS cost,
                   CASE WHEN class_id IN (101, 102, 104, 105) THEN -1 ELSE length_m / 1.4 END AS reverse_cost
            FROM ways
            """;
    private static final String DRIVE_EDGE_SQL = """
            SELECT gid AS id, source, target, cost_s AS cost, reverse_cost_s AS reverse_cost
            FROM ways
            """;
    private static final String ISOCHRONE_SQL_TEMPLATE = """
            WITH origin AS (
                SELECT id
                FROM ways_vertices_pgr
                WHERE ST_DWithin(
                    CAST(the_geom AS geography),
                    CAST(ST_SetSRID(ST_MakePoint(?, ?), 4326) AS geography),
                    1000
                )
                ORDER BY the_geom <-> ST_SetSRID(ST_MakePoint(?, ?), 4326)
                LIMIT 1
            ),
            reachable AS (
                SELECT route.node
                FROM origin
                CROSS JOIN LATERAL pgr_drivingDistance(
                    '%s',
                    origin.id,
                    ?,
                    directed := true
                ) AS route
            ),
            point_cloud AS (
                SELECT ST_Collect(ST_Transform(vertex.the_geom, 3857)) AS geometry
                FROM ways_vertices_pgr vertex
                JOIN reachable ON reachable.node = vertex.id
            ),
            hull AS (
                SELECT ST_ConcaveHull(geometry, 0.30, false) AS geometry
                FROM point_cloud
                WHERE geometry IS NOT NULL
            ),
            polygon AS (
                SELECT CASE
                    WHEN GeometryType(geometry) = 'POLYGON' THEN geometry
                    ELSE ST_Buffer(geometry, 30)
                END AS geometry
                FROM hull
            )
            SELECT ST_AsGeoJSON(ST_Transform(geometry, 4326)) AS geojson,
                   ST_Area(geometry) AS area_square_meters
            FROM polygon
            WHERE geometry IS NOT NULL
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public Result calculate(Point center, Mode mode, int travelTimeSeconds) {
        String edgeSql = mode == Mode.WALK ? WALK_EDGE_SQL : DRIVE_EDGE_SQL;
        String sql = ISOCHRONE_SQL_TEMPLATE.formatted(edgeSql.replace("'", "''"));
        try {
            List<Result> results = jdbcTemplate.query(
                    sql,
                    (resultSet, rowNumber) -> new Result(
                            parseExteriorRing(resultSet.getString("geojson")),
                            resultSet.getDouble("area_square_meters")
                    ),
                    center.getX(), center.getY(), center.getX(), center.getY(), travelTimeSeconds
            );
            if (results.isEmpty() || results.getFirst().ring().size() < 4) {
                throw routingUnavailable(null);
            }
            return results.getFirst();
        } catch (CustomException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw routingUnavailable(exception);
        }
    }

    List<List<Double>> parseExteriorRing(String geoJson) {
        try {
            JsonNode coordinates = objectMapper.readTree(geoJson).path("coordinates").path(0);
            if (!coordinates.isArray()) return List.of();
            List<List<Double>> ring = new ArrayList<>();
            for (JsonNode coordinate : coordinates) {
                if (!coordinate.isArray() || coordinate.size() < 2) continue;
                double longitude = coordinate.get(0).asDouble(Double.NaN);
                double latitude = coordinate.get(1).asDouble(Double.NaN);
                if (Double.isFinite(longitude) && Double.isFinite(latitude)) {
                    ring.add(List.of(longitude, latitude));
                }
            }
            return List.copyOf(ring);
        } catch (Exception exception) {
            log.warn("pgRouting GeoJSON 파싱 실패", exception);
            return List.of();
        }
    }

    private CustomException routingUnavailable(Exception cause) {
        if (cause != null) {
            log.warn("pgRouting 생활권 계산 실패. PostGIS/pgRouting 확장과 OSM ways 테이블을 확인하세요.", cause);
        }
        return new CustomException(PropertyErrorCode.ISOCHRONE_ROUTING_UNAVAILABLE);
    }

    public enum Mode {
        WALK,
        DRIVE
    }

    public record Result(List<List<Double>> ring, double areaSquareMeters) {}
}
