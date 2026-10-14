package com.homes.backend.domain.admin.controller;

import com.homes.backend.domain.admin.service.AdminChatService;
import com.homes.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/chats")
@RequiredArgsConstructor
public class AdminChatController implements AdminChatControllerDocs {

    private final AdminChatService adminChatService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{chatId}")
    public ApiResponse<Void> suspendChatRoom(@PathVariable Long chatId) {
        adminChatService.suspendChatRoom(chatId);
        return ApiResponse.onSuccess();
    }
}
