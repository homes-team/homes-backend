package com.homes.backend.domain.chat.controller;

import com.homes.backend.domain.chat.dto.request.ChatRoomCreateReqDto;
import com.homes.backend.domain.chat.dto.response.ChatMessageListResDto;
import com.homes.backend.domain.chat.dto.response.ChatRoomResDto;
import com.homes.backend.domain.chat.service.ChatService;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatController implements ChatControllerDocs {

    private final ChatService chatService;

    @Override
    @PreAuthorize("hasRole('USER')") // 공인중개사는 채팅방을 직접 열 수 없음(회원만 가능)
    @PostMapping
    public ApiResponse<ChatRoomResDto> createChatRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid ChatRoomCreateReqDto request
    ) {
        ChatRoomResDto response = chatService.createChatRoom(request.propertyId(), request.realtorId(), userPrincipal.getId());
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @GetMapping
    public ApiResponse<List<ChatRoomResDto>> getMyChatRooms(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ChatRoomResDto> response = chatService.getMyChatRooms(userPrincipal.getId());
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @GetMapping("/{chatId}/messages")
    public ApiResponse<List<ChatMessageListResDto>> getMessages(
            @PathVariable Long chatId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<ChatMessageListResDto> response = chatService.getMessages(chatId, userPrincipal.getId());
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'AGENT')")
    @DeleteMapping("/{chatId}/members/me")
    public ApiResponse<Void> leaveChatRoom(
            @PathVariable Long chatId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        chatService.leaveChatRoom(chatId, userPrincipal.getId());
        return ApiResponse.onSuccess(null);
    }
}
