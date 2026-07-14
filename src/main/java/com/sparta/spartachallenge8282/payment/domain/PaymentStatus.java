package com.sparta.spartachallenge8282.payment.domain;

import com.sparta.spartachallenge8282.global.common.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 상태.
 * <pre>
 * PAID     : 주문 생성 시 결제 성공                       (paid_at)
 * FAILED   : 결제 처리 실패                              (failed_reason)
 * CANCELED : 가게(사장) 사유 취소 — 미수락/거절/수락 후 취소  (canceled_at, canceled_reason)
 * REFUNDED : 고객 요청 취소(환불) — 고객이 5분 내 취소        (refunded_at, refunded_reason)
 * </pre>
 * <p>취소 기준은 <b>주체</b>다: 가게(사장) 사유는 {@code CANCELED}, 고객 요청은 {@code REFUNDED}.
 * {@code @Enumerated(EnumType.STRING)} 으로 저장.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus implements BaseEnum {

    PAID("PAID", "결제 완료"),
    FAILED("FAILED", "결제 실패"),
    CANCELED("CANCELED", "결제 취소"),
    REFUNDED("REFUNDED", "환불 완료");

    private final String code;
    private final String description;
}
