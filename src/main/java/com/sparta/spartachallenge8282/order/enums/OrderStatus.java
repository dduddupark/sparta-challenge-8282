package com.sparta.spartachallenge8282.order.enums;

public enum OrderStatus {

    /*
     * 주문 상태
     * PENDING: 주문 요청 상태
     * ACCEPTED: 가게가 주문 수락
     * COOKING: 조리 중
     * DELIVERING: 배달 중
     * CANCELED: 주문 취소
     * COMPLETED: 주문 완료
     */
    PENDING,
    ACCEPTED,
    COOKING,
    DELIVERING,
    CANCELED,
    COMPLETED
}