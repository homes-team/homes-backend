package com.homes.backend.domain.bid.exception;

import com.homes.backend.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BidErrorCode implements BaseErrorCode {
    BID_NOT_FOUND("BID404_1", "존재하지 않는 입찰 제안서입니다.", HttpStatus.NOT_FOUND),

    BID_PROPERTY_MISMATCH("BID400_1", "해당 매물에 속한 제안서가 아닙니다.", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED_BID_ACCESS("BID403_1", "해당 제안서에 접근하거나 수정할 권한이 없습니다.", HttpStatus.FORBIDDEN),

    ALREADY_BIDDED("BID400_2", "이미 해당 매물에 입찰서를 제출하셨습니다.", HttpStatus.BAD_REQUEST),

    BID_ALREADY_ACCEPTED("BID400_3", "이미 매칭이 완료된 제안서입니다.", HttpStatus.BAD_REQUEST),

    PROPERTY_ALREADY_MATCHED("BID400_4", "이미 다른 중개사와 거래가 확정된 매물입니다.", HttpStatus.BAD_REQUEST),

    BID_NOT_ACCEPTED("BID400_5", "매칭 확정된 제안서만 취소하거나 완료 처리할 수 있습니다.", HttpStatus.BAD_REQUEST),

    PROPERTY_NOT_MATCHED("BID400_6", "매칭 확정(MATCHED) 상태의 매물에서만 가능한 작업입니다.", HttpStatus.BAD_REQUEST),

    BID_NOT_PENDING("BID400_7", "대기(PENDING) 중인 제안서에서만 가능한 작업입니다. 이미 취소되었거나 거절된 제안서입니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
