package com.homes.backend.global.util;

import java.time.LocalDateTime;

public record ExifData(
        Double latitude,
        Double longitude,
        LocalDateTime originalDate
) {}
