package com.homes.backend.domain.chat.websocket;

import com.homes.backend.domain.chat.entity.ChatRoom;
import com.homes.backend.domain.chat.exception.ChatErrorCode;
import com.homes.backend.domain.chat.repository.ChatRoomRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SUBSCRIBE/SEND 시점에 "이 세션의 유저가 진짜 이 채팅방 멤버인지" 검증한다.
 * 멤버인지 판단하는 로직 자체는 REST(ChatService)에서 쓰던 ChatRoom.isMember()를 그대로 재사용한다.
 * SEND는 반드시 /app/chats/{chatId}/send로만, SUBSCRIBE는 반드시 /topic/chats/{chatId}로만 허용한다 -
 * 이걸 안 가리면 멤버가 /topic/chats/{chatId}로 직접 SEND해서 서버 검증/DB 저장을 건너뛰고
 * sender까지 조작한 가짜 메시지를 상대방에게 바로 브로드캐스트할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class ChatChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SEND_DESTINATION = Pattern.compile("^/app/chats/(\\d+)/send$");
    private static final Pattern SUBSCRIBE_DESTINATION = Pattern.compile("^/topic/chats/(\\d+)$");

    private final ChatRoomRepository chatRoomRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
            Long chatId = matchChatId(command, accessor.getDestination());
            Object userIdAttr = accessor.getSessionAttributes() != null
                    ? accessor.getSessionAttributes().get("userId")
                    : null;

            if (chatId == null || userIdAttr == null) {
                throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
            }

            Long userId = (Long) userIdAttr;
            ChatRoom room = chatRoomRepository.findById(chatId)
                    .orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

            if (!room.isMember(userId)) {
                throw new CustomException(ChatErrorCode.NOT_CHAT_MEMBER);
            }
        }

        return message;
    }

    // SEND는 /app/chats/{chatId}/send, SUBSCRIBE는 /topic/chats/{chatId} 형식과 정확히 일치할 때만 chatId를 인정한다
    private Long matchChatId(StompCommand command, String destination) {
        if (destination == null) {
            return null;
        }
        Pattern pattern = StompCommand.SEND.equals(command) ? SEND_DESTINATION : SUBSCRIBE_DESTINATION;
        Matcher matcher = pattern.matcher(destination);
        return matcher.matches() ? Long.parseLong(matcher.group(1)) : null;
    }
}
