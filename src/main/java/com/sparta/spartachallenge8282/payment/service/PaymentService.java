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
import com.sparta.spartachallenge8282.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
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
 *   <li>PG 연동 부재: transactionId 는 임시 채번한다.</li>
 *   <li>Idempotency-Key: {@code p_payment.idempotency_key} 에 저장하며, 동일 키 재요청은
 *       결제를 새로 만들지 않고 최초 결과를 그대로 반환한다(멱등). 순차 재시도는 사전 조회로,
 *       완전 동시 요청의 패자는 유니크 제약(60005)으로 방어한다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    // 롤 문자열 (user.role() 원문 — UserRole.getAuthority() 가 ROLE_ 접두사를 붙인 값)
    private static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    private static final String ROLE_OWNER    = "ROLE_OWNER";
    private static final String ROLE_MANAGER  = "ROLE_MANAGER";
    private static final String ROLE_MASTER   = "ROLE_MASTER";

    /**
     * 결제 생성. {@code amount} 는 주문 금액과 일치해야 한다.
     *
     * @param request        결제 생성 요청 (orderId, amount, method)
     * @param userId         결제 요청자(로그인 사용자) ID
     * @param idempotencyKey 중복 결제 방지 키(현재 미저장, 유니크 제약으로 대체)
     */
    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request, Long userId, String idempotencyKey) {
        String key = normalizeKey(idempotencyKey);

        // 0) 멱등 재요청 — 동일 키의 결제가 이미 있으면 새로 생성하지 않고 최초 결과를 그대로 반환.
        //    클라이언트가 응답 유실로 재시도(같은 키 재전송)해도 결제는 딱 1건만 발생한다.
        if (key != null) {
            Optional<Payment> replayed = paymentRepository.findByIdempotencyKey(key);
            if (replayed.isPresent()) {
                return PaymentCreateResponse.from(replayed.get());
            }
        }

        // 1) 주문 조회 (60004)
        Order order = orderRepository.findByIdAndDeletedAtIsNull(request.orderId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));

        // 2) 금액 일치 검증 (60006) — 요청 금액과 주문 총액이 같아야 함
        if (request.amount() != order.getTotalPrice()) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 3) 중복 결제 방지 (60005) — 주문당 결제 1건 (사전 체크: 빠른 실패 경로)
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
                .idempotencyKey(key)
                .paidAt(LocalDateTime.now())
                .build();

        // 사전 체크를 통과한 동시 요청은 order_id(또는 idempotency_key) 유니크 제약에서 충돌한다.
        // saveAndFlush 로 즉시 INSERT 를 발생시켜 이 트랜잭션 안에서 제약 위반을 잡고
        // 500 대신 60005(중복 결제)로 변환한다.
        // (동일 키의 완전 동시 요청 시 패자는 60005 를 받는다. 순차 재시도는 위 0) 단계에서 멱등 반환된다.)
        try {
            return PaymentCreateResponse.from(paymentRepository.saveAndFlush(payment));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
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

    // ── 주문(Order) 연동용 내부 API ──────────────────────────────────────────────
    // 주문 상태 변화에 따라 Order 도메인에서 호출한다. REST 진입점이 아니므로 롤 기반
    // 권한 검증을 두지 않는다(호출 주체 검증은 Order 서비스 책임). 상태 전이 규칙(PAID 가드)만 강제한다.
    // 취소 주체 기준: 가게(사장) 사유 → cancel(CANCELED), 고객 요청 → refund(REFUNDED).

    /**
     * 주문 취소에 따른 결제 취소(가게 사유). 미수락/거절/수락 후 사장 취소 시 Order 에서 호출.
     * 결제가 없으면 조용히 무시한다(멱등: 결제 미생성 주문의 취소는 정상 흐름).
     */
    @Transactional
    public void cancelByOrder(UUID orderId, String reason) {
        paymentRepository.findByOrder_IdAndDeletedAtIsNull(orderId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.PAID) {
                throw new CustomException(ErrorCode.PAYMENT_NOT_CANCELABLE); // 60007
            }
            payment.cancel(reason);
        });
    }

    /**
     * 주문 취소에 따른 결제 환불(고객 요청). 고객이 5분 내 취소 시 Order 에서 호출.
     * 결제가 없으면 조용히 무시한다(멱등).
     */
    @Transactional
    public void refundByOrder(UUID orderId, String reason) {
        paymentRepository.findByOrder_IdAndDeletedAtIsNull(orderId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.PAID) {
                throw new CustomException(ErrorCode.PAYMENT_NOT_REFUNDABLE); // 60008
            }
            payment.refund(reason);
        });
    }

    /** 특정 유저 결제 내역 조회(관리자). */
    public Page<PaymentResponse> getUserPayments(Long userId, Pageable pageable) {
        // 유저 존재 검증 (60009) — 없는 유저면 빈 페이지가 아니라 명시적 404
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw new CustomException(ErrorCode.PAYMENT_USER_NOT_FOUND);
        }
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

    /** 멱등 키 정규화 — 공백/빈 문자열은 키 미전송(null)으로 취급. */
    private String normalizeKey(String idempotencyKey) {
        return (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey;
    }
}
