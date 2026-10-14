package com.homes.backend.domain.review.exception;

import com.homes.backend.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements BaseErrorCode {

    ALREADY_REVIEWED("REVIEW400_1", "이미 리뷰를 작성했습니다.", HttpStatus.BAD_REQUEST),
    CANNOT_REVIEW_SELF("REVIEW400_2", "본인에게는 리뷰를 작성할 수 없습니다.", HttpStatus.BAD_REQUEST),
    NOT_MATCHED_WITH_AGENT("REVIEW400_3", "매칭된 적 없는 중개사에게는 리뷰를 남길 수 없습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
