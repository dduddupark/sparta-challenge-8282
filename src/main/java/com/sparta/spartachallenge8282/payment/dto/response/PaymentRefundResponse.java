package com.sparta.spartachallenge8282.payment.dto.response;

import com.sparta.spartachallenge8282.payment.entity.Payment;
import com.sparta.spartachallenge8282.payment.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 환불 응답. (PATCH /api/v1/payments/{paymentId}/refund)
 */
public record PaymentRefundResponse(
        UUID paymentId,
        PaymentStatus status,
        LocalDateTime refundedAt,
        String refundedReason
) {
    public static PaymentRefundResponse from(Payment payment) {
        return new PaymentRefundResponse(
                payment.getId(),
                payment.getStatus(),
                payment.getRefundedAt(),
                payment.getRefundedReason()
        );
    }
}
