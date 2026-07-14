package com.sparta.spartachallenge8282.payment.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 결제 환불 요청. (PATCH /api/v1/payments/{paymentId}/refund)
 */
public record PaymentRefundRequest(

        @NotBlank(message = "환불 사유는 필수입니다.")
        String reason
) {
}
