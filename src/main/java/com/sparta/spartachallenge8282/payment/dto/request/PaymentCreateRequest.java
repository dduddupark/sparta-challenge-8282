package com.sparta.spartachallenge8282.payment.dto.request;

import com.sparta.spartachallenge8282.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * 결제 생성 요청. (POST /api/v1/payments)
 *
 * <p>{@code amount} 는 주문 금액({@code p_order.total_price})과 일치해야 하며,
 * 불일치 시 서비스에서 {@code PAYMENT_AMOUNT_MISMATCH(60006)} 로 처리한다.
 */
public record PaymentCreateRequest(

        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @NotNull(message = "결제 금액은 필수입니다.")
        @Positive(message = "결제 금액은 0보다 커야 합니다.")
        Long amount,

        @NotNull(message = "결제 수단은 필수입니다.")
        PaymentMethod method
) {
}
