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
import java.util.Optional;

@Slf4j
@Component
public class BuildingRegisterClient extends PublicDataClientSupport {
    public BuildingRegisterClient(PublicDataApiProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    public Optional<BuildingRegisterTitle> findTitle(ResolvedAddress address) {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBuildingRegisterUrl() + "/getBrTitleInfo")
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
        try {
            return items(restTemplate.getForObject(uri, String.class)).stream()
                    .filter(item -> "주건축물".equals(text(item, "mainAtchGbCdNm")) || text(item, "mainAtchGbCdNm") == null)
                    .findFirst()
                    .map(this::mapTitle);
        } catch (Exception exception) {
            log.warn("건축물대장 표제부 조회 실패: legalDongCode={}", address.legalDongCode(), exception);
            throw new CustomException(PropertyErrorCode.BUILDING_PROVIDER_UNAVAILABLE);
        }
    }

    private BuildingRegisterTitle mapTitle(JsonNode item) {
        return new BuildingRegisterTitle(
                text(item, "mgmBldrgstPk"), text(item, "bldNm"), text(item, "platPlc"), text(item, "newPlatPlc"),
                parseDate(text(item, "useAprDay")), integer(item, "hhldCnt"), integer(item, "fmlyCnt"),
                decimal(item, "heit"), integer(item, "grndFlrCnt"), integer(item, "ugrndFlrCnt"),
                sum(integer(item, "rideUseElvtCnt"), integer(item, "emgenUseElvtCnt")),
                sum(integer(item, "indrAutoUtcnt"), integer(item, "indrMechUtcnt"), integer(item, "oudrAutoUtcnt"), integer(item, "oudrMechUtcnt"))
        );
    }

    static LocalDate parseDate(String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

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
}
