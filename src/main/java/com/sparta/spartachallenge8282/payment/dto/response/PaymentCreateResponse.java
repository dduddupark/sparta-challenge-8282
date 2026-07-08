package com.sparta.spartachallenge8282.payment.dto.response;

import com.sparta.spartachallenge8282.payment.entity.Payment;
import com.sparta.spartachallenge8282.payment.entity.PaymentMethod;
import com.sparta.spartachallenge8282.payment.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 생성 응답. (POST /api/v1/payments → 201 Created)
 */
public record PaymentCreateResponse(
        UUID paymentId,
        UUID orderId,
        Long amount,
        PaymentMethod method,
        PaymentStatus status,
        LocalDateTime createdAt
) {
    public static PaymentCreateResponse from(Payment payment) {
        return new PaymentCreateResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}
