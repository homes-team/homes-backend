package com.homes.backend.domain.property.controller;

import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "매물 찜(Favorite) API", description = "매물 찜하기 및 취소 토글 API")
public interface PropertyFavoriteControllerDocs {
    @Operation(summary = "찜하기/취소 토글", description = "특정 매물을 찜하거나, 이미 찜한 경우 취소합니다.")
    ApiResponse<Boolean> toggleFavorite(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long propertyId
    );
}
