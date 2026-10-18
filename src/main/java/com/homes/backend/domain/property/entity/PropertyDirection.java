package com.homes.backend.domain.property.entity;

/**
 * 매물의 주실(거실 또는 주된 채광면) 방향.
 */
public enum PropertyDirection {
    SOUTH("남향"),
    SOUTHEAST("남동향"),
    SOUTHWEST("남서향"),
    EAST("동향"),
    WEST("서향"),
    NORTHEAST("북동향"),
    NORTHWEST("북서향"),
    NORTH("북향"),
    UNKNOWN("미입력");

    private final String displayName;

    PropertyDirection(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
