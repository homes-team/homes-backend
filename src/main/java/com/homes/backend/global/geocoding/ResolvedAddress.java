package com.homes.backend.global.geocoding;

/**
 * 카카오 주소 검색 결과 중 좌표 및 건축물대장 조회에 필요한 지번 식별정보입니다.
 */
public record ResolvedAddress(
        String normalizedAddress,
        double latitude,
        double longitude,
        String legalDongCode,
        String sigunguCode,
        String bjdongCode,
        String landTypeCode,
        String mainLotNumber,
        String subLotNumber,
        String buildingName
) {
}
