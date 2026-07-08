package com.sparta.spartachallenge8282.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 결제 취소 요청. (PATCH /api/v1/payments/{paymentId}/cancel)
 */
public record PaymentCancelRequest(

        @NotBlank(message = "취소 사유는 필수입니다.")
        String reason
) {
}
