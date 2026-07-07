package com.sparta.spartachallenge8282.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 에러 코드 Enum.
 * HTTP 상태 코드와 에러 메시지를 한 곳에서 중앙 관리한다.
 * 새로운 도메인 에러는 해당 섹션에 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── Common ────────────────────────────────────────────────────────────────
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // ── Auth / Security ────────────────────────────────────────────────────────
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // ── User ──────────────────────────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),

    // ── Region ────────────────────────────────────────────────────────────────
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다."),

    // ── Category ──────────────────────────────────────────────────────────────
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),

    // ── Store ────────────────────────────────────────────────────────────────
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "가게를 찾을 수 없습니다."),
    STORE_CLOSED(HttpStatus.BAD_REQUEST, "영업 중인 가게가 아닙니다."),

    // ── Product ───────────────────────────────────────────────────────────────
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_UNAVAILABLE(HttpStatus.BAD_REQUEST, "주문할 수 없는 상품입니다."),

    // ── Order ────────────────────────────────────────────────────────────────
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 주문 상태 전이입니다."),
    ORDER_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "주문 접수 후 5분이 지나 취소할 수 없습니다."),
    MINIMUM_ORDER_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "최소 주문 금액을 충족하지 못했습니다."),

    // ── Payment ───────────────────────────────────────────────────────────────
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "결제에 실패했습니다."),

    // ── AI ───────────────────────────────────────────────────────────────────
    AI_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI API 호출 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
