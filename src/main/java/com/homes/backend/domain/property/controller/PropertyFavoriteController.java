package com.homes.backend.domain.property.controller;

import com.homes.backend.domain.property.service.PropertyFavoriteService;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/favorites")
public class PropertyFavoriteController implements PropertyFavoriteControllerDocs {
    private final PropertyFavoriteService favoriteService;

    @Override
    @PostMapping("/{propertyId}")
    public ApiResponse<Boolean> toggleFavorite(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long propertyId
    ) {
        if (userPrincipal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        boolean isFavorited = favoriteService.toggleFavorite(userPrincipal.getId(), propertyId);
        return ApiResponse.onSuccess(isFavorited);
    }
}
