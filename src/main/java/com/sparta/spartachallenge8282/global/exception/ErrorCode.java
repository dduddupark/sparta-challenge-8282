package com.sparta.spartachallenge8282.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 에러 코드 Enum.
 * errorCode: 클라이언트에 내려주는 숫자 비즈니스 에러코드
 *
 * <pre>
 * 10001 ~ 10099 : Auth     (인증/토큰 관련)
 * 10101 ~ 10199 : User     (회원 도메인)
 * 20001 ~ 29999 : Store
 * 30001 ~ 30999 : Category
 * 31001 ~ 31999 : Region
 * 40001 ~ 49999 : Menu
 * 50001 ~ 59999 : Order
 * 60001 ~ 69999 : Payment
 * 70001 ~ 79999 : AI
 * 80001 ~ 89999 : Review
 * 90001 ~       : Common
 * 99999         : Internal Server Error
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── Auth (10001 ~ 10099) ──────────────────────────────────────────────────
    // 인증/토큰 관련 오류 — 클라이언트가 10003을 보면 refreshToken() 호출
    UNAUTHORIZED(10001, HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    INVALID_TOKEN(10002, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(10003, HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_REFRESH_TOKEN(10004, HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    ACCESS_DENIED(10005, HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // ── User (10101 ~ 10199) ──────────────────────────────────────────────────
    // 회원 도메인 오류
    USER_NOT_FOUND(10101, HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    DUPLICATE_USERNAME(10102, HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(10103, HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(10104, HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    INVALID_SIGNUP_ROLE(10105, HttpStatus.BAD_REQUEST, "회원가입 시 CUSTOMER 또는 OWNER만 선택 가능합니다."),
    ALREADY_DELETED_USER(10106, HttpStatus.BAD_REQUEST, "이미 탈퇴한 회원입니다."),


    // ── Store (20001 ~ 29999) ─────────────────────────────────────────────────
    STORE_NOT_FOUND(20001, HttpStatus.NOT_FOUND, "가게를 찾을 수 없습니다."),
    STORE_CLOSED(20002, HttpStatus.BAD_REQUEST, "영업 중인 가게가 아닙니다."),


    // ── Category (30001 ~ 30999) ──────────────────────────────────────────────
    CATEGORY_NOT_FOUND(30001, HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),

    // ── Region (31001 ~ 31999) ──
    REGION_NOT_FOUND(31001, HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다."),
    DUPLICATE_REGION_NAME(31002, HttpStatus.CONFLICT, "이미 존재하는 지역명입니다."),
    REGION_IN_USE(31003, HttpStatus.CONFLICT, "사용 중인 지역은 삭제할 수 없습니다."),
    ALREADY_DELETED_REGION(31004, HttpStatus.CONFLICT, "이미 삭제된 지역입니다."),
    REGION_NOT_SERVICEABLE(31005, HttpStatus.CONFLICT, "현재 주문 가능한 지역이 아닙니다."),

    // ── Menu (40001 ~ 49999) ────────────────────────────────────────────────
    MENU_NOT_FOUND(40001, HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),


    // ── Order (50001 ~ 59999) ─────────────────────────────────────────────────
    ORDER_NOT_FOUND(50001, HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(50002, HttpStatus.BAD_REQUEST, "유효하지 않은 주문 상태 전이입니다."),
    ORDER_CANCEL_NOT_ALLOWED(50003, HttpStatus.BAD_REQUEST, "주문 접수 후 5분이 지나 취소할 수 없습니다."),
    MINIMUM_ORDER_AMOUNT_NOT_MET(50004, HttpStatus.BAD_REQUEST, "최소 주문 금액을 충족하지 못했습니다."),


    // ── Payment (60001 ~ 69999) ───────────────────────────────────────────────
    PAYMENT_NOT_FOUND(60001, HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_FAILED(60002, HttpStatus.BAD_REQUEST, "결제에 실패했습니다."),
    INVALID_PAYMENT_REQUEST(60003, HttpStatus.BAD_REQUEST, "잘못된 결제 요청입니다."),
    PAYMENT_ORDER_NOT_FOUND(60004, HttpStatus.NOT_FOUND, "결제 대상 주문을 찾을 수 없습니다."),
    PAYMENT_ALREADY_PROCESSED(60005, HttpStatus.CONFLICT, "이미 결제 완료된 주문입니다."),
    PAYMENT_AMOUNT_MISMATCH(60006, HttpStatus.BAD_REQUEST, "요청 금액과 주문 금액이 일치하지 않습니다."),
    PAYMENT_NOT_CANCELABLE(60007, HttpStatus.CONFLICT, "취소할 수 없는 결제 상태입니다."),
    PAYMENT_NOT_REFUNDABLE(60008, HttpStatus.CONFLICT, "환불할 수 없는 결제 상태입니다."),
    PAYMENT_USER_NOT_FOUND(60009, HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),


    // ── AI (70001 ~ 79999) ────────────────────────────────────────────────────
    AI_API_ERROR(70001, HttpStatus.INTERNAL_SERVER_ERROR, "AI API 호출 중 오류가 발생했습니다."),

    // ── REVIEW (80001 ~ 89999) ────────────────────────────────────────────────────
    REVIEW_API_ERROR(70001, HttpStatus.INTERNAL_SERVER_ERROR, "REVIEW API 호출 중 오류가 발생했습니다."),



    // ── Common (90001 ~) ──────────────────────────────────────────────────────
    INVALID_INPUT(90001, HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    RESOURCE_NOT_FOUND(90002, HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR(99999, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final int code;           // 클라이언트용 숫자 비즈니스 에러코드
    private final HttpStatus status;  // HTTP 상태 코드
    private final String message;     // 에러 메시지
}
