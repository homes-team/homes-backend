package com.homes.backend.domain.user.controller;

import com.homes.backend.domain.property.dto.response.PropertyListRespDto;
import com.homes.backend.domain.property.dto.response.ReportListRespDto;
import com.homes.backend.domain.user.dto.request.*;
import com.homes.backend.domain.user.dto.response.IdentityVerificationResDto;
import com.homes.backend.domain.user.dto.response.UserDetailResDto;
import com.homes.backend.domain.user.dto.response.UserProfileResDto;
import com.homes.backend.domain.user.dto.response.UserSignupResDto;
import com.homes.backend.domain.user.dto.response.UserUpdateProfileResDto;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.TokenDto;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "유저 & 인증(User) API", description = "회원가입, 로그인, 로그아웃 및 이메일 인증을 담당하는 API")
public interface UserControllerDocs {

    @Operation(summary = "이메일 중복 확인", description = "단순히 특정 이메일이 이미 DB에 가입되어 있는지 여부만 확인합니다.")
    @PostMapping("/check-email")
    ApiResponse<Void> checkEmail(@RequestBody EmailCheckReqDto request);

    @Operation(summary = "일반 회원가입", description = "새로운 유저의 회원가입을 진행합니다. **반드시 사전에 이메일 인증이 완료**되어야 승인됩니다.")
    @PostMapping("/signup")
    ApiResponse<UserSignupResDto> signUp(@RequestBody UserCreateReqDto request);

    @Operation(summary = "로그인 API", description = "이메일과 비밀번호로 로그인을 진행하고 토큰 세트를 발급합니다.")
    @PostMapping("/login")
    ApiResponse<TokenDto> login(
            @RequestBody @Valid UserLoginReqDto request
    );

