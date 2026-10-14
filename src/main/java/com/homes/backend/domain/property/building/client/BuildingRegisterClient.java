package com.homes.backend.domain.property.building.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.building.config.PublicDataApiProperties;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.geocoding.ResolvedAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class BuildingRegisterClient extends PublicDataClientSupport {
    /**
     * Creates a client for the building-register title endpoints.
     */
    public BuildingRegisterClient(PublicDataApiProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    /**
     * Retrieves the main-building title record for a resolved address.
     */
    public Optional<BuildingRegisterTitle> findTitle(ResolvedAddress address) {
        URI uri = requestUri("getBrTitleInfo", address);
        try {
            List<JsonNode> responseItems = items(restTemplate.getForObject(uri, String.class));
            return selectMainBuilding(responseItems).map(this::mapTitle);
        } catch (Exception exception) {
            log.warn("건축물대장 표제부 조회 실패: legalDongCode={}", address.legalDongCode(), exception);
            throw new CustomException(PropertyErrorCode.BUILDING_PROVIDER_UNAVAILABLE);
        }
    }

    /**
     * Retrieves optional complex-level totals from the recap-title record.
     */
    public Optional<BuildingRegisterRecap> findRecap(ResolvedAddress address) {
        URI uri = requestUri("getBrRecapTitleInfo", address);
        try {
            return items(restTemplate.getForObject(uri, String.class)).stream()
                    .findFirst()
                    .map(this::mapRecap);
        } catch (Exception exception) {
            log.warn("건축물대장 총괄표제부 조회 실패로 단지정보 보강을 생략합니다: legalDongCode={}",
                    address.legalDongCode(), exception);
            return Optional.empty();
        }
    }

    /**
     * Builds a building-register API URI from a requested operation and lot address.
     */
    private URI requestUri(String operation, ResolvedAddress address) {
        return UriComponentsBuilder.fromHttpUrl(properties.getBuildingRegisterUrl() + "/" + operation)
                .queryParam("serviceKey", serviceKey())
                .queryParam("sigunguCd", address.sigunguCode())
                .queryParam("bjdongCd", address.bjdongCode())
                .queryParam("platGbCd", address.landTypeCode())
                .queryParam("bun", address.mainLotNumber())
                .queryParam("ji", address.subLotNumber())
                .queryParam("numOfRows", 100)
                .queryParam("pageNo", 1)
                .queryParam("_type", "json")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
    }

    /**
     * Selects the main-building record, falling back to the first returned item.
     */
    private Optional<JsonNode> selectMainBuilding(List<JsonNode> responseItems) {
        return responseItems.stream()
                .filter(this::isMainBuilding)
                .findFirst()
                .or(() -> responseItems.stream().findFirst());
    }

    /**
     * Identifies the official main-building codes returned by the provider.
     */
    private boolean isMainBuilding(JsonNode item) {
        return "0".equals(text(item, "mainAtchGbCd"))
                || "주건축물".equals(text(item, "mainAtchGbCdNm"));
    }

    /**
     * Maps a title record to the building-level enrichment fields.
     */
    private BuildingRegisterTitle mapTitle(JsonNode item) {
        return new BuildingRegisterTitle(
                text(item, "mgmBldrgstPk"), text(item, "bldNm"), text(item, "platPlc"), text(item, "newPlatPlc"),
                parseDate(text(item, "useAprDay")), positiveInteger(item, "hhldCnt"), positiveInteger(item, "fmlyCnt"),
                decimal(item, "heit"), integer(item, "grndFlrCnt"), integer(item, "ugrndFlrCnt"),
                sum(integer(item, "rideUseElvtCnt"), integer(item, "emgenUseElvtCnt")),
                sum(integer(item, "indrAutoUtcnt"), integer(item, "indrMechUtcnt"), integer(item, "oudrAutoUtcnt"), integer(item, "oudrMechUtcnt"))
        );
    }

    /**
     * Maps a recap-title record to complex-level enrichment fields.
     */
    private BuildingRegisterRecap mapRecap(JsonNode item) {
        return new BuildingRegisterRecap(
                text(item, "mgmBldrgstPk"), parseDate(text(item, "useAprDay")),
                positiveInteger(item, "hhldCnt"), positiveInteger(item, "fmlyCnt"),
                positiveInteger(item, "mainBldCnt"),
                positiveSum(integer(item, "indrAutoUtcnt"), integer(item, "indrMechUtcnt"),
                        integer(item, "oudrAutoUtcnt"), integer(item, "oudrMechUtcnt"))
        );
    }

    /**
     * Parses a compact building-register approval date.
     */
    static LocalDate parseDate(String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    /**
     * Sums available values while preserving an entirely missing total as {@code null}.
     */
    private static Integer sum(Integer... values) {
        int sum = 0;
        boolean present = false;
        for (Integer value : values) {
            if (value != null) {
                sum += value;
                present = true;
            }
        }
        return present ? sum : null;
    }

    /**
     * Returns a sum only when it represents a positive provider value.
     */
    private static Integer positiveSum(Integer... values) {
        Integer value = sum(values);
        return value != null && value > 0 ? value : null;
    }
}
