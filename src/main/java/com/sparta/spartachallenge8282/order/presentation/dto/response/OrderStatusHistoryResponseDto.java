package com.sparta.spartachallenge8282.order.presentation.dto.response;

import com.sparta.spartachallenge8282.order.domain.OrderStatus;
import com.sparta.spartachallenge8282.order.domain.OrderStatusHistory;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 상태 변경 이력 응답 DTO
 */
public record OrderStatusHistoryResponseDto(
        UUID historyId,
        OrderStatus previousStatus,
        OrderStatus changedStatus,
        Long changedBy,
        String changedByRole,
        String changeReason,
        LocalDateTime changedAt
) {

    /**
     * OrderStatusHistory 엔티티를 응답 DTO로 변환한다.
     */
    public static OrderStatusHistoryResponseDto from(
            OrderStatusHistory history
    ) {
        return new OrderStatusHistoryResponseDto(
                history.getId(),
                history.getPreviousStatus(),
                history.getChangedStatus(),
                history.getChangedBy(),
                history.getChangedByRole(),
                history.getChangeReason(),
                history.getChangedAt()
        );
    }
}