package com.sparta.spartachallenge8282.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
    /**
     * 특정 주문(orderId)의 상태 변경 이력을
     * 변경 시각이 오래된 순서대로 조회한다.(오름차순)
     */
    List<OrderStatusHistory> findAllByOrder_IdOrderByChangedAtAsc(
            UUID orderId
    );

}