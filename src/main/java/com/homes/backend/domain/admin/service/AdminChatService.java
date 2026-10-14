package com.homes.backend.domain.admin.service;

import com.homes.backend.domain.chat.entity.ChatRoom;
import com.homes.backend.domain.chat.exception.ChatErrorCode;
import com.homes.backend.domain.chat.repository.ChatRoomRepository;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminChatService {

    private final ChatRoomRepository chatRoomRepository;

    /**
     * 문제 채팅방 정지. 대화 내역은 보존하고 새 메시지 전송만 막는다(ChatService.sendMessage에서 검사)
     */
    @Transactional
    public void suspendChatRoom(Long chatId) {
        ChatRoom room = chatRoomRepository.findById(chatId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        room.suspend();
    }
}
