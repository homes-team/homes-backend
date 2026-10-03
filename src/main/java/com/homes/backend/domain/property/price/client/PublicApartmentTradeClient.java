package com.homes.backend.domain.property.price.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.homes.backend.domain.property.building.config.PublicDataApiProperties;
import com.homes.backend.domain.property.price.model.ApartmentTrade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PublicApartmentTradeClient implements ApartmentTradeProvider {
    private static final int PAGE_SIZE = 1000;
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final PublicDataApiProperties properties;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;
    private final RestTemplate restTemplate;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public PublicApartmentTradeClient(PublicDataApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.xmlMapper = new XmlMapper();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMillis());
        factory.setReadTimeout(properties.getReadTimeoutMillis());
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public List<ApartmentTrade> findMonthlyTrades(String sigunguCode, YearMonth contractMonth) {
        if (!StringUtils.hasText(properties.getServiceKey())) {
            throw new ApartmentTradeProviderException("공공데이터 서비스 키가 설정되지 않았습니다.");
        }
        String cacheKey = sigunguCode + ":" + contractMonth;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.trades();
        }

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(properties.getApartmentTradeUrl())
                    .queryParam("serviceKey", serviceKey())
                    .queryParam("LAWD_CD", sigunguCode)
                    .queryParam("DEAL_YMD", contractMonth.format(DateTimeFormatter.ofPattern("yyyyMM")))
                    .queryParam("numOfRows", PAGE_SIZE)
                    .queryParam("pageNo", 1)
                    .encode(StandardCharsets.UTF_8)
                    .build().toUri();
            String body = restTemplate.getForObject(uri, String.class);
            List<ApartmentTrade> trades = parse(body);
            cache.put(cacheKey, new CacheEntry(List.copyOf(trades), System.currentTimeMillis()));
            return trades;
        } catch (ApartmentTradeProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("아파트 실거래가 조회 실패: sigunguCode={}, contractMonth={}", sigunguCode, contractMonth, exception);
            throw new ApartmentTradeProviderException("실거래가 제공기관 조회에 실패했습니다.", exception);
        }
    }

    List<ApartmentTrade> parse(String responseBody) throws Exception {
        if (!StringUtils.hasText(responseBody)) return List.of();
        JsonNode root = responseBody.stripLeading().startsWith("<")
                ? xmlMapper.readTree(responseBody)
                : objectMapper.readTree(responseBody);
        JsonNode response = root.has("response") ? root.path("response") : root;
        JsonNode header = response.path("header");
        String resultCode = header.path("resultCode").asText();
        if (!resultCode.isBlank() && !"00".equals(resultCode) && !"000".equals(resultCode)) {
            throw new ApartmentTradeProviderException("공공데이터 응답 오류: " + resultCode);
        }
        JsonNode item = response.path("body").path("items").path("item");
        if (item.isMissingNode() || item.isNull()) return List.of();

        List<JsonNode> nodes = new ArrayList<>();
        if (item.isArray()) item.forEach(nodes::add);
        else if (item.isObject()) nodes.add(item);
        return nodes.stream().map(this::map).filter(java.util.Objects::nonNull).toList();
    }

    private ApartmentTrade map(JsonNode node) {
        try {
            if (StringUtils.hasText(text(node, "cdealDay", "해제사유발생일"))
                    || "O".equalsIgnoreCase(text(node, "cdealType", "해제여부"))) {
                return null;
            }
            long price = Long.parseLong(text(node, "dealAmount", "거래금액").replace(",", "").trim());
            double area = Double.parseDouble(text(node, "excluUseAr", "전용면적"));
            int year = Integer.parseInt(text(node, "dealYear", "년"));
            int month = Integer.parseInt(text(node, "dealMonth", "월"));
            int day = Integer.parseInt(text(node, "dealDay", "일"));
            return new ApartmentTrade(
                    text(node, "aptNm", "아파트"), text(node, "umdNm", "법정동"), text(node, "jibun", "지번"),
                    area, integer(node, 0, "floor", "층"), nullableInteger(node, "buildYear", "건축년도"),
                    price, LocalDate.of(year, month, day));
        } catch (NullPointerException | NumberFormatException | DateTimeException exception) {
            log.debug("유효하지 않은 실거래가 항목을 제외합니다: {}", node);
            return null;
        }
    }

    private static String text(JsonNode node, String... names) {
        for (String name : names) {
            String value = node.path(name).asText();
            if (!value.isBlank()) return value;
        }
        return null;
    }

    private static int integer(JsonNode node, int fallback, String... names) {
        Integer value = nullableInteger(node, names);
        return value == null ? fallback : value;
    }

    private static Integer nullableInteger(JsonNode node, String... names) {
        String value = text(node, names);
        if (value == null) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String serviceKey() {
        String key = properties.getServiceKey();
        return key.contains("%") ? URLDecoder.decode(key, StandardCharsets.UTF_8) : key;
    }

    private record CacheEntry(List<ApartmentTrade> trades, long storedAtMillis) {
        boolean isExpired() {
            return System.currentTimeMillis() - storedAtMillis > CACHE_TTL.toMillis();
        }
    }
}
