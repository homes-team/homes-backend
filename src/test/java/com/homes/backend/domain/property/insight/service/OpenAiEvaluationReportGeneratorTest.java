package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.insight.config.OpenAiApiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiEvaluationReportGeneratorTest {
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void requestsStructuredOutputAndParsesTheReport() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/responses", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String reportJson = objectMapper.writeValueAsString(Map.of(
                    "summary", "교통과 일조량이 우수합니다.",
                    "strengths", List.of("교통이 편리합니다."),
                    "weaknesses", List.of()
            ));
            byte[] response = objectMapper.writeValueAsBytes(Map.of(
                    "output", List.of(Map.of(
                            "type", "message",
                            "content", List.of(Map.of("type", "output_text", "text", reportJson))
                    ))
            ));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        OpenAiApiProperties properties = new OpenAiApiProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        OpenAiEvaluationReportGenerator generator =
                new OpenAiEvaluationReportGenerator(properties, objectMapper);

        AiEvaluationReportGenerator.GeneratedReport report =
                generator.generate("{\"categories\":[]}").orElseThrow();

        assertThat(authorization.get()).isEqualTo("Bearer test-key");
        JsonNode request = objectMapper.readTree(requestBody.get());
        assertThat(request.path("model").asText()).isEqualTo("gpt-5-nano");
        assertThat(request.path("reasoning").path("effort").asText()).isEqualTo("minimal");
        assertThat(request.path("text").path("format").path("type").asText()).isEqualTo("json_schema");
        assertThat(report.summary()).isEqualTo("교통과 일조량이 우수합니다.");
        assertThat(report.strengths()).containsExactly("교통이 편리합니다.");
        assertThat(report.weaknesses()).isEmpty();
    }
}
