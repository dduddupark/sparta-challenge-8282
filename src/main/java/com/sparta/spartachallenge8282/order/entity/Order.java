package com.sparta.spartachallenge8282.order.entity;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
일부 Payment 개발을 병렬적으로 하기 위해 Order 일부 구현
 */
@Entity
@Getter
@Table(name = "p_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    /** 최종 주문 금액. p_payment.amount 와 동일해야 함. (ERD상 int, 원 단위) */
    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Builder
    private Order(Integer totalPrice) {
        this.totalPrice = totalPrice;
    }
}
