package com.homes.backend.domain.property.insight.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "external-api.openai")
public class OpenAiApiProperties {
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com";
    private int connectTimeoutMillis = 3000;
    private int readTimeoutMillis = 15000;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
