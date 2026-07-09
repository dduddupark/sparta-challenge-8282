package com.sparta.spartachallenge8282.order.dto.response;

import com.sparta.spartachallenge8282.order.entity.OrderItem;

import java.util.UUID;

//OrderItem 엔티티를 API 응답 형태로 변환
public record OrderItemResponseDto(
        // 주문 상품 ID
        UUID orderItemId,
        // 주문한 메뉴 ID
        UUID menuId,
        // 주문 당시 메뉴 이름
        String menuName,
        // 주문 당시 메뉴 가격
        int orderPrice,
        // 주문 수량
        int quantity,
        // 주문 상품 총 금액
        int totalPrice
) {

    public static OrderItemResponseDto from(OrderItem orderItem) {
        return new OrderItemResponseDto(
                orderItem.getId(),
                orderItem.getMenuId(),
                orderItem.getMenuName(),
                orderItem.getMenuPrice(),
                orderItem.getQuantity(),
                orderItem.getTotalPrice()
        );
    }
}