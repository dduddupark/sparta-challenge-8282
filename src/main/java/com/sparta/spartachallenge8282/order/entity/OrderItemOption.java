package com.sparta.spartachallenge8282.order.entity;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_order_item_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 어떤 주문 상품에 선택된 옵션인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    // 원본 메뉴 옵션 ID
    @Column(nullable = false)
    private UUID menuOptionId;

    // 원본 옵션 그룹 ID
    @Column(nullable = false)
    private UUID optionGroupId;

    // 주문 당시 옵션 그룹명 스냅샷
    @Column(nullable = false, length = 100)
    private String optionGroupName;

    // 주문 당시 옵션명 스냅샷
    @Column(nullable = false, length = 100)
    private String optionName;

    // 주문 당시 옵션 추가 금액
    @Column(nullable = false)
    private int additionalPrice;

    private OrderItemOption(
            UUID menuOptionId,
            UUID optionGroupId,
            String optionGroupName,
            String optionName,
            int additionalPrice
    ) {
        this.menuOptionId = menuOptionId;
        this.optionGroupId = optionGroupId;
        this.optionGroupName = optionGroupName;
        this.optionName = optionName;
        this.additionalPrice = additionalPrice;
    }

    public static OrderItemOption create(
            UUID menuOptionId,
            UUID optionGroupId,
            String optionGroupName,
            String optionName,
            int additionalPrice
    ) {
        return new OrderItemOption(
                menuOptionId,
                optionGroupId,
                optionGroupName,
                optionName,
                additionalPrice
        );
    }

    protected void assignOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }
}