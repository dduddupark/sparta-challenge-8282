package com.sparta.spartachallenge8282.payment.presentation.dto.response;

import com.sparta.spartachallenge8282.payment.domain.Payment;
import com.sparta.spartachallenge8282.payment.domain.PaymentMethod;
import com.sparta.spartachallenge8282.payment.domain.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 상세 응답. (단건/주문별 조회 및 목록 항목)
 */
public record PaymentResponse(
        UUID paymentId,
        UUID orderId,
        Long amount,
        PaymentMethod method,
        PaymentStatus status,
        LocalDateTime paidAt,
        LocalDateTime canceledAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        String canceledReason,
        String refundedReason
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getCanceledAt(),
                payment.getRefundedAt(),
                payment.getCreatedAt(),
                payment.getCanceledReason(),
                payment.getRefundedReason()
        );
    }
}
