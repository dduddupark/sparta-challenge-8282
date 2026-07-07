package com.sparta.spartachallenge8282.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 모든 API 응답의 공통 래퍼.
 * <pre>
 * 성공: { "success": true,  "message": "조회 성공", "data": {...}, "timestamp": "..." }
 * 실패: { "success": false, "errorCode": 10001, "message": "로그인이 필요합니다.", "timestamp": "..." }
 * </pre>
 * errorCode, data 필드는 null이면 JSON 응답에 포함되지 않는다.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final Integer errorCode;   // 비즈니스 에러 식별 코드 (성공 시 null → JSON 미포함)
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, Integer errorCode, String message, T data) {
        this.success   = success;
        this.errorCode = errorCode;
        this.message   = message;
        this.data      = data;
        this.timestamp = LocalDateTime.now();
    }

    /** 데이터 포함 성공 응답 */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, null, message, data);
    }

    /** 데이터 없는 성공 응답 */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, null, message, null);
    }

    /** 에러 코드 포함 실패 응답 */
    public static <T> ApiResponse<T> error(int errorCode, String message) {
        return new ApiResponse<>(false, errorCode, message, null);
    }
}
