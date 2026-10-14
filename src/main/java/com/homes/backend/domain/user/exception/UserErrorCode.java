package com.homes.backend.domain.user.exception;

import com.homes.backend.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    DUPLICATE_EMAIL("USER400_1", "이미 사용 중인 이메일입니다.", HttpStatus.BAD_REQUEST),
    DUPLICATE_NICKNAME("USER400_2", "이미 사용 중인 닉네임입니다.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("USER400_3", "유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD("USER400_4", "비밀번호가 틀렸습니다.", HttpStatus.BAD_REQUEST),
    VERIFICATION_CODE_EXPIRED("USER400_5", "인증번호가 만료되었거나 없습니다.", HttpStatus.BAD_REQUEST),
    WRONG_VERIFICATION_CODE("USER400_6", "인증번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED("USER400_7","이메일 인증이 완료되지 않은 사용자입니다.", HttpStatus.BAD_REQUEST),
    INVALID_REFRESH_TOKEN("USER400_8", "유효하지 않거나 만료된 Refresh Token입니다.", HttpStatus.BAD_REQUEST),
    INVALID_GOOGLE_CODE("USER400_9", "유효하지 않거나 구글 통신 장애가 있습니다.",HttpStatus.BAD_REQUEST ),
    ALREADY_IDENTITY_VERIFIED("USER400_10", "이미 실명 인증이 완료된 사용자입니다.", HttpStatus.BAD_REQUEST),
    IDENTITY_VERIFICATION_NOT_COMPLETED("USER400_11", "본인인증이 완료되지 않았거나 유효하지 않은 인증 정보입니다.", HttpStatus.BAD_REQUEST),
    PORTONE_COMMUNICATION_ERROR("USER400_12", "포트원 서버와 통신 중 오류가 발생했습니다.", HttpStatus.BAD_REQUEST),
    CANNOT_REPORT_SELF("USER400_14", "본인을 신고할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ALREADY_REPORTED_USER("USER400_15", "이미 신고한 유저입니다.", HttpStatus.BAD_REQUEST),
    CANNOT_REPORT_UNCONTACTED_USER("USER400_16", "채팅한 적 없는 유저는 신고할 수 없습니다.", HttpStatus.BAD_REQUEST);
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}