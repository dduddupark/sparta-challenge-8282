package com.sparta.spartachallenge8282.payment.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.repository.OrderRepository;
import com.sparta.spartachallenge8282.payment.dto.request.PaymentCreateRequest;
import com.sparta.spartachallenge8282.payment.entity.PaymentMethod;
import com.sparta.spartachallenge8282.payment.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 결제 생성 동시성 테스트 (실제 커밋 경합 재현).
 *
 * <p>{@code createPayment} 는 {@code existsByOrder_Id} 사전 체크를 두지만, 동시 요청은
 * 둘 다 사전 체크를 통과할 수 있다. 이때 {@code order_id} 유니크 제약이 최종 방어선이며,
 * {@code saveAndFlush} 가 던지는 {@link org.springframework.dao.DataIntegrityViolationException}
 * 을 {@code PAYMENT_ALREADY_PROCESSED(60005)} 로 변환하는지 검증한다.
 *
 * <p>롤백 트랜잭션 테스트로는 커밋 경합을 재현할 수 없어, 각 시도를 독립 트랜잭션으로
 * 커밋하고 종료 후 수동 정리한다.
 */
@SpringBootTest
class PaymentConcurrencyTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    private UUID orderId;

    @AfterEach
    void cleanup() {
        if (orderId == null) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(txManager);
        UUID oid = orderId;
        tx.executeWithoutResult(s -> {
            paymentRepository.findByOrder_IdAndDeletedAtIsNull(oid)
                    .ifPresent(paymentRepository::delete);
            orderRepository.findById(oid).ifPresent(orderRepository::delete);
        });
    }

    @Test
    @DisplayName("동시에 같은 주문을 결제하면 정확히 1건만 성공하고 나머지는 PAYMENT_ALREADY_PROCESSED(60005)")
    void concurrentCreateOnlyOneSucceeds() throws InterruptedException {
        // given: 다른 트랜잭션에서도 보이도록 주문을 커밋
        TransactionTemplate tx = new TransactionTemplate(txManager);
        this.orderId = tx.execute(s -> {
            Order order = Order.create(
                    "T" + UUID.randomUUID().toString().substring(0, 12),
                    1L,
                    UUID.randomUUID(),
                    27000, 0, 0,
                    "서울특별시 종로구 세종대로 172", "3층", "문 앞에 두고 벨 눌러주세요."
            );
            return orderRepository.save(order).getId();
        });

        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger alreadyProcessed = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    // 각 시도를 독립 트랜잭션으로 커밋 → 실제 경합 재현
                    tx.executeWithoutResult(s -> paymentService.createPayment(
                            new PaymentCreateRequest(orderId, 27000L, PaymentMethod.CARD), 1L, null));
                    success.incrementAndGet();
                } catch (CustomException e) {
                    if (e.getErrorCode() == ErrorCode.PAYMENT_ALREADY_PROCESSED) {
                        alreadyProcessed.incrementAndGet();
                    } else {
                        unexpected.incrementAndGet();
                    }
                } catch (Exception e) {
                    unexpected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();        // 모든 스레드 대기 상태 진입
        start.countDown();    // 동시 출발
        done.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(success.get()).isEqualTo(1);
        assertThat(alreadyProcessed.get()).isEqualTo(threads - 1);
        assertThat(unexpected.get()).isZero();
        // DB 에도 정확히 1건만 남는다
        assertThat(paymentRepository.findByOrder_IdAndDeletedAtIsNull(orderId)).isPresent();
    }
}
