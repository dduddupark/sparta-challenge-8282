package com.sparta.spartachallenge8282.menu.domain;

import com.sparta.spartachallenge8282.global.common.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 메뉴 판매 상태.
 *
 * <p>{@code badge}(프로모션 표시)와는 독립적으로 동작한다 — 품절 상태에서도 배지는 표시될 수 있다.
 */
@Getter
@RequiredArgsConstructor
public enum MenuStatus implements BaseEnum {

    ON_SALE("ON_SALE", "판매중"),
    SOLD_OUT("SOLD_OUT", "품절");

    private final String code;
    private final String description;
}
