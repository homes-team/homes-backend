package com.homes.backend.domain.admin.controller;

import com.homes.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "관리자(Admin) API", description = "관리자 전용 API")
public interface AdminChatControllerDocs {

    @Operation(summary = "문제 채팅방 정지", description = "신고/문의가 들어온 채팅방을 정지시킵니다. 기존 대화 내역은 보존되며, " +
            "정지된 방에는 더 이상 새 메시지를 보낼 수 없습니다.")
    @DeleteMapping("/{chatId}")
    ApiResponse<Void> suspendChatRoom(
            @Parameter(description = "정지시킬 채팅방 ID") @PathVariable Long chatId
    );
}
