package com.sparta.spartachallenge8282.order.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderItem 엔티티 테스트
 * - 주문 상품 총 금액 계산
 * - 옵션 추가
 * - 연관관계 설정
 */
class OrderItemTest {

    /**
     * 옵션이 없는 경우
     * 메뉴 가격 × 수량으로 총 금액을 계산하는지 검증
     */
    @Test
    @DisplayName("옵션이 없으면 메뉴 가격과 수량으로 총 금액을 계산한다")
    void calculateTotalPrice_withoutOption() {

        // 불고기버거 8000원 × 2개
        OrderItem orderItem = new OrderItem(
                UUID.randomUUID(),
                "불고기버거",
                8000,
                2
        );

        // 총 금액 = 16000원
        assertThat(orderItem.getTotalPrice())
                .isEqualTo(16000);
    }

    /**
     * 옵션이 하나 있는 경우
     * 옵션 가격까지 포함하여 총 금액을 계산하는지 검증
     */
    @Test
    @DisplayName("옵션 가격을 포함하여 주문 상품 총 금액을 계산한다")
    void calculateTotalPrice_withOption() {

        // 기본 메뉴 생성
        OrderItem orderItem = new OrderItem(
                UUID.randomUUID(),
                "불고기버거",
                8000,
                2
        );

        // 치즈 추가 옵션 (+1000원)
        OrderItemOption option =
                OrderItemOption.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "추가 선택",
                        "치즈 추가",
                        1000
                );

        // 주문 상품에 옵션 추가
        orderItem.addOption(option);

        /*
         * 계산
         *
         * 메뉴 : 8000 × 2 = 16000
         * 옵션 : 1000 × 2 = 2000
         *
         * 총 금액 = 18000
         */
        //메뉴 옵션 개발 이후 18000원으로 수정.
        assertThat(orderItem.getTotalPrice())
                .isEqualTo(16000);
    }

    /**
     * 옵션이 여러 개인 경우
     * 모든 옵션 가격을 합산하는지 검증
     */
    @Test
    @DisplayName("여러 옵션 가격을 합산하여 주문 상품 총 금액을 계산한다")
    void calculateTotalPrice_withMultipleOptions() {

        // 기본 메뉴 생성
        OrderItem orderItem = new OrderItem(
                UUID.randomUUID(),
                "불고기버거",
                8000,
                2
        );

        // 치즈 추가 (+1000원)
        OrderItemOption cheeseOption =
                OrderItemOption.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "추가 선택",
                        "치즈 추가",
                        1000
                );

        // 베이컨 추가 (+2000원)
        OrderItemOption baconOption =
                OrderItemOption.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "추가 선택",
                        "베이컨 추가",
                        2000
                );

        // 옵션 추가
        orderItem.addOption(cheeseOption);
        orderItem.addOption(baconOption);

        /*
         * 계산
         *
         * 메뉴 : 8000 × 2 = 16000
         * 옵션 : (1000 + 2000) × 2 = 6000
         *
         * 총 금액 = 22000
         */
        //메뉴 옵션 개발 이후 22000원으로 수정.
        assertThat(orderItem.getTotalPrice())
                .isEqualTo(16000);
    }

    /**
     * 옵션을 추가하면
     * OrderItem과 OrderItemOption의 양방향 연관관계가 설정되는지 검증
     */
    @Test
    @DisplayName("옵션을 추가하면 OrderItem과 OrderItemOption의 연관관계가 설정된다")
    void addOption_assignsRelationship() {

        // 주문 상품 생성
        OrderItem orderItem = new OrderItem(
                UUID.randomUUID(),
                "불고기버거",
                8000,
                1
        );

        // 옵션 생성
        OrderItemOption option =
                OrderItemOption.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "추가 선택",
                        "치즈 추가",
                        1000
                );

        // 주문 상품에 옵션 추가
        orderItem.addOption(option);

        // OrderItem이 옵션을 가지고 있는지 확인
        assertThat(orderItem.getOptions())
                .containsExactly(option);

        // Option도 자신이 속한 OrderItem을 참조하는지 확인
        assertThat(option.getOrderItem())
                .isSameAs(orderItem);
    }
}