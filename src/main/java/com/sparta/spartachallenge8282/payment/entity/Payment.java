package com.sparta.spartachallenge8282.payment.entity;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import com.sparta.spartachallenge8282.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 엔티티 (p_payment).
 *
 * <p>주문(p_order)과 1:1 관계이며 {@code amount} 는 {@code p_order.total_price} 와 항상 동일해야 한다.
 * 현재 {@link Order} 는 임시(placeholder) 엔티티이므로, Order 도메인 정식 구현 후
 * 필요한 필드/제약을 재검토한다.
 *
 * <p>상태 전이는 {@link #cancel}, {@link #refund}, {@link #fail} 로만 수행한다(무분별한 setter 금지).
 * 전이 가능 여부·권한 검증 등 비즈니스 규칙은 서비스 계층에서 담당한다.
 */
@Entity
@Getter
@Table(name = "p_payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;



    // NOTE: Order 는 현재 임시(placeholder) 엔티티. 정식 Order 도메인 구현 후 연관관계 재검토.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    /** 결제 금액. p_order.total_price 와 동일해야 함. (ERD상 결제 금액만 bigint) */
    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /** PG 거래 ID. */
    @Column(name = "transaction_id", nullable = false, unique = true, length = 100)
    private String transactionId;

    /**
     * 멱등 키. 동일 키의 재요청은 결제를 새로 생성하지 않고 최초 결과를 그대로 반환한다.
     * 키를 보내지 않은 요청은 null(유니크 제약상 다중 null 허용). 키 저장 시 유니크 제약이 중복을 방지한다.
     */
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    private LocalDateTime paidAt;

    private LocalDateTime canceledAt;

    @Column(length = 255)
    private String canceledReason;

    private LocalDateTime refundedAt;

    @Column(length = 255)
    private String refundedReason;

    @Column(length = 255)
    private String failedReason;

    @Builder
    private Payment(Order order, Long amount, PaymentMethod method, PaymentStatus status,
                    String transactionId, String idempotencyKey, LocalDateTime paidAt) {
        this.order = order;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.transactionId = transactionId;
        this.idempotencyKey = idempotencyKey;
        this.paidAt = paidAt;
    }

    // ── 상태 전이 ──────────────────────────────────────────────────────────────

    /** 결제 취소 처리. (가능 여부 검증은 서비스에서) */
    public void cancel(String reason) {
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.canceledReason = reason;
    }

    /** 결제 환불 처리. (가능 여부 검증은 서비스에서) */
    public void refund(String reason) {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
        this.refundedReason = reason;
    }

    /** 결제 실패 처리. */
    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failedReason = reason;
    }
}
