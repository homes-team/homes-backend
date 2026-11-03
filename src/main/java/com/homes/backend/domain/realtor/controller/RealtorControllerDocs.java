package com.homes.backend.domain.realtor.controller;

import com.homes.backend.domain.property.dto.response.PresignedUrlResDto;
import com.homes.backend.domain.realtor.dto.request.RealtorSignupReqDto;
import com.homes.backend.domain.realtor.dto.response.RealtorSignupResDto;
import com.homes.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "중개사(Realtor) API", description = "공인중개사 전용 회원가입을 담당하는 API")
public interface RealtorControllerDocs {

    @Operation(summary = "중개사 전용 회원가입", description = "사업자 정보와 사업자등록증/중개사무소 등록증 이미지 URL을 입력받아 중개사 회원가입을 진행합니다. " +
            "이미지는 `GET /realtors/presigned-url`로 미리 S3에 업로드한 뒤 그 결과 URL을 여기에 담아 보냅니다. " +
            "**반드시 사전에 이메일 인증이 완료**되어야 하며, 가입 직후에는 `isVerified=false` 상태로 관리자 승인을 대기합니다.")
    ApiResponse<RealtorSignupResDto> signUpRealtor(@RequestBody @Valid RealtorSignupReqDto request);

    @Operation(summary = "중개사 가입 서류/프로필 이미지 업로드용 Presigned URL 발급", description = "아직 계정이 없는 중개사 가입 지원자가 " +
            "사업자등록증/중개사무소 등록증/프로필 사진을 S3에 직접 업로드할 수 있는 임시 권한 URL을 발급합니다. " +
            "이미지는 `agents/` 폴더에 저장됩니다. 사전에 이메일 인증을 마친 email이어야 발급되며, " +
            "이 발급 주체 정보는 실제 회원가입 제출 시 대조되고 한 번 제출에 성공하면 재사용할 수 없습니다. " +
            "익명 호출이 허용되는 엔드포인트라 IP당 분당 발급 횟수를 제한합니다.")
    ApiResponse<PresignedUrlResDto> getPresignedUrl(
            @Parameter(description = "업로드할 원본 파일명 (확장자 추출용)", example = "business-cert.jpg", required = true)
            @RequestParam String fileName,
            @Parameter(description = "이메일 인증을 마친 이메일", example = "realtor@homes.com", required = true)
            @RequestParam String email,
            @Parameter(hidden = true) HttpServletRequest request
    );
}
