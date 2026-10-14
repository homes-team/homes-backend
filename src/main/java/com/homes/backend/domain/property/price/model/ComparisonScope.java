package com.homes.backend.domain.property.price.model;

public enum ComparisonScope {
    SAME_COMPLEX("동일 단지"),
    SAME_LEGAL_DONG("동일 법정동"),
    SAME_DISTRICT("동일 시군구"),
    UNAVAILABLE("비교 불가");

    private final String description;

    ComparisonScope(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
