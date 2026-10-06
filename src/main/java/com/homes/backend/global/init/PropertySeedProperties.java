package com.homes.backend.global.init;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.seed.properties")
public class PropertySeedProperties {
    private boolean enabled = false;
    private String ownerEmail = "";
    private String imageBaseUrl = "http://localhost:8080/seed-images";
}
