package com.sparta.spartachallenge8282.order.entity;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import com.sparta.spartachallenge8282.order.entity.OrderItem;
import com.sparta.spartachallenge8282.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    // 주문 식별 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 주문 취소 시간
    @Column
    private LocalDateTime canceledAt;

    // 영수증에 노출되는 주문 번호
    @Column(nullable = false, unique = true, length = 30)
    private String orderNumber;

    // 주문을 생성한 유저 아이디
    @Column(nullable = false)
    private Long userId;

    // 주문을 받은 가게 아이디
    @Column(nullable = false)
    private UUID storeId;

    // 주문 진행 상황
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus orderStatus;

    // 메뉴 가격의 총합
    @Column(nullable = false)
    private int menuTotalPrice;

    // 할인 금액
    @Column(nullable = false)
    private int discountAmount;

    // 배달비
    @Column(nullable = false)
    private int deliveryFee;

    // 결제할 총 금액
    @Column(nullable = false)
    private int totalPrice;

    // 배달 주소
    @Column(nullable = false, length = 255)
    private String deliveryAddress;

    // 배달 상세 주소
    @Column(length = 255)
    private String deliveryDetailAddress;

    // 주문 요청 사항
    // ex : 운전길 조심히 오셔요! 고기 많이주세요 !
    @Column(length = 255)
    private String requestMessage;

    /*
     * 주문 상품 목록
     * Order가 부모, OrderItem이 자식
     * 주문이 저장될 때 주문상품도 함께 저장되도록 cascade를 사용
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private Order(
            String orderNumber,
            Long userId,
            UUID storeId,
            int menuTotalPrice,
            int discountAmount,
            int deliveryFee,
            String deliveryAddress,
            String deliveryDetailAddress,
            String requestMessage
    ) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.storeId = storeId;
        this.orderStatus = OrderStatus.PENDING;
        this.menuTotalPrice = menuTotalPrice;
        this.discountAmount = discountAmount;
        this.deliveryFee = deliveryFee;
        this.totalPrice = menuTotalPrice - discountAmount + deliveryFee;
        this.deliveryAddress = deliveryAddress;
        this.deliveryDetailAddress = deliveryDetailAddress;
        this.requestMessage = requestMessage;
    }

    public static Order create(
            String orderNumber,
            Long userId,
            UUID storeId,
            int menuTotalPrice,
            int discountAmount,
            int deliveryFee,
            String deliveryAddress,
            String deliveryDetailAddress,
            String requestMessage
    ) {
        return new Order(
                orderNumber,
                userId,
                storeId,
                menuTotalPrice,
                discountAmount,
                deliveryFee,
                deliveryAddress,
                deliveryDetailAddress,
                requestMessage
        );
    }

    /*
     * 주문에 주문상품을 추가한다
     * 양방향 연관관계에서는
     * Order의 리스트에도 넣고,
     * OrderItem의 order 필드도 세팅해야 한다.
     */
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.assignOrder(this);
    }

    // 주문 취소 처리
    // 주문 상태를 CANCELED로 변경하고 취소 시간을 기록
    public void cancel() {
        this.orderStatus = OrderStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }
}