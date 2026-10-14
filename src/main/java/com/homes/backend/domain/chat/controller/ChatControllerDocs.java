package com.homes.backend.domain.chat.controller;

import com.homes.backend.domain.chat.dto.request.ChatRoomCreateReqDto;
import com.homes.backend.domain.chat.dto.response.ChatMessageListResDto;
import com.homes.backend.domain.chat.dto.response.ChatRoomResDto;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "채팅(Chat) API", description = "회원-중개사 간 1:1 채팅방 및 메시지 내역을 담당하는 API")
public interface ChatControllerDocs {

    @Operation(summary = "채팅방 생성", description = "회원이 특정 매물에 대해 특정 중개사와의 1:1 채팅방을 생성/재오픈합니다. " +
            "이미 같은 (매물, 회원, 중개사) 조합의 방이 있으면 새로 만들지 않고 기존 방을 재활성화합니다. 중개사(role=AGENT)는 호출할 수 없습니다.")
    @PostMapping
    ApiResponse<ChatRoomResDto> createChatRoom(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid ChatRoomCreateReqDto request
    );

    @Operation(summary = "내 채팅방 목록 조회", description = "로그인한 유저(회원/중개사)가 참여 중인 채팅방 목록을 조회합니다.")
    @GetMapping
    ApiResponse<List<ChatRoomResDto>> getMyChatRooms(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "채팅방 메시지 내역 조회", description = "특정 채팅방의 과거 메시지 내역을 오래된 순으로 조회합니다. 방 참여자만 조회할 수 있습니다.")
    @GetMapping("/{chatId}/messages")
    ApiResponse<List<ChatMessageListResDto>> getMessages(
            @PathVariable Long chatId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "채팅방 나가기", description = "채팅방에서 나(me)를 삭제합니다. 방 자체는 삭제되지 않고 내 쪽 참여 여부만 '나감'으로 표시되어 상대방의 메시지 이력은 보존됩니다.")
    @DeleteMapping("/{chatId}/members/me")
    ApiResponse<Void> leaveChatRoom(
            @PathVariable Long chatId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );
}
