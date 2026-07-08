package com.sparta.spartachallenge8282.global.exception;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 전역 예외 핸들러.
 * 모든 컨트롤러에서 발생하는 예외를 ApiResponse 형태로 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 커스텀 비즈니스 예외 처리.
     * ErrorCode에 정의된 HTTP 상태와 메시지를 그대로 응답한다.
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[CustomException] {} - {}", errorCode.name(), errorCode.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    /**
     * @Valid / @Validated 유효성 검사 실패 처리.
     * 필드별 오류 메시지를 "필드: 메시지" 형식으로 합쳐서 응답한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String field = ((FieldError) error).getField();
                    String message = error.getDefaultMessage() != null
                            ? error.getDefaultMessage()
                            : "유효하지 않은 값입니다.";
                    return field + ": " + message;
                })
                .collect(Collectors.joining(", "));

        log.warn("[ValidationException] {}", errorMessage);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), errorMessage));
    }

    /**
     * Spring Security 인가 실패(Access Denied) 처리.
     * @PreAuthorize 등급 권한 위반 시 10005(ACCESS_DENIED) 공통 에러를 응답한다.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        log.warn("[AccessDeniedException] 접근 권한 거부 - {}", e.getMessage());
        return ResponseEntity
                .status(org.springframework.http.HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.ACCESS_DENIED.getCode(), ErrorCode.ACCESS_DENIED.getMessage()));
    }

    /**
     * 처리되지 않은 모든 예외 처리.
     * 서버 내부 오류로 간주하고 상세 메시지는 노출하지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("[UnhandledException] {}", e.getMessage(), e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
