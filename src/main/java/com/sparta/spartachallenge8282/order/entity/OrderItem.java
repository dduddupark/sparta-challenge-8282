package com.sparta.spartachallenge8282.order.entity;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    // 주문된 상품 고유 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 해당 상품이 속한 주문 식별
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 주문한 메뉴 ID
    // 아직 Menu 엔티티 연동 전이므로 UUID만 저장
    @Column(nullable = false)
    private UUID menuId;

    // 주문 당시 메뉴 이름
    @Column(nullable = false, length = 100)
    private String menuName;

    // 주문 당시 메뉴 가격
    @Column(nullable = false)
    private int menuPrice;

    // 메뉴 주문 수량
    @Column(nullable = false)
    private int quantity;

    // 해당 주문 상품 총 금액
    @Column(nullable = false)
    private int totalPrice;

    public OrderItem(
            UUID menuId,
            String menuName,
            int menuPrice,
            int quantity
    ) {
        this.menuId = menuId;
        this.menuName = menuName;
        this.menuPrice = menuPrice;
        this.quantity = quantity;
        this.totalPrice = menuPrice * quantity;
    }

    /*
    * Order와 연관관계 연결
    * Order.addOrderItem()안에서만 호출하는 방식이 좋다고 함.
    * 이유 :
     */
    protected void assignOrder(Order order) {
        this.order = order;
    }
}