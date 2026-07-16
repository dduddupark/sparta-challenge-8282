package com.sparta.spartachallenge8282.order.presentation.dto.response;

import com.sparta.spartachallenge8282.order.domain.Order;
import com.sparta.spartachallenge8282.order.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderStatusUpdateResponseDto(
        UUID orderId,
        String orderNumber,
        OrderStatus orderStatus,
        LocalDateTime updatedAt
) {

    public static OrderStatusUpdateResponseDto from(Order order) {
        return new OrderStatusUpdateResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getUpdatedAt()
        );
    }
}