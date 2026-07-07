package com.sparta.spartachallenge8282.payment.entity;

import com.sparta.spartachallenge8282.global.common.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 상태.
 * <pre>
 * PAID     : 주문 생성 시 결제 성공        (paid_at)
 * FAILED   : 결제 처리 실패               (failed_reason)
 * CANCELED : 사장님 미수락 등으로 취소     (canceled_at, canceled_reason)
 * REFUNDED : 수락 후 취소 등으로 환불      (refunded_at, refunded_reason)
 * </pre>
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