    @Operation(summary = "비밀번호 변경", description = "로그인한 유저의 기존 비밀번호를 검증한 후, 새로운 비밀번호로 안전하게 암호화하여 수정합니다.")
    @PatchMapping("/me/password")
    ApiResponse<Void> updatePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid UserUpdatePasswordReqDto request
    );

    @Operation(summary = "로그아웃", description = "현재 사용 중인 Access Token을 가로채 수명만큼 Redis 블랙리스트에 등록하여 다음 요청을 차단합니다.")
    @PostMapping("/logout")
    ApiResponse<Void> logout(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Bearer 가 포함된 Access Token을 실어주세요.", required = true)
            @RequestHeader("Authorization") String accessToken
    );

    @Operation(summary = "이메일 인증번호 발송 요청", description = "회원가입 전 이메일 중복을 검사하고, 가입 가능한 이메일이라면 6자리 인증번호를 진짜 메일함으로 쏩니다.")
    @PostMapping("/emails/verification-requests")
    ApiResponse<Void> sendVerificationCode(@RequestBody @Valid EmailCheckReqDto request);

    @Operation(summary = "이메일 인증번호 검증", description = "사용자가 메일로 받은 6자리 인증번호를 대조하여 검증합니다. 성공 시 Redis에 가입 증표를 남깁니다.")
    @PostMapping("/emails/verifications")
    ApiResponse<Void> verifyCode(@RequestBody @Valid EmailVerificationReqDto request);

    @Operation(summary = "내가 내놓은 집 확인", description = "현재 로그인한 유저가 등록한 매물 리스트를 조회합니다.")
    ApiResponse<List<PropertyListRespDto>> getMyProperties(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "토큰 재발급 API", description = "만료된 Access Token을 Refresh Token을 사용해 갱신합니다.")
    @PostMapping("/refresh")
    ApiResponse<TokenDto> refresh(
            @Parameter(description = "Bearer 가 포함된 Refresh Token을 실어주세요.", required = true)
            @RequestHeader("RefreshToken") String refreshToken
    );

    @Operation(summary = "내가 찜한 매물 확인", description = "현재 로그인한 유저가 찜한 매물 리스트를 조회합니다.")
    @GetMapping("/me/favorites")
    ApiResponse<List<PropertyListRespDto>> getMyFavoriteProperties(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "구글 소셜 로그인 및 자동 회원가입", description = "프론트엔드로부터 Google Authorization Code를 전달받아, 실시간으로 구글 서버에서 지메일을 파싱한 뒤 소셜 가입/로그인을 진행하고 자체 JWT 토큰 세트를 반환합니다.")
    ApiResponse<TokenDto> googleLogin(OAuthLoginReqDto reqDto);

    @Operation(summary = "내가 신고한 매물 내역", description = "현재 로그인한 유저가 신고한 매물 내역 리스트를 조회합니다.")
    @GetMapping("me/reports")
    ApiResponse<List<ReportListRespDto>> getMyReports(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "실명 인증 요청", description = "프론트엔드에서 포트원(PortOne) SDK로 본인인증(PASS/문자)을 완료한 뒤 발급받은 identityVerificationId를 전달받아, " +
            "포트원 서버에 검증을 요청하고 성공 시 로그인한 본인의 실명/전화번호를 반영합니다.")
    @PostMapping("/me/verification")
    ApiResponse<IdentityVerificationResDto> verifyIdentity(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid IdentityVerificationReqDto request
    );

    @Operation(summary = "실명 인증 강제 동기화 (관리자 전용)", description = "포트원은 본인인증 완료를 알려주는 웹훅을 제공하지 않기 때문에, " +
            "\"인증은 분명 완료했는데 마이페이지에 반영이 안 된다\"는 문의가 들어왔을 때 관리자가 identityVerificationId로 포트원 서버를 재조회해 " +
            "해당 유저의 인증 상태를 강제로 맞춰주는 예외 처리용 API입니다. role이 ADMIN인 유저만 호출할 수 있습니다.")
    @PostMapping("/me/verification/callback")
    ApiResponse<IdentityVerificationResDto> forceSyncIdentityVerification(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid AdminIdentityVerificationSyncReqDto request
    );

    @Operation(summary = "마이페이지 회원정보 조회", description = "로그인한 유저 본인의 회원정보(이메일, 실명, 닉네임, 전화번호, 실명 인증 여부, 이용 목적 등)를 조회합니다.")
    @GetMapping("/me")
    ApiResponse<UserProfileResDto> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "회원정보 수정", description = "마이페이지에서 닉네임과 이용 목적을 수정합니다. " +
            "값을 보내지 않은 필드는 기존 값이 유지됩니다(부분 수정). " +
            "실명/전화번호는 실명 인증을 통해서만 변경할 수 있으므로 이 API로는 수정할 수 없습니다.")
    @PatchMapping("/me")
    ApiResponse<UserUpdateProfileResDto> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid UserUpdateProfileReqDto request
    );

    @Operation(summary = "내가 최근 본 방 조회", description = "현재 로그인한 유저가 최근에 조회한 매물 리스트를 최신순으로 조회합니다.")
    @GetMapping("/me/recent-views")
    ApiResponse<List<PropertyListRespDto>> getMyRecentViews(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "회원 탈퇴", description = "로그인한 유저의 회원 정보를 DB에서 완전히 삭제합니다. " +
            "본인이 찜한 기록/신고 내역/중개사 프로필은 함께 삭제되지만, 등록한 매물이 남아있으면 먼저 매물을 삭제해야 탈퇴할 수 있습니다. " +
            "탈퇴에 사용된 Access Token은 즉시 블랙리스트에 등록되어 더 이상 사용할 수 없습니다.")
    @DeleteMapping("/me")
    ApiResponse<Void> deleteAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Bearer 가 포함된 Access Token을 실어주세요.", required = true)
            @RequestHeader("Authorization") String accessToken
    );

    @Operation(summary = "유저 상세보기 (공인중개사 전용)", description = "공인중개사가 매물 등록자의 평판(실명인증 여부, 리뷰 평점 등)을 확인합니다. " +
            "이메일/전화번호 등 개인정보는 노출하지 않습니다.")
    @GetMapping("/{userId}")
    ApiResponse<UserDetailResDto> getUserDetail(@PathVariable Long userId);

}