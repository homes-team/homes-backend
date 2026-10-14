package com.homes.backend.domain.chat.controller;

import com.homes.backend.domain.chat.dto.request.ChatMessageSendReqDto;
import com.homes.backend.domain.chat.dto.response.ChatMessageListResDto;
import com.homes.backend.domain.chat.exception.ChatErrorCode;
import com.homes.backend.domain.chat.service.ChatService;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

/**
 * 실시간 메시지 송수신 (STOMP). 방 멤버인지 여부는 ChatChannelInterceptor가 SEND 시점에 먼저 검증한다.
 * 클라이언트는 /app/chats/{chatId}/send 로 보내고, /topic/chats/{chatId}를 구독해서 받는다.
 */
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chats/{chatId}/send")
    public void sendMessage(
            @DestinationVariable Long chatId,
            ChatMessageSendReqDto request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        if (request == null || !StringUtils.hasText(request.content())) {
            throw new CustomException(ChatErrorCode.EMPTY_MESSAGE_CONTENT);
        }

        Long senderId = (Long) headerAccessor.getSessionAttributes().get("userId");

        ChatMessageListResDto response = chatService.sendMessage(chatId, senderId, request.content());

        messagingTemplate.convertAndSend("/topic/chats/" + chatId, response);
    }
}
