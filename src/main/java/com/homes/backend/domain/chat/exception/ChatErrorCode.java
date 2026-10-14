package com.homes.backend.domain.chat.exception;

import com.homes.backend.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

    CHAT_ROOM_NOT_FOUND("CHAT400_1", "채팅방을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    EMPTY_MESSAGE_CONTENT("CHAT400_2", "메시지 내용은 비어있을 수 없습니다.", HttpStatus.BAD_REQUEST),
    CHAT_ROOM_SUSPENDED("CHAT400_3", "관리자에 의해 정지된 채팅방입니다.", HttpStatus.BAD_REQUEST),
    NOT_CHAT_MEMBER("CHAT403_1", "해당 채팅방에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
