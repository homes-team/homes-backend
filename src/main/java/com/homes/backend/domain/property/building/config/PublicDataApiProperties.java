package com.homes.backend.domain.property.building.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "external-api.public-data")
public class PublicDataApiProperties {
    private String serviceKey = "";
    private String buildingRegisterUrl = "https://apis.data.go.kr/1613000/BldRgstHubService";
    private String apartmentListUrl = "https://apis.data.go.kr/1613000/AptListService4";
    private String apartmentBasisUrl = "https://apis.data.go.kr/1613000/AptBasisInfoServiceV5";
    private int connectTimeoutMillis = 3000;
    private int readTimeoutMillis = 5000;
}
