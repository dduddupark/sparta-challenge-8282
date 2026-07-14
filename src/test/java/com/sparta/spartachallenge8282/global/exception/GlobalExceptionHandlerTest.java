package com.sparta.spartachallenge8282.global.exception;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("AccessDeniedException 발생 시 403 ACCESS_DENIED 예외 응답을 반환한다")
    void handleAccessDeniedException() {
        // given
        AccessDeniedException exception = new AccessDeniedException("권한이 없습니다.");

        // when
        ResponseEntity<ApiResponse<?>> response = globalExceptionHandler.handleAccessDeniedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        
        ApiResponse<?> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED.getCode());
        assertThat(body.getMessage()).isEqualTo(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("CustomException 발생 시 해당 ErrorCode에 맞는 예외 응답을 반환한다")
    void handleCustomException() {
        // given
        CustomException exception = new CustomException(ErrorCode.USER_NOT_FOUND);

        // when
        ResponseEntity<ApiResponse<?>> response = globalExceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.getStatus());
        
        ApiResponse<?> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.getCode());
        assertThat(body.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("처리되지 않은 Exception 발생 시 500 INTERNAL_ERROR 예외 응답을 반환한다")
    void handleException() {
        // given
        Exception exception = new RuntimeException("예상치 못한 서버 오류");

        // when
        ResponseEntity<ApiResponse<?>> response = globalExceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        
        ApiResponse<?> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        assertThat(body.getMessage()).isEqualTo(ErrorCode.INTERNAL_ERROR.getMessage());
    }
}
