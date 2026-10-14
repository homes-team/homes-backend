package com.homes.backend.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.homes.backend.global.exception.model.BaseErrorCode;

public record ApiResponse<T>(
        @JsonProperty("isSuccess") boolean isSuccess,
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) T result
) {
    private static final String SUCCESS_CODE = "COMMON_200";
    private static final String SUCCESS_MESSAGE = "요청에 성공하였습니다.";

    /**
     * 데이터가 있는 성공 응답
     */
    public static <T> ApiResponse<T> onSuccess(T result) {
        return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, result);
    }

    /**
     * 데이터가 없는 성공 응답
     */
    public static ApiResponse<Void> onSuccess() {
        return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /**
     * 에러 발생 시 응답 (데이터 없음)
     */
    public static <T> ApiResponse<T> onFailure(BaseErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    // 에러 발생 시 응답 (하드코딩용 - GlobalExceptionHandler에서 사용)
    public static <T> ApiResponse<T> onFailure(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}