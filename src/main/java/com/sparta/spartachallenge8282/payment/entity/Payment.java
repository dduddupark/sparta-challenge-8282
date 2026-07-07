package com.sparta.spartachallenge8282.payment.entity;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
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
 * Order 도메인이 아직 없어 연관관계(@OneToOne) 대신 {@code orderId}(UUID) 컬럼으로 보관한다.
 * (추후 Order 엔티티 확정 시 association 전환 검토)
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



    // TODO: Order 도메인 확정 후 아래 JPA 연관관계로 전환하고 위 orderId(UUID) 컬럼은 제거.
    //       (현재는 Order 엔티티가 없어 컴파일 불가하므로 주석 처리)
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
                    String transactionId, LocalDateTime paidAt) {
        this.order = order;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.transactionId = transactionId;
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
