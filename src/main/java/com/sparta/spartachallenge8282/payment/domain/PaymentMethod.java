package com.sparta.spartachallenge8282.payment.domain;

import com.sparta.spartachallenge8282.global.common.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 수단.
 * <p>ERD 기준 현재 {@code CARD} 만 존재. {@code @Enumerated(EnumType.STRING)} 으로 저장.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod implements BaseEnum {

    CARD("CARD", "카드");

    private final String code;
    private final String description;
}
