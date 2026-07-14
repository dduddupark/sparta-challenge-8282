package com.sparta.spartachallenge8282.payment.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.enums.OrderStatus;
import com.sparta.spartachallenge8282.order.repository.OrderRepository;
import com.sparta.spartachallenge8282.payment.presentation.dto.request.PaymentCancelRequest;
import com.sparta.spartachallenge8282.payment.presentation.dto.request.PaymentCreateRequest;
import com.sparta.spartachallenge8282.payment.presentation.dto.request.PaymentRefundRequest;
import com.sparta.spartachallenge8282.payment.presentation.dto.response.PaymentCancelResponse;
import com.sparta.spartachallenge8282.payment.presentation.dto.response.PaymentCreateResponse;
import com.sparta.spartachallenge8282.payment.presentation.dto.response.PaymentRefundResponse;
import com.sparta.spartachallenge8282.payment.presentation.dto.response.PaymentResponse;
import com.sparta.spartachallenge8282.payment.domain.Payment;
import com.sparta.spartachallenge8282.payment.domain.PaymentStatus;
import com.sparta.spartachallenge8282.payment.domain.PaymentRepository;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import com.sparta.spartachallenge8282.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 결제 도메인 서비스.
 *
 * <p>인터페이스/구현체 분리 없이 단일 {@code @Service} 클래스로 구성한다.
 * 금액 일치 검증, 중복 결제 방지, 상태 전이, 접근 권한(본인 주문 여부)을 담당한다.
 *
 * <p><b>OWNER 가게 스코프 검증</b>: OWNER 는 <b>본인 가게 결제만</b> 조회/취소할 수 있다.
 * {@code payment.order.storeId} 가 요청 OWNER 소유 가게인지 {@link StoreRepository} 로 대조하여
 * 타 가게 결제 접근(IDOR)을 차단한다. (Store 도메인 연동 완료 — 과거 TODO 해소)
 *
 * <p><b>제약(도메인 미완성)</b>
 * <ul>
 *   <li>PG 연동 부재: transactionId 는 임시 채번한다.</li>
 *   <li>Idempotency-Key: {@code p_payment.idempotency_key} 유니크 제약을 1차 방어선으로 삼는다.
 *       사전 조회로 중복을 판별하지 않고 <b>먼저 INSERT 를 시도</b>한 뒤, 유니크 제약 위반이 나면
 *       이미 저장된 결제와 이번 요청을 대조한다(insert-first). 같은 요청이면 최초 결과를 그대로
 *       반환(멱등), 같은 키에 다른 요청이면 {@code PAYMENT_IDEMPOTENCY_KEY_CONFLICT(60010)} 로 거부한다.
 *       이 방식은 사전조회-후-INSERT 사이의 경합(TOCTOU)을 원천 제거한다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PlatformTransactionManager txManager;

    // 롤 문자열 (user.role() 원문 — UserRole.getAuthority() 가 ROLE_ 접두사를 붙인 값과 동일).
    // 하드코딩 대신 UserRole enum(SSOT)에서 파생시켜 역할 코드/표기 변경에 자동 동기화한다.
    private static final String ROLE_CUSTOMER = UserRole.CUSTOMER.getAuthority();
    private static final String ROLE_OWNER    = UserRole.OWNER.getAuthority();
    private static final String ROLE_MANAGER  = UserRole.MANAGER.getAuthority();
    private static final String ROLE_MASTER   = UserRole.MASTER.getAuthority();

    /**
     * 결제 생성. 요청자는 주문 소유자여야 하고(본인 주문만 결제), 주문은 결제 대기(PENDING) 상태여야 하며,
     * {@code amount} 는 주문 금액과 일치해야 한다.
     *
     * <p><b>insert-first 멱등 처리</b> — 사전 조회 없이 곧바로 INSERT 를 시도한다(TOCTOU 경합 제거).
     * {@code idempotency_key}/{@code order_id} 유니크 제약 위반이 나면 {@link #resolveConflict}
     * 로 넘어가 이미 저장된 결제와 이번 요청을 대조한다.
     *
     * <p><b>트랜잭션 설계</b> — 이 메서드는 <b>트랜잭션 밖</b>(orchestrator)에서 동작한다.
     * flush 중 {@link DataIntegrityViolationException} 이 나면 그 트랜잭션은 rollback-only 로
     * 오염되므로, 같은 트랜잭션에서 재조회 후 정상 반환하면 커밋 시 예외가 난다.
     * 따라서 INSERT 는 {@code insertTx}, 충돌 재조회는 별도의 {@code REQUIRES_NEW} 읽기
     * 트랜잭션({@code resolveTx})으로 분리한다.
     *
     * @param request        결제 생성 요청 (orderId, amount, method)
     * @param userId         결제 요청자(로그인 사용자) ID
     * @param idempotencyKey 중복 결제 방지 키 (유니크 제약으로 멱등 보장)
     */
    public PaymentCreateResponse createPayment(PaymentCreateRequest request, Long userId, String idempotencyKey) {
        String key = normalizeKey(idempotencyKey);

        // 1) 곧바로 INSERT 시도 — 성공하면 그대로 반환.
        //    (주변에 트랜잭션이 없으면 새 트랜잭션을, 있으면 참여한다 — REQUIRED)
        TransactionTemplate insertTx = new TransactionTemplate(txManager);
        try {
            return insertTx.execute(status -> insertPayment(request, userId, key));
        } catch (DataIntegrityViolationException e) {
            // 2) 유니크 제약 위반 — 이미 저장된 결제와 이번 요청을 별도 트랜잭션에서 대조.
            //    (오염된 INSERT 트랜잭션과 격리하기 위해 REQUIRES_NEW 로 커밋된 승자를 조회)
            TransactionTemplate resolveTx = new TransactionTemplate(txManager);
            resolveTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            resolveTx.setReadOnly(true);
            return resolveTx.execute(status -> resolveConflict(request, userId, key));
        }
    }

    /**
     * 결제 INSERT 본체. 주문 조회·금액 검증 후 저장한다. 유니크 제약 위반 시
     * {@link DataIntegrityViolationException} 을 그대로 던져 호출부가 충돌을 판별하게 한다.
     */
    private PaymentCreateResponse insertPayment(PaymentCreateRequest request, Long userId, String key) {
        // 주문 조회 (60004)
        Order order = orderRepository.findByIdAndDeletedAtIsNull(request.orderId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));

        // 소유자 검증 — 본인 주문에만 결제할 수 있다(남의 주문 결제 생성 = IDOR 차단).
        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 주문 상태 검증 — 결제 대기(PENDING) 주문에만 결제 가능(취소/완료된 주문 결제 차단). (60011)
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new CustomException(ErrorCode.PAYMENT_ORDER_NOT_PAYABLE);
        }

        // 금액 일치 검증 (60006) — 요청 금액과 주문 총액이 같아야 함
        if (request.amount() != order.getTotalPrice()) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 결제 저장 (성공 처리 — PG 연동 부재로 transactionId 임시 채번).
        // saveAndFlush 로 즉시 INSERT 를 발생시켜 유니크 제약 위반을 이 자리에서 노출한다.
        Payment payment = Payment.builder()
                .order(order)
                .amount(request.amount())
                .method(request.method())
                .status(PaymentStatus.PAID)
                .transactionId(generateTransactionId())
                .idempotencyKey(key)
                .paidAt(LocalDateTime.now())
                .build();

        return PaymentCreateResponse.from(paymentRepository.saveAndFlush(payment));
    }

    /**
     * 유니크 제약 위반 해소 — 이미 저장된 결제와 이번 요청을 대조한다.
     *
     * <ul>
     *   <li>멱등키로 기존 결제를 찾고, 요청 지문(주문·금액·수단·소유자)이 <b>같으면</b>
     *       최초 결과를 그대로 반환(진짜 멱등 재요청).</li>
     *   <li>같은 키인데 요청 지문이 <b>다르면</b> {@code PAYMENT_IDEMPOTENCY_KEY_CONFLICT(60010)}.
     *       소유자 불일치도 여기서 걸러 남의 결제 정보 노출(IDOR)을 차단한다.</li>
     *   <li>키가 없거나 키로 못 찾은 충돌(=순수 주문 중복)이면 {@code PAYMENT_ALREADY_PROCESSED(60005)}.</li>
     * </ul>
     */
    private PaymentCreateResponse resolveConflict(PaymentCreateRequest request, Long userId, String key) {
        if (key != null) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                Payment payment = existing.get();
                if (isSameRequest(payment, request, userId)) {
                    return PaymentCreateResponse.from(payment); // 최초 결과 그대로 반환 (멱등)
                }
                throw new CustomException(ErrorCode.PAYMENT_IDEMPOTENCY_KEY_CONFLICT); // 같은 키, 다른 요청
            }
        }
        // 멱등키 없음 또는 키로 못 찾음 → order_id 유니크 위반(주문당 결제 1건)
        throw new CustomException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }

    /** 저장된 결제가 이번 요청과 동일한지(주문·금액·수단·소유자) 판별 — 멱등 재요청 여부 판정. */
    private boolean isSameRequest(Payment payment, PaymentCreateRequest request, Long userId) {
        return payment.getOrder().getId().equals(request.orderId())
                && payment.getAmount().equals(request.amount())
                && payment.getMethod() == request.method()
                && payment.getOrder().getUserId().equals(userId);
    }

    /** 주문의 결제 내역 조회. */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId, UserDetailsImpl user) {
        Payment payment = findActivePayment(paymentId);
        validateReadAccess(payment, user);
        return PaymentResponse.from(payment);
    }

    /** 결제 전체 목록 조회(관리자). status 는 선택 필터. */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getUserPayments(Long userId, Pageable pageable) {
        // 유저 존재 검증 (60009) — 없는 유저면 빈 페이지가 아니라 명시적 404
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw new CustomException(ErrorCode.PAYMENT_USER_NOT_FOUND);
        }
        return paymentRepository.findByOrder_UserIdAndDeletedAtIsNull(userId, pageable)
                .map(PaymentResponse::from);
    }

    /** 고객 본인 결제 목록 조회. (userId 는 인증 주체에서 도출 — 타인 조회 불가) */
    @Transactional(readOnly = true)
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
        if (ROLE_MANAGER.equals(role) || ROLE_MASTER.equals(role)) {
            return;
        }
        if (ROLE_OWNER.equals(role)) {
            validateOwnerStoreScope(payment, user); // 본인 가게 결제만
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
            validateOwnerStoreScope(payment, user); // 본인 가게 결제만 취소
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

    /**
     * 결제가 걸린 주문의 가게가 요청 OWNER 소유인지 검증.
     * {@code order.storeId} 로 Store 소유 여부를 대조해 타 가게 결제 접근(IDOR)을 차단한다.
     */
    private void validateOwnerStoreScope(Payment payment, UserDetailsImpl user) {
        UUID storeId = payment.getOrder().getStoreId();
        if (!storeRepository.existsByIdAndOwner_IdAndDeletedAtIsNull(storeId, user.userId())) {
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
