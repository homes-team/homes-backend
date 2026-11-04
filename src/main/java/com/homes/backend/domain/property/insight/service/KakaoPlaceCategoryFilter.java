package com.homes.backend.domain.property.insight.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.StringUtils;

/** Filters keyword-search matches by Kakao's place taxonomy instead of the place name. */
final class KakaoPlaceCategoryFilter {
    private KakaoPlaceCategoryFilter() {
    }

    static boolean isPark(JsonNode document) {
        String categoryName = document.path("category_name").asText(null);
        if (!StringUtils.hasText(categoryName)) {
            return false;
        }
        for (String category : categoryName.split(">")) {
            if (category.strip().contains("공원")) {
                return true;
            }
        }
        return false;
    }
}
