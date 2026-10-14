package com.homes.backend.domain.realtor.exception;

import com.homes.backend.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RealtorErrorCode implements BaseErrorCode {

    DUPLICATE_BUSINESS_NUM("REALTOR400_1", "이미 등록된 사업자등록번호입니다.", HttpStatus.BAD_REQUEST),
    AGENT_NOT_FOUND("REALTOR400_2", "중개사 프로필을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    OFFICE_LOCATION_NOT_SET("REALTOR400_3", "중개사무소 위치 정보가 등록되지 않았습니다.", HttpStatus.BAD_REQUEST),
    UNAPPROVED_AGENT("REALTOR403_1", "관리자 승인이 완료되지 않은 중개사입니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
