package com.sparta.spartachallenge8282.order.dto.request;

import com.sparta.spartachallenge8282.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderStatusUpdateRequestDto(

        // 변경할 주문 상태
        @NotNull(message = "변경할 주문 상태는 필수입니다.")
        OrderStatus orderStatus,

        // 상태 변경 사유
        @Size(max = 255, message = "상태 변경 사유는 255자 이하로 입력해주세요.")
        String reason
) {
}