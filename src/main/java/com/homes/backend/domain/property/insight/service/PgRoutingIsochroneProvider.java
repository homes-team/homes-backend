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
    private static final double WALK_BASE_RADIUS_DEGREES = 0.02;
    private static final double DRIVE_RADIUS_DEGREES_PER_TEN_MINUTES = 0.10;
    private static final double MAX_EDGE_RADIUS_DEGREES = 0.60;
    private static final int MAX_RADIUS_EXPANSIONS = 3;
    private static final int WALK_HULL_GRID_SIZE_METERS = 50;
    private static final int DRIVE_HULL_GRID_SIZE_METERS = 200;
    private static final int QUERY_TIMEOUT_SECONDS = 30;
    private static final String WALK_EDGE_SQL_TEMPLATE = """
            SELECT id, source, target,
                   CASE WHEN tag_id IN (101, 102, 104, 105) THEN -1 ELSE length_m / 1.4 END AS cost,
                   CASE WHEN tag_id IN (101, 102, 104, 105) THEN -1 ELSE length_m / 1.4 END AS reverse_cost
            FROM ways
            WHERE geom && ST_Expand(ST_SetSRID(ST_MakePoint(%s, %s), 4326), %s)
            """;
    private static final String DRIVE_EDGE_SQL_TEMPLATE = """
            SELECT id, source, target, cost_s AS cost, reverse_cost_s AS reverse_cost
            FROM ways
            WHERE geom && ST_Expand(ST_SetSRID(ST_MakePoint(%s, %s), 4326), %s)
            """;
    private static final String ISOCHRONE_SQL_TEMPLATE = """
            WITH origin AS (
                SELECT id
                FROM ways_vertices_pgr
                WHERE ST_DWithin(
                    CAST(geom AS geography),
                    CAST(ST_SetSRID(ST_MakePoint(?, ?), 4326) AS geography),
                    1000
                )
                ORDER BY geom <-> ST_SetSRID(ST_MakePoint(?, ?), 4326)
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
                SELECT ST_Collect(DISTINCT ST_SnapToGrid(
                           ST_Transform(vertex.geom, 3857),
                           __HULL_GRID_SIZE_METERS__
                       )) AS geometry,
                       COALESCE(BOOL_OR(
                           ABS(ST_X(vertex.geom) - ?) >= ?
                           OR ABS(ST_Y(vertex.geom) - ?) >= ?
                       ), false) AS boundary_reached
                FROM ways_vertices_pgr vertex
                JOIN reachable ON reachable.node = vertex.id
            ),
            hull AS (
                SELECT ST_ConcaveHull(geometry, 0.30, false) AS geometry,
                       boundary_reached
                FROM point_cloud
                WHERE geometry IS NOT NULL
            ),
            polygon AS (
                SELECT CASE
                    WHEN GeometryType(geometry) = 'POLYGON' THEN geometry
                    ELSE ST_Buffer(geometry, 30)
                END AS geometry,
                boundary_reached
                FROM hull
            )
            SELECT ST_AsGeoJSON(ST_Transform(geometry, 4326)) AS geojson,
                   ST_Area(CAST(ST_Transform(geometry, 4326) AS geography)) AS area_square_meters,
                   boundary_reached
            FROM polygon
            WHERE geometry IS NOT NULL
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public Result calculate(Point center, Mode mode, int travelTimeSeconds) {
        try {
            double radiusDegrees = initialRadiusDegrees(mode, travelTimeSeconds);
            QueryResult calculated = null;
            for (int attempt = 0; attempt < MAX_RADIUS_EXPANSIONS; attempt++) {
                calculated = execute(center, mode, travelTimeSeconds, radiusDegrees);
                if (!calculated.boundaryReached() || radiusDegrees >= MAX_EDGE_RADIUS_DEGREES) {
                    break;
                }
                radiusDegrees = Math.min(radiusDegrees * 2, MAX_EDGE_RADIUS_DEGREES);
            }
            if (calculated == null || calculated.ring().size() < 4) throw routingUnavailable(null);
            return new Result(calculated.ring(), calculated.areaSquareMeters());
        } catch (CustomException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw routingUnavailable(exception);
        }
    }

    private QueryResult execute(Point center, Mode mode, int travelTimeSeconds, double radiusDegrees) {
        String edgeSqlTemplate = mode == Mode.WALK ? WALK_EDGE_SQL_TEMPLATE : DRIVE_EDGE_SQL_TEMPLATE;
        String edgeSql = edgeSqlTemplate.formatted(
                Double.toString(center.getX()),
                Double.toString(center.getY()),
                Double.toString(radiusDegrees)
        );
        int hullGridSizeMeters = mode == Mode.WALK
                ? WALK_HULL_GRID_SIZE_METERS
                : DRIVE_HULL_GRID_SIZE_METERS;
        String sql = ISOCHRONE_SQL_TEMPLATE
                .replace("__HULL_GRID_SIZE_METERS__", Integer.toString(hullGridSizeMeters))
                .formatted(edgeSql.replace("'", "''"));
        double boundaryThreshold = radiusDegrees * 0.9;
        List<QueryResult> results = jdbcTemplate.query(
                sql,
                statement -> {
                    statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    statement.setDouble(1, center.getX());
                    statement.setDouble(2, center.getY());
                    statement.setDouble(3, center.getX());
                    statement.setDouble(4, center.getY());
                    statement.setInt(5, travelTimeSeconds);
                    statement.setDouble(6, center.getX());
                    statement.setDouble(7, boundaryThreshold);
                    statement.setDouble(8, center.getY());
                    statement.setDouble(9, boundaryThreshold);
                },
                (resultSet, rowNumber) -> new QueryResult(
                        parseExteriorRing(resultSet.getString("geojson")),
                        resultSet.getDouble("area_square_meters"),
                        resultSet.getBoolean("boundary_reached")
                )
        );
        if (results.isEmpty()) throw routingUnavailable(null);
        return results.getFirst();
    }

    private double initialRadiusDegrees(Mode mode, int travelTimeSeconds) {
        if (mode == Mode.WALK) {
            return Math.max(WALK_BASE_RADIUS_DEGREES, WALK_BASE_RADIUS_DEGREES * travelTimeSeconds / 600.0);
        }
        return Math.max(
                DRIVE_RADIUS_DEGREES_PER_TEN_MINUTES / 2,
                DRIVE_RADIUS_DEGREES_PER_TEN_MINUTES * travelTimeSeconds / 600.0
        );
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

    private record QueryResult(List<List<Double>> ring, double areaSquareMeters, boolean boundaryReached) {}
}
