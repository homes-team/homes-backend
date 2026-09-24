package com.homes.backend.global.init;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.entity.Station;
import com.homes.backend.domain.property.repository.StationRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StationDataInitializer implements ApplicationRunner {
    private static final Set<String> SUPPORTED_POI_TYPES = Set.of("지하철역", "버스정류장");
    private static final int BATCH_SIZE = 1_000;

    private final StationRepository stationRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Set<String> missingPoiTypes = SUPPORTED_POI_TYPES.stream()
                .filter(type -> !stationRepository.existsByPoiType(type))
                .collect(Collectors.toSet());
        if (missingPoiTypes.isEmpty()) {
            ensureIndexes();
            log.info("✅ 전국 교통 POI 데이터가 이미 존재합니다. 초기화를 스킵합니다.");
            return;
        }

        log.info("전국 교통 POI 데이터 초기화를 시작합니다. 대상={}", missingPoiTypes);

        try {
            // 파일이 반드시 src/main/resources 폴더 바로 아래에 있어야 합니다!
            InputStream inputStream = new ClassPathResource("transportation_processed.geojson").getInputStream();
            JsonNode rootNode = objectMapper.readTree(inputStream);
            JsonNode features = rootNode.path("features");

            List<Station> stationsToSave = new ArrayList<>();
            int savedCount = 0;

            for (JsonNode feature : features) {
                JsonNode properties = feature.path("properties");
                String poiType = properties.path("poi_type").asText();

                if (missingPoiTypes.contains(poiType)) {
                    double lat = properties.path("lat").asDouble();
                    double lon = properties.path("lon").asDouble();
                    Point point = geometryFactory.createPoint(new Coordinate(lon, lat));

                    // '역역' -> '역' 으로 정제
                    String rawPoiName = properties.path("poi_name").asText();
                    String cleanPoiName = (rawPoiName != null && rawPoiName.endsWith("역역"))
                            ? rawPoiName.replace("역역", "역")
                            : rawPoiName;

                    Station station = Station.builder()
                            .poiId(properties.path("poi_id").asText())
                            .poiName(cleanPoiName)
                            .poiType(poiType)
                            .coordinate(point)
                            .build();

                    stationsToSave.add(station);
                    if (stationsToSave.size() == BATCH_SIZE) {
                        savedCount += stationsToSave.size();
                        saveBatch(stationsToSave);
                    }
                }
            }

            savedCount += stationsToSave.size();
            saveBatch(stationsToSave);
            ensureIndexes();
            log.info("전국 교통 POI 데이터 {}개 초기화 완료", savedCount);

        } catch (Exception e) {
            log.error("❌ 전국 교통 POI 데이터 초기화 중 에러 발생 (파일 경로 확인 필요)", e);
            throw new IllegalStateException("전국 교통 POI 데이터 초기화에 실패했습니다.", e);
        }
    }

    private void saveBatch(List<Station> stations) {
        if (stations.isEmpty()) return;
        stationRepository.saveAll(stations);
        stationRepository.flush();
        entityManager.clear();
        stations.clear();
    }

    private void ensureIndexes() {
        entityManager.createNativeQuery(
                "CREATE INDEX IF NOT EXISTS idx_station_coordinate_gist ON station USING GIST (coordinate)")
                .executeUpdate();
        entityManager.createNativeQuery(
                "CREATE INDEX IF NOT EXISTS idx_station_poi_type ON station (poi_type)")
                .executeUpdate();
    }
}
