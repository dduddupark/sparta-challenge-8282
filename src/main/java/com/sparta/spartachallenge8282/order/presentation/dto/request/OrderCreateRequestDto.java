package com.sparta.spartachallenge8282.order.presentation.dto.request;

import com.sparta.spartachallenge8282.payment.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/*
 * 주문 생성 요청 DTO
 * - 클라이언트가 보낸 주문 생성 JSON 데이터
 * - Controller에서 Service로 전달
 */
public record OrderCreateRequestDto(

        // 주문할 가게 ID
        @NotNull(message = "가게 ID는 필수입니다.")
        UUID storeId,

        // 배송 주소
        @NotBlank(message = "배송 주소는 필수입니다.")
        String deliveryAddress,

        // 배송 상세 주소
        String deliveryDetailAddress,

        // 고객 요청사항
        String requestMessage,

        // 주문 상품 목록
        @Valid
        @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
        List<OrderItemRequestDto> orderItems,

        // 주문과 동시에 생성할 결제 수단
        @NotNull(message = "결제 수단은 필수입니다.")
        PaymentMethod paymentMethod
) {
}