package com.homes.backend.domain.property.building.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.building.config.PublicDataApiProperties;
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
public class ApartmentBasisClient extends PublicDataClientSupport {
    /**
     * Creates a client for the K-apt complex basic-information endpoint.
     */
    public ApartmentBasisClient(PublicDataApiProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    /**
     * Retrieves the basic information for the supplied K-apt complex code.
     */
    public Optional<ApartmentBasicInformation> findBasicInformation(String kaptCode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getApartmentBasisUrl() + "/getAphusBassInfoV5")
                .queryParam("serviceKey", serviceKey())
                .queryParam("kaptCode", kaptCode)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
        try {
            return items(restTemplate.getForObject(uri, String.class)).stream().findFirst().map(this::map);
        } catch (Exception exception) {
            log.warn("공동주택 기본정보 조회 실패: kaptCode={}", kaptCode, exception);
            return Optional.empty();
        }
    }

    /**
     * Maps a K-apt response item to the fields used for building enrichment.
     */
    private ApartmentBasicInformation map(JsonNode item) {
        return new ApartmentBasicInformation(
                text(item, "kaptCode"), text(item, "kaptName"), text(item, "doroJuso", "kaptAddr"),
                parseDate(text(item, "kaptUsedate")), integer(item, "kaptdaCnt", "hoCnt"),
                integer(item, "kaptDongCnt"), integer(item, "kaptTopFloor"), integer(item, "kaptdPcnt"),
                text(item, "codeHallNm"), text(item, "codeHeatNm")
        );
    }

    /**
     * Parses K-apt approval dates in compact or separator-delimited form.
     */
    static LocalDate parseDate(String value) {
        if (value == null) return null;
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() != 8) return null;
        try {
            return LocalDate.parse(digits, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
