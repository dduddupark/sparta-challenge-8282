package com.sparta.spartachallenge8282.order.domain;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
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

    // OrderItemOption 테이블과 연관관계 설정
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemOption> options = new ArrayList<>();

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

    /**
     * 주문 상품에 선택한 옵션을 추가.
     * 옵션 추가 후 주문 상품 총금액을 다시 계산.
     */
    public void addOption(OrderItemOption option) {
        this.options.add(option);
        option.assignOrderItem(this);

        recalculateTotalPrice();
    }
    /**
     * 메뉴 가격과 선택한 옵션 가격을 합산하여
     * 주문 상품 총금액을 다시 계산한다.
     * 계산식:
     * (메뉴 가격 + 옵션 추가 금액 합계) × 수량
     */
    private void recalculateTotalPrice() {
        int totalOptionPrice = options.stream()
                .mapToInt(OrderItemOption::getAdditionalPrice)
                .sum();

        this.totalPrice =
                (this.menuPrice + totalOptionPrice)
                        * this.quantity;
    // 주문 상품에 선택한 옵션을 추가
    public void addOption(OrderItemOption option) {
        this.options.add(option);
        option.assignOrderItem(this);
    }
}