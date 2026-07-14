package com.sparta.spartachallenge8282.payment.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.repository.OrderRepository;
import com.sparta.spartachallenge8282.payment.presentation.dto.request.PaymentCreateRequest;
import com.sparta.spartachallenge8282.payment.presentation.dto.response.PaymentCreateResponse;
import com.sparta.spartachallenge8282.payment.domain.PaymentMethod;
import com.sparta.spartachallenge8282.payment.domain.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 결제 멱등키 insert-first 처리 검증 (커밋된 결제 기반).
 *
 * <p>{@code createPayment} 는 사전 조회 없이 곧바로 INSERT 를 시도하고, 유니크 제약 위반 시
 * 별도 트랜잭션에서 기존 결제와 이번 요청을 대조한다. 이 경로는 <b>이미 커밋된 결제</b>가 있어야
 * 재현되므로(REQUIRES_NEW 재조회가 커밋된 승자를 봐야 함), 롤백 기반의
 * {@code PaymentServiceIntegrationTest} 가 아니라 커밋 기반의 본 테스트에서 검증한다.
 *
 * <p>각 결제는 독립 트랜잭션으로 커밋되므로 종료 후 수동 정리한다.
 */
@SpringBootTest
class PaymentIdempotencyTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    private final List<UUID> createdOrderIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        if (createdOrderIds.isEmpty()) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(s -> createdOrderIds.forEach(oid -> {
            paymentRepository.findByOrder_IdAndDeletedAtIsNull(oid)
                    .ifPresent(paymentRepository::delete);
            orderRepository.findById(oid).ifPresent(orderRepository::delete);
        }));
    }

    /** 주문을 커밋해 다른 트랜잭션에서도 보이게 한다. */
    private UUID persistCommittedOrder(Long userId, int totalPrice) {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        UUID id = tx.execute(s -> {
            Order order = Order.create(
                    "T" + UUID.randomUUID().toString().substring(0, 12),
                    userId,
                    UUID.randomUUID(),
                    totalPrice, 0, 0,
                    "서울특별시 종로구 세종대로 172", "3층", "문 앞에 두고 벨 눌러주세요."
            );
            return orderRepository.save(order).getId();
        });
        createdOrderIds.add(id);
        return id;
    }

    @Test
    @DisplayName("멱등 - 같은 키 + 같은 요청 재시도면 새 결제 없이 최초 결과를 그대로 반환한다")
    void idempotentReplaySameRequest() {
        UUID orderId = persistCommittedOrder(1L, 27000);
        String key = "idem-key-replay";
        PaymentCreateRequest req = new PaymentCreateRequest(orderId, 27000L, PaymentMethod.CARD);

        PaymentCreateResponse first = paymentService.createPayment(req, 1L, key);
        // 응답 유실 후 같은 키로 재시도
        PaymentCreateResponse retry = paymentService.createPayment(req, 1L, key);

        assertThat(retry.paymentId()).isEqualTo(first.paymentId());
        // 결제는 정확히 1건만 존재
        assertThat(paymentRepository.findByOrder_IdAndDeletedAtIsNull(orderId)).isPresent();
    }

    @Test
    @DisplayName("충돌 - 같은 키인데 다른 요청(다른 주문/소유자)이면 PAYMENT_IDEMPOTENCY_KEY_CONFLICT")
    void sameKeyDifferentRequestConflicts() {
        UUID orderA = persistCommittedOrder(1L, 27000); // 소유자 1
        UUID orderB = persistCommittedOrder(2L, 27000); // 소유자 2 (남의 주문)
        String key = "idem-key-shared";

        // 소유자 1 이 키로 결제 완료
        paymentService.createPayment(
                new PaymentCreateRequest(orderA, 27000L, PaymentMethod.CARD), 1L, key);

        // 소유자 2 가 같은 키를 재사용 → 남의 결제(orderA)를 반환하지 않고 충돌로 거부(IDOR 차단)
        assertThatThrownBy(() -> paymentService.createPayment(
                new PaymentCreateRequest(orderB, 27000L, PaymentMethod.CARD), 2L, key))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_IDEMPOTENCY_KEY_CONFLICT);

        // orderB 에는 결제가 생성되지 않는다
        assertThat(paymentRepository.findByOrder_IdAndDeletedAtIsNull(orderB)).isEmpty();
    }

    @Test
    @DisplayName("중복 - 키 없이 이미 결제된 주문을 다시 결제하면 PAYMENT_ALREADY_PROCESSED")
    void duplicateOrderWithoutKey() {
        UUID orderId = persistCommittedOrder(1L, 27000);
        PaymentCreateRequest req = new PaymentCreateRequest(orderId, 27000L, PaymentMethod.CARD);

        paymentService.createPayment(req, 1L, null);

        assertThatThrownBy(() -> paymentService.createPayment(req, 1L, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }
}
