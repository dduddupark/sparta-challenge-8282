package com.sparta.spartachallenge8282.payment.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.repository.OrderRepository;
import com.sparta.spartachallenge8282.payment.dto.request.PaymentCancelRequest;
import com.sparta.spartachallenge8282.payment.dto.request.PaymentCreateRequest;
import com.sparta.spartachallenge8282.payment.dto.request.PaymentRefundRequest;
import com.sparta.spartachallenge8282.payment.dto.response.PaymentCancelResponse;
import com.sparta.spartachallenge8282.payment.dto.response.PaymentCreateResponse;
import com.sparta.spartachallenge8282.payment.dto.response.PaymentRefundResponse;
import com.sparta.spartachallenge8282.payment.dto.response.PaymentResponse;
import com.sparta.spartachallenge8282.payment.entity.Payment;
import com.sparta.spartachallenge8282.payment.entity.PaymentStatus;
import com.sparta.spartachallenge8282.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 도메인 서비스.
 *
 * <p>인터페이스/구현체 분리 없이 단일 {@code @Service} 클래스로 구성한다.
 * 금액 일치 검증, 중복 결제 방지, 상태 전이, 접근 권한(본인 주문 여부)을 담당한다.
 *
 * <p><b>제약(도메인 미완성)</b>
 * <ul>
 *   <li>OWNER "본인 가게" 검증: Store 도메인이 없어 롤 레벨(@PreAuthorize)까지만 검증하고
 *       가게 소유 여부는 TODO 로 남긴다.</li>
 *   <li>유저 존재 검증(60009): User 도메인이 없어 생략한다(빈 페이지 반환).</li>
 *   <li>PG 연동 부재: transactionId 는 임시 채번한다.</li>
 *   <li>Idempotency-Key: 저장 컬럼이 없어, 주문당 1건 유니크 제약(중복 결제 60005)으로 대체한다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    // 롤 문자열 (UserDetailsImpl.getRole() 원문 — ROLE_ 접두사 없음 가정)
    //TODO User Enum 있으면 교체해줘야함
    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String ROLE_OWNER    = "OWNER";
    private static final String ROLE_MANAGER  = "MANAGER";
    private static final String ROLE_MASTER   = "MASTER";

    /**
     * 결제 생성. {@code amount} 는 주문 금액과 일치해야 한다.
     *
     * @param request        결제 생성 요청 (orderId, amount, method)
     * @param userId         결제 요청자(로그인 사용자) ID
     * @param idempotencyKey 중복 결제 방지 키(현재 미저장, 유니크 제약으로 대체)
     */
    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request, Long userId, String idempotencyKey) {
        // 1) 주문 조회 (60004)
        Order order = orderRepository.findByIdAndDeletedAtIsNull(request.orderId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));

        // 2) 금액 일치 검증 (60006) — 요청 금액과 주문 총액이 같아야 함
        if (request.amount() != order.getTotalPrice()) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 3) 중복 결제 방지 (60005) — 주문당 결제 1건
        if (paymentRepository.existsByOrder_Id(order.getId())) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        // 4) 결제 저장 (성공 처리 — PG 연동 부재로 transactionId 임시 채번)
        Payment payment = Payment.builder()
                .order(order)
                .amount(request.amount())
                .method(request.method())
                .status(PaymentStatus.PAID)
                .transactionId(generateTransactionId())
                .paidAt(LocalDateTime.now())
                .build();

        return PaymentCreateResponse.from(paymentRepository.save(payment));
    }

    /** 주문의 결제 내역 조회. */
    public PaymentResponse getPaymentByOrder(UUID orderId, UserDetailsImpl user) {
        // 주문 존재 여부 먼저 확인 (60004) → 결제 존재 여부 (60001)
        orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));

        Payment payment = paymentRepository.findByOrder_IdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        validateReadAccess(payment, user);
        return PaymentResponse.from(payment);
    }

    /** 결제 단건 조회. */
    public PaymentResponse getPayment(UUID paymentId, UserDetailsImpl user) {
        Payment payment = findActivePayment(paymentId);
        validateReadAccess(payment, user);
        return PaymentResponse.from(payment);
    }

    /** 결제 전체 목록 조회(관리자). status 는 선택 필터. */
    public Page<PaymentResponse> getPayments(PaymentStatus status, Pageable pageable) {
        Page<Payment> payments = (status == null)
                ? paymentRepository.findByDeletedAtIsNull(pageable)
                : paymentRepository.findByStatusAndDeletedAtIsNull(status, pageable);
        return payments.map(PaymentResponse::from);
    }

    /** 결제 취소. (PAID 상태만 가능) */
    @Transactional
    public PaymentCancelResponse cancelPayment(UUID paymentId, PaymentCancelRequest request, UserDetailsImpl user) {
        Payment payment = findActivePayment(paymentId);
        validateCancelAccess(payment, user); // OWNER/MANAGER/MASTER 만 (OWNER 가게 스코프는 TODO)

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_CANCELABLE); // 60007
        }

        payment.cancel(request.reason());
        return PaymentCancelResponse.from(payment);
    }

    /** 결제 환불. (PAID 상태만 가능) */
    @Transactional
    public PaymentRefundResponse refundPayment(UUID paymentId, PaymentRefundRequest request, UserDetailsImpl user) {
        Payment payment = findActivePayment(paymentId);
        validateOwnerOrAdmin(payment, user); // CUSTOMER 는 본인 주문만 환불 요청 가능

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_REFUNDABLE); // 60008
        }

        payment.refund(request.reason());
        return PaymentRefundResponse.from(payment);
    }

    /** 특정 유저 결제 내역 조회(관리자). */
    public Page<PaymentResponse> getUserPayments(Long userId, Pageable pageable) {
        // TODO: User 도메인 확정 후 유저 존재 검증 (60009)
        return paymentRepository.findByOrder_UserIdAndDeletedAtIsNull(userId, pageable)
                .map(PaymentResponse::from);
    }

    /** 고객 본인 결제 목록 조회. (userId 는 인증 주체에서 도출 — 타인 조회 불가) */
    public Page<PaymentResponse> getMyPayments(UserDetailsImpl user, Pageable pageable) {
        return paymentRepository.findByOrder_UserIdAndDeletedAtIsNull(user.userId(), pageable)
                .map(PaymentResponse::from);
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────────────────

    private Payment findActivePayment(UUID paymentId) {
        return paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND)); // 60001
    }

    /**
     * 조회 접근 권한 검증.
     * MANAGER/MASTER 는 전체 허용, CUSTOMER 는 본인 주문만, OWNER 는 (가게 검증은 TODO) 롤 레벨 허용.
     */
    private void validateReadAccess(Payment payment, UserDetailsImpl user) {
        String role = user.role();
        if (ROLE_MANAGER.equals(role) || ROLE_MASTER.equals(role) || ROLE_OWNER.equals(role)) {
            // TODO: OWNER 는 본인 가게(payment.order.storeId) 결제인지 Store 도메인으로 검증
            return;
        }
        if (ROLE_CUSTOMER.equals(role)) {
            validateOrderOwner(payment, user);
            return;
        }
        throw new CustomException(ErrorCode.ACCESS_DENIED);
    }

    /**
     * 결제 취소 동작의 권한 검증.
     * 명세상 취소 주체는 OWNER(및 MANAGER/MASTER). CUSTOMER 등 그 외 롤은 거부한다.
     */
    private void validateCancelAccess(Payment payment, UserDetailsImpl user) {
        String role = user.role();
        if (ROLE_MANAGER.equals(role) || ROLE_MASTER.equals(role)) {
            return;
        }
        if (ROLE_OWNER.equals(role)) {
            // TODO: OWNER 는 본인 가게(payment.order.storeId) 결제인지 Store 도메인으로 검증
            return;
        }
        throw new CustomException(ErrorCode.ACCESS_DENIED);
    }

    /**
     * 환불 등 CUSTOMER 주체 동작의 권한 검증.
     * MANAGER/MASTER 는 허용, CUSTOMER 는 본인 주문만 허용.
     */
    private void validateOwnerOrAdmin(Payment payment, UserDetailsImpl user) {
        String role = user.role();
        if (ROLE_MANAGER.equals(role) || ROLE_MASTER.equals(role)) {
            return;
        }
        if (ROLE_CUSTOMER.equals(role)) {
            validateOrderOwner(payment, user);
            return;
        }
        throw new CustomException(ErrorCode.ACCESS_DENIED);
    }

    /** 결제가 걸린 주문이 요청자 본인 것인지 검증. */
    private void validateOrderOwner(Payment payment, UserDetailsImpl user) {
        if (!payment.getOrder().getUserId().equals(user.userId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    /** PG 연동 부재 상태의 임시 거래 ID 채번. */
    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID();
    }
}
