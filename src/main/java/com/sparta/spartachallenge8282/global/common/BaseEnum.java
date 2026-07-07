package com.sparta.spartachallenge8282.global.common;

/**
 * 도메인 Enum(UserRole, OrderStatus 등)이 공통으로 구현하는 인터페이스.
 * code: DB 저장값 또는 외부 표현값, description: 사람이 읽을 수 있는 설명.
 */
public interface BaseEnum {
    String getCode();
    String getDescription();
}
