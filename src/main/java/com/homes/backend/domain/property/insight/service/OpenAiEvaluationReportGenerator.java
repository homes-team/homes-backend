package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.insight.config.OpenAiApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class OpenAiEvaluationReportGenerator implements AiEvaluationReportGenerator {
    static final String MODEL = "gpt-5-nano";
    static final String MODEL_VERSION = "OPENAI_GPT_5_NANO_EVIDENCE_V2";

    private static final String SYSTEM_PROMPT = """
            당신은 부동산 매물의 생활 여건을 설명하는 분석가입니다.
            제공된 점수, calculation, evidence만 사용하고 확인되지 않은 시설, 거리, 가격 또는 사실을 만들어내지 마세요.
            점수 자체를 변경하지 말고 소비자가 이해하기 쉬운 한국어로 요약하세요.
            강점과 약점에는 반드시 해당 항목의 실제 거리, 시설 수, 방향, 층수 또는 연도 중 하나를 근거로 포함하세요.
            데이터 수집률이나 수집 진행 상황보다 왜 해당 점수가 산정되었는지를 우선 설명하세요.
            강점과 약점은 각각 최대 3개이며, evidence가 없으면 해당 항목을 언급하지 마세요.
            """;

    private final OpenAiApiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiEvaluationReportGenerator(OpenAiApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(properties.getReadTimeoutMillis());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public String modelVersion() {
        return MODEL_VERSION;
    }

    @Override
    public Optional<GeneratedReport> generate(String evaluationInputJson) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            String responseBody = restClient.post()
                    .uri("/v1/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .body(requestBody(evaluationInputJson))
                    .retrieve()
                    .body(String.class);

            return parseResponse(responseBody);
        } catch (Exception exception) {
            log.warn("OpenAI AI 매물 리포트 생성에 실패하여 규칙 기반 리포트를 사용합니다: {}",
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private Map<String, Object> requestBody(String evaluationInputJson) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "summary", Map.of("type", "string"),
                "strengths", Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 3),
                "weaknesses", Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 3)
        ));
        schema.put("required", List.of("summary", "strengths", "weaknesses"));
        schema.put("additionalProperties", false);

        return Map.of(
                "model", MODEL,
                "reasoning", Map.of("effort", "minimal"),
                "input", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", "다음 JSON 평가 입력으로 리포트를 생성하세요.\n" + evaluationInputJson)
                ),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "property_evaluation_report",
                        "strict", true,
                        "schema", schema
                )),
                "max_output_tokens", 500
        );
    }

    private Optional<GeneratedReport> parseResponse(String responseBody) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            return Optional.empty();
        }

        JsonNode root = objectMapper.readTree(responseBody);
        String outputText = extractOutputText(root);
        if (outputText == null) {
            return Optional.empty();
        }

        JsonNode reportNode = objectMapper.readTree(outputText);
        String summary = reportNode.path("summary").asText("").trim();
        if (summary.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new GeneratedReport(
                summary,
                stringList(reportNode.path("strengths")),
                stringList(reportNode.path("weaknesses"))
        ));
    }

    private String extractOutputText(JsonNode root) {
        for (JsonNode output : root.path("output")) {
            if (!"message".equals(output.path("type").asText())) {
                continue;
            }
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    return content.path("text").asText(null);
                }
            }
        }
        return null;
    }

    private List<String> stringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText("").trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
            if (values.size() == 3) {
                break;
            }
        }
        return List.copyOf(values);
    }
}
