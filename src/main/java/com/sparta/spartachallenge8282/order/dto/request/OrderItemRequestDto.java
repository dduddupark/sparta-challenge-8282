package com.sparta.spartachallenge8282.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/*
 * 주문 상품 요청 DTO
 * - 주문할 메뉴 ID와 수량
 */
public record OrderItemRequestDto(

        // 주문할 메뉴 ID
        @NotNull(message = "메뉴 ID는 필수입니다.")
        UUID menuId,

        // 주문 수량
        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        Integer quantity,

        // 선택한 옵션 ID 목록
        // 옵션이 없는 메뉴도 가능하므로 빈 리스트 허용
        List<UUID> optionIds
) {
}