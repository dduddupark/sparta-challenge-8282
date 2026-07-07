package com.sparta.spartachallenge8282.global.exception;

import lombok.Getter;

/**
 * 애플리케이션 전역 커스텀 예외.
 * ErrorCode를 통해 HTTP 상태와 메시지를 함께 전달한다.
 *
 * <pre>
 * 사용 예:
 *   throw new CustomException(ErrorCode.USER_NOT_FOUND);
 *   throw new CustomException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
 * </pre>
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
