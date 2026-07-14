package com.sparta.spartachallenge8282.payment.presentation.dto.response;

import com.sparta.spartachallenge8282.payment.domain.Payment;
import com.sparta.spartachallenge8282.payment.domain.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 취소 응답. (PATCH /api/v1/payments/{paymentId}/cancel)
 */
public record PaymentCancelResponse(
        UUID paymentId,
        PaymentStatus status,
        LocalDateTime canceledAt,
        String canceledReason
) {
    public static PaymentCancelResponse from(Payment payment) {
        return new PaymentCancelResponse(
                payment.getId(),
                payment.getStatus(),
                payment.getCanceledAt(),
                payment.getCanceledReason()
        );
    }
}
