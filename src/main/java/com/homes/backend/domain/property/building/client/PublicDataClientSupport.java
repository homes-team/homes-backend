package com.homes.backend.domain.property.building.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.building.config.PublicDataApiProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

abstract class PublicDataClientSupport {
    protected final PublicDataApiProperties properties;
    protected final ObjectMapper objectMapper;
    protected final RestTemplate restTemplate;

    /**
     * Configures shared JSON parsing and timeout-aware HTTP access for public-data clients.
     */
    protected PublicDataClientSupport(PublicDataApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMillis());
        factory.setReadTimeout(properties.getReadTimeoutMillis());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Returns the provider service key in the form required for URI construction.
     */
    protected String serviceKey() {
        String key = properties.getServiceKey();
        return key != null && key.contains("%")
                ? URLDecoder.decode(key, StandardCharsets.UTF_8)
                : key;
    }

    /**
     * Extracts successful public-data response items regardless of the API's item shape.
     */
    protected List<JsonNode> items(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode header = root.path("response").path("header");
        String resultCode = header.path("resultCode").asText();
        if (!resultCode.isBlank() && !"00".equals(resultCode) && !"000".equals(resultCode)) {
            throw new IllegalStateException("공공데이터 응답 오류: " + resultCode);
        }
        JsonNode body = root.path("response").path("body");
        JsonNode items = body.path("items");
        JsonNode item = items.isArray() ? items : items.path("item");
        if (item.isMissingNode() || item.isNull()) {
            item = body.path("item");
        }
        if (item.isArray()) {
            return objectMapper.convertValue(item, objectMapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
        }
        return item.isObject() ? List.of(item) : List.of();
    }

    /**
     * Returns the first nonblank value among the given JSON field names.
     */
    protected static String text(JsonNode node, String... names) {
        for (String name : names) {
            String value = node.path(name).asText();
            if (!value.isBlank()) return value;
        }
        return null;
    }

    /**
     * Parses the first available named JSON field as an integer.
     */
    protected static Integer integer(JsonNode node, String... names) {
        String value = text(node, names);
        if (value == null) return null;
        try {
            return Integer.valueOf(value.replace(",", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Parses a strictly positive integer field, treating zero as unavailable data.
     */
    protected static Integer positiveInteger(JsonNode node, String... names) {
        Integer value = integer(node, names);
        return value != null && value > 0 ? value : null;
    }

    /**
     * Parses the first available named JSON field as a decimal number.
     */
    protected static Double decimal(JsonNode node, String... names) {
        String value = text(node, names);
        if (value == null) return null;
        try {
            return Double.valueOf(value.replace(",", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
