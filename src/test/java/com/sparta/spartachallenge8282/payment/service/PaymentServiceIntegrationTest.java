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
import com.sparta.spartachallenge8282.payment.entity.PaymentMethod;
import com.sparta.spartachallenge8282.payment.entity.PaymentStatus;
import com.sparta.spartachallenge8282.payment.repository.PaymentRepository;
import com.sparta.spartachallenge8282.user.entity.User;
import com.sparta.spartachallenge8282.user.entity.UserRole;
import com.sparta.spartachallenge8282.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PaymentService 통합 테스트 (실제 PostgreSQL 사용).
 *
 * <p>{@code @Transactional} 로 각 테스트는 종료 후 롤백된다.
 * Payment 는 Order 에 종속되므로, 각 테스트는 예시 Order 를 먼저 persist 한 뒤 진행한다.
 */
@SpringBootTest
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long OTHER_CUSTOMER_ID = 2L;

    // ── 테스트 픽스처 ───────────────────────────────────────────────────────────

    /** 예시 주문 저장. totalPrice = menuTotalPrice(=totalPrice) - 0 + 0 이 되도록 구성. */
    private Order persistOrder(Long userId, int totalPrice) {
        Order order = Order.create(
                "T" + UUID.randomUUID().toString().substring(0, 12), // 주문번호(<=30자, 유니크)
                userId,
                UUID.randomUUID(),          // storeId
                totalPrice,                 // menuTotalPrice
                0,                          // discountAmount
                0,                          // deliveryFee
                "서울특별시 종로구 세종대로 172",
                "3층",
                "문 앞에 두고 벨 눌러주세요."
        );
        return orderRepository.save(order);
    }

    /** 예시 유저 저장 후 생성된 PK 반환. */
    private Long persistUser() {
        User user = User.builder()
                .email("u" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .password("encoded")
                .nickname("tester")
                .address("서울특별시 종로구 세종대로 172")
                .role(UserRole.CUSTOMER)
                .build();
        return userRepository.save(user).getId();
    }

    private UserDetailsImpl customer(Long userId) {
        return new UserDetailsImpl(userId, "customer" + userId + "@test.com", "CUSTOMER");
    }

    private UserDetailsImpl owner() {
        return new UserDetailsImpl(9L, "owner@test.com", "OWNER");
    }

    /** 예시 주문 + 결제(PAID) 생성 후 결제 응답 반환. */
    private PaymentCreateResponse createPaidPayment(Long userId, int amount) {
        Order order = persistOrder(userId, amount);
        return paymentService.createPayment(
                new PaymentCreateRequest(order.getId(), (long) amount, PaymentMethod.CARD),
                userId, "idem-" + order.getId());
    }

    // ── 1. 결제 생성 ───────────────────────────────────────────────────────────
    @Nested
    @DisplayName("결제 생성")
    class CreatePayment {

        @Test
        @DisplayName("성공 - 주문 금액과 일치하면 PAID 상태로 결제가 생성된다")
        void success() {
            Order order = persistOrder(CUSTOMER_ID, 27000);

            PaymentCreateResponse res = paymentService.createPayment(
                    new PaymentCreateRequest(order.getId(), 27000L, PaymentMethod.CARD),
                    CUSTOMER_ID, "idem-key-1");

            assertThat(res.paymentId()).isNotNull();
            assertThat(res.orderId()).isEqualTo(order.getId());
            assertThat(res.amount()).isEqualTo(27000L);
            assertThat(res.method()).isEqualTo(PaymentMethod.CARD);
            assertThat(res.status()).isEqualTo(PaymentStatus.PAID);
            // 실제 DB 저장 확인
            assertThat(paymentRepository.existsByOrder_Id(order.getId())).isTrue();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주문이면 PAYMENT_ORDER_NOT_FOUND")
        void orderNotFound() {
            PaymentCreateRequest req =
                    new PaymentCreateRequest(UUID.randomUUID(), 27000L, PaymentMethod.CARD);

            assertThatThrownBy(() -> paymentService.createPayment(req, CUSTOMER_ID, null))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 요청 금액이 주문 금액과 다르면 PAYMENT_AMOUNT_MISMATCH")
        void amountMismatch() {
            Order order = persistOrder(CUSTOMER_ID, 27000);
            PaymentCreateRequest req =
                    new PaymentCreateRequest(order.getId(), 30000L, PaymentMethod.CARD);

            assertThatThrownBy(() -> paymentService.createPayment(req, CUSTOMER_ID, null))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        @Test
        @DisplayName("멱등 - 같은 Idempotency-Key 재요청이면 새 결제 없이 최초 결과를 그대로 반환한다")
        void idempotentReplay() {
            Order order = persistOrder(CUSTOMER_ID, 27000);
            String key = "idem-key-replay";

            PaymentCreateResponse first = paymentService.createPayment(
                    new PaymentCreateRequest(order.getId(), 27000L, PaymentMethod.CARD), CUSTOMER_ID, key);

            // 같은 키로 재요청 (재시도 시나리오)
            PaymentCreateResponse retry = paymentService.createPayment(
                    new PaymentCreateRequest(order.getId(), 27000L, PaymentMethod.CARD), CUSTOMER_ID, key);

            // 동일한 결제가 반환되고, DB 에도 결제는 1건뿐
            assertThat(retry.paymentId()).isEqualTo(first.paymentId());
            assertThat(paymentRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("실패 - 이미 결제된 주문이면 PAYMENT_ALREADY_PROCESSED")
        void alreadyProcessed() {
            Order order = persistOrder(CUSTOMER_ID, 27000);
            paymentService.createPayment(
                    new PaymentCreateRequest(order.getId(), 27000L, PaymentMethod.CARD),
                    CUSTOMER_ID, null);

            PaymentCreateRequest again =
                    new PaymentCreateRequest(order.getId(), 27000L, PaymentMethod.CARD);

            assertThatThrownBy(() -> paymentService.createPayment(again, CUSTOMER_ID, null))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
    }

    // ── 2. 주문의 결제 내역 조회 ─────────────────────────────────────────────────
    @Nested
    @DisplayName("주문의 결제 내역 조회")
    class GetPaymentByOrder {

        @Test
        @DisplayName("성공 - 본인 주문이면 결제 내역을 조회한다")
        void success() {
            Order order = persistOrder(CUSTOMER_ID, 27000);
            paymentService.createPayment(
                    new PaymentCreateRequest(order.getId(), 27000L, PaymentMethod.CARD),
                    CUSTOMER_ID, null);

            PaymentResponse res = paymentService.getPaymentByOrder(order.getId(), customer(CUSTOMER_ID));

            assertThat(res.orderId()).isEqualTo(order.getId());
            assertThat(res.status()).isEqualTo(PaymentStatus.PAID);
            assertThat(res.amount()).isEqualTo(27000L);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주문이면 PAYMENT_ORDER_NOT_FOUND")
        void orderNotFound() {
            assertThatThrownBy(() ->
                    paymentService.getPaymentByOrder(UUID.randomUUID(), customer(CUSTOMER_ID)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 주문은 있으나 결제가 없으면 PAYMENT_NOT_FOUND")
        void paymentNotFound() {
            Order order = persistOrder(CUSTOMER_ID, 27000);

            assertThatThrownBy(() ->
                    paymentService.getPaymentByOrder(order.getId(), customer(CUSTOMER_ID)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 타인의 주문 결제를 CUSTOMER 가 조회하면 ACCESS_DENIED")
        void otherCustomerDenied() {
            Order order = persistOrder(CUSTOMER_ID, 27000);
            paymentService.createPayment(
                    new PaymentCreateRequest(order.getId(), 27000L, PaymentMethod.CARD),
                    CUSTOMER_ID, null);

            assertThatThrownBy(() ->
                    paymentService.getPaymentByOrder(order.getId(), customer(OTHER_CUSTOMER_ID)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }

    // ── 3. 결제 취소 ───────────────────────────────────────────────────────────
    @Nested
    @DisplayName("결제 취소")
    class CancelPayment {

        @Test
        @DisplayName("성공 - OWNER 가 PAID 결제를 취소하면 CANCELED 로 전이된다")
        void success() {
            PaymentCreateResponse created = createPaidPayment(CUSTOMER_ID, 27000);

            PaymentCancelResponse res = paymentService.cancelPayment(
                    created.paymentId(), new PaymentCancelRequest("사장님 미수락"), owner());

            assertThat(res.status()).isEqualTo(PaymentStatus.CANCELED);
            assertThat(res.canceledReason()).isEqualTo("사장님 미수락");
            assertThat(res.canceledAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 - CUSTOMER 는 결제를 취소할 수 없다 (ACCESS_DENIED)")
        void customerDenied() {
            PaymentCreateResponse created = createPaidPayment(CUSTOMER_ID, 27000);

            assertThatThrownBy(() -> paymentService.cancelPayment(
                    created.paymentId(), new PaymentCancelRequest("취소요청"), customer(CUSTOMER_ID)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("실패 - 이미 취소된 결제는 다시 취소할 수 없다 (PAYMENT_NOT_CANCELABLE)")
        void notCancelable() {
            PaymentCreateResponse created = createPaidPayment(CUSTOMER_ID, 27000);
            paymentService.cancelPayment(created.paymentId(), new PaymentCancelRequest("1차 취소"), owner());

            assertThatThrownBy(() -> paymentService.cancelPayment(
                    created.paymentId(), new PaymentCancelRequest("2차 취소"), owner()))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_CANCELABLE);
        }
    }

    // ── 4. 결제 환불 ───────────────────────────────────────────────────────────
    @Nested
    @DisplayName("결제 환불")
    class RefundPayment {

        @Test
        @DisplayName("성공 - 본인 CUSTOMER 가 PAID 결제를 환불하면 REFUNDED 로 전이된다")
        void success() {
            PaymentCreateResponse created = createPaidPayment(CUSTOMER_ID, 27000);

            PaymentRefundResponse res = paymentService.refundPayment(
                    created.paymentId(), new PaymentRefundRequest("단순 변심"), customer(CUSTOMER_ID));

            assertThat(res.status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(res.refundedReason()).isEqualTo("단순 변심");
            assertThat(res.refundedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 - 타인 CUSTOMER 가 환불 요청하면 ACCESS_DENIED")
        void otherCustomerDenied() {
            PaymentCreateResponse created = createPaidPayment(CUSTOMER_ID, 27000);

            assertThatThrownBy(() -> paymentService.refundPayment(
                    created.paymentId(), new PaymentRefundRequest("환불"), customer(OTHER_CUSTOMER_ID)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("실패 - PAID 가 아니면 환불할 수 없다 (PAYMENT_NOT_REFUNDABLE)")
        void notRefundable() {
            PaymentCreateResponse created = createPaidPayment(CUSTOMER_ID, 27000);
            paymentService.refundPayment(
                    created.paymentId(), new PaymentRefundRequest("1차 환불"), customer(CUSTOMER_ID));

            assertThatThrownBy(() -> paymentService.refundPayment(
                    created.paymentId(), new PaymentRefundRequest("2차 환불"), customer(CUSTOMER_ID)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_REFUNDABLE);
        }
    }

    // ── 4-1. 특정 유저 결제 내역 조회(관리자) ────────────────────────────────────
    @Nested
    @DisplayName("특정 유저 결제 내역 조회")
    class GetUserPayments {

        @Test
        @DisplayName("성공 - 존재하는 유저의 결제 목록을 반환한다")
        void success() {
            Long userId = persistUser();
            createPaidPayment(userId, 27000);

            var page = paymentService.getUserPayments(userId, org.springframework.data.domain.PageRequest.of(0, 20));

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().get(0).amount()).isEqualTo(27000L);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 유저면 PAYMENT_USER_NOT_FOUND")
        void userNotFound() {
            Long missingUserId = 999_999L;

            assertThatThrownBy(() ->
                    paymentService.getUserPayments(missingUserId, org.springframework.data.domain.PageRequest.of(0, 20)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_USER_NOT_FOUND);
        }
    }

    // ── 4-2. 주문 연동 취소/환불 (내부 API) ──────────────────────────────────────
    @Nested
    @DisplayName("주문 연동 취소/환불")
    class OrderLinkedTransition {

        @Test
        @DisplayName("cancelByOrder - 가게 사유 취소면 PAID → CANCELED")
        void cancelByOrder() {
            PaymentCreateResponse created = createPaidPayment(CUSTOMER_ID, 27000);

            paymentService.cancelByOrder(created.orderId(), "사장님 미수락");

            Payment reloaded = paymentRepository.findById(created.paymentId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.CANCELED);
            assertThat(reloaded.getCanceledReason()).isEqualTo("사장님 미수락");
        }

        @Test
        @DisplayName("refundByOrder - 고객 요청 취소면 PAID → REFUNDED")
        void refundByOrder() {
            PaymentCreateResponse created = createPaidPayment(CUSTOMER_ID, 27000);

            paymentService.refundByOrder(created.orderId(), "고객 5분내 취소");

            Payment reloaded = paymentRepository.findById(created.paymentId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(reloaded.getRefundedReason()).isEqualTo("고객 5분내 취소");
        }

        @Test
        @DisplayName("cancelByOrder - PAID 가 아니면 PAYMENT_NOT_CANCELABLE")
        void cancelByOrderNotPaid() {
            PaymentCreateResponse created = createPaidPayment(CUSTOMER_ID, 27000);
            paymentService.refundByOrder(created.orderId(), "먼저 환불"); // PAID 아님으로 만들기

            assertThatThrownBy(() -> paymentService.cancelByOrder(created.orderId(), "재취소"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_CANCELABLE);
        }

        @Test
        @DisplayName("cancelByOrder - 결제가 없는 주문이면 조용히 무시(예외 없음)")
        void cancelByOrderNoPayment() {
            Order order = persistOrder(CUSTOMER_ID, 27000); // 결제 미생성

            paymentService.cancelByOrder(order.getId(), "결제 없음");

            assertThat(paymentRepository.existsByOrder_Id(order.getId())).isFalse();
        }
    }

    // ── 5. 트랜잭션/영속화 검증 ──────────────────────────────────────────────────
    // flush + clear 로 영속성 컨텍스트를 비운 뒤 DB 에서 다시 읽어, 변경이 실제로
    // DB 에 반영(영속화)되는지 확인한다. (특히 취소/환불은 save() 없이 dirty checking 으로 반영)
    @Nested
    @DisplayName("트랜잭션/영속화")
    class Persistence {

        @Test
        @DisplayName("결제 생성이 DB 에 실제로 저장된다 (flush/clear 후 재조회)")
        void createIsPersisted() {
            Order order = persistOrder(CUSTOMER_ID, 27000);
            PaymentCreateResponse created = paymentService.createPayment(
                    new PaymentCreateRequest(order.getId(), 27000L, PaymentMethod.CARD),
                    CUSTOMER_ID, null);

            em.flush();
            em.clear(); // 영속성 컨텍스트 비움 → 이후 조회는 DB 에서 새로 읽음

            Payment reloaded = paymentRepository.findById(created.paymentId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(reloaded.getAmount()).isEqualTo(27000L);
            assertThat(reloaded.getOrder().getId()).isEqualTo(order.getId());
        }

        @Test
        @DisplayName("취소가 dirty checking 으로 DB 에 반영된다 (save 호출 없음)")
        void cancelIsPersisted() {
            PaymentCreateResponse created = createPaidPayment(CUSTOMER_ID, 27000);
            em.flush();
            em.clear();

            paymentService.cancelPayment(created.paymentId(), new PaymentCancelRequest("사장님 미수락"), owner());

            em.flush();
            em.clear();

            Payment reloaded = paymentRepository.findById(created.paymentId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.CANCELED);
            assertThat(reloaded.getCanceledReason()).isEqualTo("사장님 미수락");
            assertThat(reloaded.getCanceledAt()).isNotNull();
        }

        @Test
        @DisplayName("환불이 dirty checking 으로 DB 에 반영된다 (save 호출 없음)")
        void refundIsPersisted() {
            PaymentCreateResponse created = createPaidPayment(CUSTOMER_ID, 27000);
            em.flush();
            em.clear();

            paymentService.refundPayment(created.paymentId(), new PaymentRefundRequest("단순 변심"), customer(CUSTOMER_ID));

            em.flush();
            em.clear();

            Payment reloaded = paymentRepository.findById(created.paymentId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(reloaded.getRefundedReason()).isEqualTo("단순 변심");
            assertThat(reloaded.getRefundedAt()).isNotNull();
        }

        @Test
        @DisplayName("결제 생성 실패(금액 불일치) 시 결제가 DB 에 남지 않는다")
        void failedCreateLeavesNoRow() {
            Order order = persistOrder(CUSTOMER_ID, 27000);

            assertThatThrownBy(() -> paymentService.createPayment(
                    new PaymentCreateRequest(order.getId(), 30000L, PaymentMethod.CARD), CUSTOMER_ID, null))
                    .isInstanceOf(CustomException.class);

            em.flush();
            em.clear();

            // 검증 단계에서 예외 → save 미실행 → 주문에 결제 없음
            assertThat(paymentRepository.existsByOrder_Id(order.getId())).isFalse();
        }
    }
}
