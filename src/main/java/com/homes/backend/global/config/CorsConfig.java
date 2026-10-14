package com.homes.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 API 주소에 대해
                .allowedOriginPatterns("*") // 모든 프론트엔드 주소 접근 허용 (나중에 배포 시 프론트 도메인만 허용하도록 수정)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // 1시간 동안 브라우저에 정책 캐시
    }
}