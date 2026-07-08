package com.sparta.spartachallenge8282.order.dto.response;

import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/*
 * 주문 생성 응답 DTO
 * - Order 엔티티를 클라이언트 응답 형태로 변환
 */
public record OrderCreateResponseDto(
        UUID orderId,
        String orderNumber,
        OrderStatus orderStatus,
        int menuTotalPrice,
        int discountAmount,
        int deliveryFee,
        int totalPrice,
        LocalDateTime orderedAt
) {

    public static OrderCreateResponseDto from(Order order) {
        return new OrderCreateResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getMenuTotalPrice(),
                order.getDiscountAmount(),
                order.getDeliveryFee(),
                order.getTotalPrice(),
                order.getCreatedAt()
        );
    }
}