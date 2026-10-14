package com.homes.backend.domain.chat.service;

import com.homes.backend.domain.chat.dto.response.ChatMessageListResDto;
import com.homes.backend.domain.chat.dto.response.ChatRoomResDto;
import com.homes.backend.domain.chat.entity.ChatMessage;
import com.homes.backend.domain.chat.entity.ChatRoom;
import com.homes.backend.domain.chat.exception.ChatErrorCode;
import com.homes.backend.domain.chat.repository.ChatMessageRepository;
import com.homes.backend.domain.chat.repository.ChatRoomRepository;
import com.homes.backend.domain.notification.entity.NotificationType;
import com.homes.backend.domain.notification.service.NotificationService;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.realtor.entity.Agent;
import com.homes.backend.domain.realtor.exception.RealtorErrorCode;
import com.homes.backend.domain.realtor.repository.AgentRepository;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PropertyRepository propertyRepository;
    private final AgentRepository agentRepository;
    private final NotificationService notificationService;

    /**
     * 유저가 특정 매물에 대해 특정 중개사와의 채팅방을 직접 열기 (버튼 클릭 흐름)
     */
    @Transactional
    public ChatRoomResDto createChatRoom(Long propertyId, Long realtorId, Long callerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        if (!property.getUser().getId().equals(callerId)) {
            throw new CustomException(PropertyErrorCode.UNAUTHORIZED_ACCESS);
        }

        Agent agent = agentRepository.findById(realtorId)
                .orElseThrow(() -> new CustomException(RealtorErrorCode.AGENT_NOT_FOUND));

        ChatRoom room = createOrReopenRoom(property, property.getUser(), agent.getUser());
        return ChatRoomResDto.from(room);
    }

    /**
     * (매물, 회원, 중개사) 조합의 채팅방을 생성하거나, 이미 있으면 재오픈한다.
     * 매칭 확정(Bid accept) 시 자동 트리거되는 경로에서도 재사용된다.
     */
    @Transactional
    public ChatRoom createOrReopenRoom(Property property, User user, User agentUser) {
        return chatRoomRepository.findByPropertyIdAndUserIdAndAgentUserId(property.getId(), user.getId(), agentUser.getId())
                .map(room -> {
                    room.reopen();
                    return room;
                })
                .orElseGet(() -> saveNewRoomOrReopenExisting(property, user, agentUser));
    }

    /**
     * 조회 후 저장 사이의 경합(같은 조합의 방 생성 요청이 동시에 들어오는 경우)으로 유니크 제약을 위반하면,
     * 그사이 다른 트랜잭션이 만든 기존 방을 다시 조회해 재오픈한다 (중복 방 생성 방지)
     */
    private ChatRoom saveNewRoomOrReopenExisting(Property property, User user, User agentUser) {
        try {
            return chatRoomRepository.save(
                    ChatRoom.builder()
                            .property(property)
                            .user(user)
                            .agentUser(agentUser)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            ChatRoom room = chatRoomRepository.findByPropertyIdAndUserIdAndAgentUserId(
                            property.getId(), user.getId(), agentUser.getId())
                    .orElseThrow(() -> e);
            room.reopen();
            return room;
        }
    }

    /**
     * 내 채팅방 목록 (회원/중개사 어느 쪽이든 조회 가능)
     */
    public List<ChatRoomResDto> getMyChatRooms(Long userId) {
        return chatRoomRepository.findActiveRoomsByParticipantId(userId).stream()
                .map(ChatRoomResDto::from)
                .toList();
    }

    /**
     * 특정 채팅방의 과거 메시지 내역 조회. 조회하는 시점에 상대방이 보낸 메시지를 읽음 처리한다
     * (내가 보낸 메시지의 읽음 여부는 상대방이 조회할 때 바뀌는 값이라 여기서 건드리지 않음).
     */
    @Transactional
    public List<ChatMessageListResDto> getMessages(Long chatId, Long callerId) {
        ChatRoom room = getRoomAndValidateMembership(chatId, callerId);

        chatMessageRepository.markMessagesAsRead(room.getId(), callerId);

        return chatMessageRepository.findAllByRoomIdOrderByCreatedAtAscIdAsc(room.getId()).stream()
                .map(ChatMessageListResDto::from)
                .toList();
    }

    /**
     * 웹소켓으로 들어온 메시지를 저장. 방 멤버인지는 ChatChannelInterceptor가 SEND 시점에 이미 검증했지만,
     * 서비스 단에서도 한 번 더 확인해 방어적으로 처리한다 (재사용: getRoomAndValidateMembership).
     */
    @Transactional
    public ChatMessageListResDto sendMessage(Long chatId, Long senderId, String content) {
        ChatRoom room = getRoomAndValidateMembership(chatId, senderId);

        if (room.isSuspended()) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_SUSPENDED);
        }

        boolean senderIsUserSide = room.isUserSide(senderId);
        User sender = senderIsUserSide ? room.getUser() : room.getAgentUser();
        User recipient = senderIsUserSide ? room.getAgentUser() : room.getUser();

        ChatMessage message = ChatMessage.builder()
                .content(content)
                .room(room)
                .sender(sender)
                .build();

        chatMessageRepository.save(message);

        notificationService.createNotification(
                recipient.getId(), NotificationType.CHAT, "새로운 채팅 메시지가 도착했습니다.", chatId);

        return ChatMessageListResDto.from(message);
    }

    /**
     * 채팅방 나가기 - 행을 지우지 않고 내 쪽 플래그만 true로 표시 (상대방 이력 보존)
     */
    @Transactional
    public void leaveChatRoom(Long chatId, Long callerId) {
        ChatRoom room = getRoomAndValidateMembership(chatId, callerId);

        if (room.isUserSide(callerId)) {
            chatRoomRepository.markUserLeft(chatId);
        } else {
            chatRoomRepository.markAgentLeft(chatId);
        }
    }

    private ChatRoom getRoomAndValidateMembership(Long chatId, Long callerId) {
        ChatRoom room = chatRoomRepository.findById(chatId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.isMember(callerId)) {
            throw new CustomException(ChatErrorCode.NOT_CHAT_MEMBER);
        }

        return room;
    }
}
