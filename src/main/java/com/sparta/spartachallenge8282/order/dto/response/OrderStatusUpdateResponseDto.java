package com.sparta.spartachallenge8282.order.dto.response;

import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.enums.OrderStatus;

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