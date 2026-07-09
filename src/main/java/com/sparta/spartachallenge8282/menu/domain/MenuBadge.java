package com.sparta.spartachallenge8282.menu.domain;

import com.sparta.spartachallenge8282.global.common.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 메뉴 프로모션 배지.
 *
 * <p>{@code status}(판매 상태)와 독립적으로 동작한다.
 */
@Getter
@RequiredArgsConstructor
public enum MenuBadge implements BaseEnum {

    NONE("NONE", "없음"),
    DISCOUNT("DISCOUNT", "할인중"),
    EVENT("EVENT", "이벤트");

    private final String code;
    private final String description;
}
