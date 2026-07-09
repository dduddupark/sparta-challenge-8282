package com.sparta.spartachallenge8282.order.dto.response;

import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/* 주문 목록 조회 Dto
* - 예시 )
[주문]
교촌치킨
2026.07.08
35,000원
배달완료

[주문]
메가커피
2026.07.05
5,500원
주문취소
 */

//TODO: storeId를 불러올지 storename이 좋을지 고민 중입니다.

public record OrderListResponseDto(
        UUID orderId,
        String orderNumber,
        UUID storeId,
        OrderStatus orderStatus,
        int totalPrice,
        LocalDateTime orderedAt
) {

    public static OrderListResponseDto from(Order order) {
        return new OrderListResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getStoreId(),
                order.getOrderStatus(),
                order.getTotalPrice(),
                order.getCreatedAt()
        );
    }
}