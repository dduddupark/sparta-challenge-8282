package com.sparta.spartachallenge8282.payment.domain;

import com.sparta.spartachallenge8282.payment.domain.Payment;
import com.sparta.spartachallenge8282.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 결제 Repository.
 *
 * <p>Soft Delete 정책에 따라 조회는 {@code deletedAt IS NULL} 을 기본 조건으로 둔다.
 * (OrderRepository 컨벤션과 동일)
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /** 결제 단건 조회 (삭제되지 않은 것만) */
    Optional<Payment> findByIdAndDeletedAtIsNull(UUID id);

    /** 주문의 결제 조회 (삭제되지 않은 것만) */
    Optional<Payment> findByOrder_IdAndDeletedAtIsNull(UUID orderId);

    /** 해당 주문에 결제가 이미 존재하는지 (중복 결제 방지) */
    boolean existsByOrder_Id(UUID orderId);

    /** 멱등 키로 결제 조회 (동일 키 재요청 시 최초 결과 반환용). DB 유니크 제약과 동일하게 삭제 여부 무관 조회. */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /** 전체 결제 목록 (삭제되지 않은 것만) */
    Page<Payment> findByDeletedAtIsNull(Pageable pageable);

    /** 상태별 결제 목록 (삭제되지 않은 것만) */
    Page<Payment> findByStatusAndDeletedAtIsNull(PaymentStatus status, Pageable pageable);

    /** 특정 유저의 결제 목록 (주문 → 유저 기준, 삭제되지 않은 것만) */
    Page<Payment> findByOrder_UserIdAndDeletedAtIsNull(Long userId, Pageable pageable);
}
