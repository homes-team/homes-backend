package com.homes.backend.global.exception;

import com.homes.backend.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 (우리가 만든 CustomException)
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.warn("CustomException: {}", e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.onFailure(e.getErrorCode()));
    }

    /**
     * @PreAuthorize 등 메서드 단위 권한 체크 실패.
     * 컨트롤러 핸들러 호출 중 발생하는 예외라 Spring MVC가 SecurityConfig의 accessDeniedHandler보다 먼저 처리하므로 여기서 잡아야 함.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        return ResponseEntity.status(GlobalErrorCode.FORBIDDEN.getHttpStatus())
                .body(ApiResponse.onFailure(GlobalErrorCode.FORBIDDEN));
    }

    /**
     * @Valid 검증 실패 - 필드 에러 메시지 모아서 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("ValidationException: {}", errorMessage);
        return ResponseEntity.status(GlobalErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.onFailure(GlobalErrorCode.INVALID_INPUT.getCode(), errorMessage));
    }

    /**
     * @Validated 파라미터 검증 실패
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("ConstraintViolationException: {}", errorMessage);
        return ResponseEntity.status(GlobalErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.onFailure(GlobalErrorCode.INVALID_INPUT.getCode(), errorMessage));
    }

    /**
     * 파라미터 타입 변환 실패 (ex. Enum 바인딩 실패)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
        return ResponseEntity.status(GlobalErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.onFailure(GlobalErrorCode.INVALID_INPUT));
    }

    /**
     * 필수 멀티파트 파일(@RequestPart)이 누락된 경우
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<String>> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        String errorMessage = "필수 파라미터 '" + e.getRequestPartName() + "'가 누락되었습니다.";
        log.warn("MissingServletRequestPartException: {}", errorMessage);
        return ResponseEntity.status(GlobalErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.onFailure(GlobalErrorCode.INVALID_INPUT.getCode(), errorMessage));
    }

    /**
     * 요청 바디를 읽을 수 없는 경우 (JSON 파싱 실패, 인코딩 손상 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity.status(GlobalErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.onFailure(GlobalErrorCode.INVALID_INPUT));
    }

    /**
     * 존재하지 않는 경로 (404)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("NoResourceFoundException: {}", e.getMessage());
        return ResponseEntity.status(GlobalErrorCode.NOT_FOUND.getHttpStatus())
                .body(ApiResponse.onFailure(GlobalErrorCode.NOT_FOUND));
    }

    /**
     * 허용되지 않는 HTTP 메서드 (405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("MethodNotSupportedException: {}", e.getMessage());
        return ResponseEntity.status(GlobalErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(ApiResponse.onFailure(GlobalErrorCode.METHOD_NOT_ALLOWED));
    }

    /**
     * 핸들링되지 않은 서버 내부 예외 (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("UnhandledException: {}", e.getMessage(), e);
        return ResponseEntity.status(GlobalErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.onFailure(GlobalErrorCode.INTERNAL_SERVER_ERROR));
    }
}