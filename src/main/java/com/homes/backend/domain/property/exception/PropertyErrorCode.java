package com.homes.backend.domain.property.exception;

import com.homes.backend.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PropertyErrorCode implements BaseErrorCode {
    PROPERTY_NOT_FOUND("PROP404_1", "존재하지 않는 매물입니다.",HttpStatus.NOT_FOUND),
    UNAUTHORIZED_ACCESS("PROP403_1", "해당 매물을 수정/삭제 권한이 없습니다.", HttpStatus.FORBIDDEN),

    CANNOT_FAVORITE_OWN_PROPERTY("PROP403_2", "본인의 매물은 찜할 수 없습니다.", HttpStatus.FORBIDDEN),
    CANNOT_REPORT_OWN_PROPERTY("PROP400_2", "본인의 매물은 신고할 수 없습니다.", HttpStatus.BAD_REQUEST),

    ALREADY_REPORTED("PROP400_1", "이미 신고한 매물입니다.", HttpStatus.BAD_REQUEST),
    BUILDING_ADDRESS_RESOLUTION_FAILED("BUILDING422_1", "매물 주소를 건축물 조회 정보로 변환할 수 없습니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    BUILDING_INFORMATION_NOT_FOUND("BUILDING422_2", "해당 주소의 건물정보를 찾을 수 없습니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    BUILDING_PROVIDER_UNAVAILABLE("BUILDING502_1", "공공 건물정보 제공기관에 일시적으로 연결할 수 없습니다.", HttpStatus.BAD_GATEWAY),
    INVALID_ISOCHRONE_PARAMETER("ISOCHRONE400", "이동수단은 walk 또는 drive, 이동시간은 5, 10, 15, 20, 30분 중 하나여야 합니다.", HttpStatus.BAD_REQUEST),
    ISOCHRONE_COORDINATE_UNAVAILABLE("ISOCHRONE422", "해당 매물 위치에서 이동 가능 영역을 계산할 수 없습니다.", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
