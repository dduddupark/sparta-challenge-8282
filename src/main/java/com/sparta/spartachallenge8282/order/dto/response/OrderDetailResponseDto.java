package com.sparta.spartachallenge8282.order.dto.response;

import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponseDto(
        // 주문 ID
        UUID orderId,

        // 주문 번호
        // ex)A260708269
        String orderNumber,

        // 주문한 고객 ID
        Long userId,

        // 주문 받는 가게 ID
        UUID storeId,

        // 주문 상태
        OrderStatus orderStatus,

        // 메뉴 금액 합계
        int menuTotalPrice,

        // 할인 금액
        int discountAmount,

        // 배달비
        int deliveryFee,

        // 최종 결제 금액
        int totalPrice,

        // 요청 사항
        String requestMessage,

        // 배송 주소
        String deliveryAddress,

        // 배송 상세 주소
        String deliveryDetailAddress,

        // 주문 시간
        LocalDateTime orderedAt,

        // 취소 시간
        LocalDateTime canceledAt,

        // 주문 상품 목록
        List<OrderItemResponseDto> orderItems
) {

    public static OrderDetailResponseDto from(Order order) {
        return new OrderDetailResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getStoreId(),
                order.getOrderStatus(),
                order.getMenuTotalPrice(),
                order.getDiscountAmount(),
                order.getDeliveryFee(),
                order.getTotalPrice(),
                order.getRequestMessage(),
                order.getDeliveryAddress(),
                order.getDeliveryDetailAddress(),

                // 현재는 별도 orderedAt 필드가 없으므로 createdAt을 주문 시간으로 사용
                order.getCreatedAt(),

                // 주문 취소 시간
                order.getCanceledAt(),

                order.getOrderItems()
                        .stream()
                        .map(OrderItemResponseDto::from)
                        .toList()
        );
    }
}