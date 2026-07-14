package com.sparta.spartachallenge8282.order.entity;

import com.sparta.spartachallenge8282.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_order_status_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 상태 변경 대상 주문
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 변경 전 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus previousStatus;

    // 변경 후 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus changedStatus;

    // 상태 변경을 수행한 사용자 ID
    @Column(nullable = false)
    private Long changedBy;

    // 상태 변경을 수행한 사용자 역할
    @Column(nullable = false, length = 30)
    private String changedByRole;

    // 상태 변경 사유
    @Column(length = 255)
    private String changeReason;

    // 상태 변경 시간
    @Column(nullable = false)
    private LocalDateTime changedAt;

    private OrderStatusHistory(
            Order order,
            OrderStatus previousStatus,
            OrderStatus changedStatus,
            Long changedBy,
            String changedByRole,
            String changeReason
    ) {
        this.order = order;
        this.previousStatus = previousStatus;
        this.changedStatus = changedStatus;
        this.changedBy = changedBy;
        this.changedByRole = changedByRole;
        this.changeReason = changeReason;
        this.changedAt = LocalDateTime.now();
    }

    public static OrderStatusHistory create(
            Order order,
            OrderStatus previousStatus,
            OrderStatus changedStatus,
            Long changedBy,
            String changedByRole,
            String changeReason
    ) {
        return new OrderStatusHistory(
                order,
                previousStatus,
                changedStatus,
                changedBy,
                changedByRole,
                changeReason
        );
    }
}