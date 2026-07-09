package com.sparta.spartachallenge8282.order.dto.response;

import com.sparta.spartachallenge8282.order.entity.OrderItemOption;

import java.util.UUID;

public record OrderItemOptionResponseDto(
        // 주문 상품에 선택된 옵션의 고유 ID
        UUID orderItemOptionId,

        // 원본 메뉴 옵션 ID
        // 주문 당시 어떤 옵션을 선택했는지 추적하기 위한 값
        UUID menuOptionId,

        // 원본 옵션 그룹 ID
        // 선택한 옵션이 어떤 그룹에 속하는지 식별
        // 예) 음료 선택, 사이드 선택
        UUID optionGroupId,

        // 주문 당시 옵션 그룹명
        // 메뉴 정보 변경과 관계없이 주문 당시 정보를 유지
        String optionGroupName,

        // 주문 당시 선택한 옵션명
        // 예) 콜라, 치즈 추가
        String optionName,

        // 주문 당시 옵션 추가 금액
        // 이후 메뉴 가격이 변경되어도 주문 금액을 보존하기 위해 저장
        int additionalPrice
) {

    public static OrderItemOptionResponseDto from(OrderItemOption option) {
        return new OrderItemOptionResponseDto(
                option.getId(),
                option.getMenuOptionId(),
                option.getOptionGroupId(),
                option.getOptionGroupName(),
                option.getOptionName(),
                option.getAdditionalPrice()
        );
    }
}