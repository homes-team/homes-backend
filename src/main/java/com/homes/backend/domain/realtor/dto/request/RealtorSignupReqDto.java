package com.homes.backend.domain.realtor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "중개사 회원가입 요청 DTO")
public record RealtorSignupReqDto(
        @Schema(description = "이메일 (로그인 아이디)", example = "realtor@homes.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Schema(description = "비밀번호", example = "password1!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 최소 1개씩 포함해야 합니다.")
        String password,

        @Schema(description = "대표자 실명", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @Schema(description = "대표 연락처", example = "010-1234-5678")
        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        String phone,

        @Schema(description = "중개사무소 이름", example = "홈즈공인중개사사무소")
        @NotBlank(message = "중개사무소명은 필수입니다.")
        String officeName,

        @Schema(description = "사업자등록번호", example = "123-45-67890")
        @NotBlank(message = "사업자등록번호는 필수입니다.")
        @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자등록번호 형식이 올바르지 않습니다. (예: 123-45-67890)")
        String businessNum,

        @Schema(description = "중개사무소 주소 (선택, 나중에 등록 가능). 위경도는 이 주소를 기반으로 서버가 자동으로 채운다.", example = "서울 강남구 역삼동 123-45")
        @Size(max = 255, message = "중개사무소 주소는 255자를 초과할 수 없습니다.")
        @Pattern(regexp = ".*\\S.*", message = "중개사무소 주소는 공백만으로 입력할 수 없습니다.")
        String officeAddress,

        @Schema(description = "사업자등록증 이미지 URL (Presigned URL로 미리 업로드 후 전달)", example = "https://homes-duksung-images.s3.ap-northeast-2.amazonaws.com/properties/xxx.jpg")
        @NotBlank(message = "사업자등록증 이미지는 필수입니다.")
        @Pattern(regexp = "^https://homes-duksung-images\\.s3\\.ap-northeast-2\\.amazonaws\\.com/.+$",
                message = "허용된 S3 저장소의 URL만 사용할 수 있습니다.")
        String businessCertUrl,

        @Schema(description = "중개사무소 등록증 이미지 URL (Presigned URL로 미리 업로드 후 전달)", example = "https://homes-duksung-images.s3.ap-northeast-2.amazonaws.com/properties/xxx.jpg")
        @NotBlank(message = "중개사무소 등록증 이미지는 필수입니다.")
        @Pattern(regexp = "^https://homes-duksung-images\\.s3\\.ap-northeast-2\\.amazonaws\\.com/.+$",
                message = "허용된 S3 저장소의 URL만 사용할 수 있습니다.")
        String agentCertUrl,

        @Schema(description = "프로필 사진 URL (선택, Presigned URL로 미리 업로드 후 전달)", example = "https://homes-duksung-images.s3.ap-northeast-2.amazonaws.com/properties/xxx.jpg")
        @Pattern(regexp = "^https://homes-duksung-images\\.s3\\.ap-northeast-2\\.amazonaws\\.com/.+$",
                message = "허용된 S3 저장소의 URL만 사용할 수 있습니다.")
        String profileImageUrl
) {}
