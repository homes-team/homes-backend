package com.homes.backend.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserReportReason {
    HARASSMENT("욕설/괴롭힘이 있었어요"),
    SCAM_ATTEMPT("사기를 시도했어요"),
    SPAM("스팸/도배성 메시지를 보냈어요"),
    INAPPROPRIATE_CONTENT("부적절한 내용을 보냈어요"),
    OTHER("기타 사유");

    private final String description;
}
