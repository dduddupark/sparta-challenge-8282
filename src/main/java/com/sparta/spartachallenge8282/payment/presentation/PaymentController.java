package com.sparta.spartachallenge8282.payment.presentation;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.payment.presentation.dto.request.PaymentCancelRequest;
import com.sparta.spartachallenge8282.payment.presentation.dto.request.PaymentCreateRequest;
import com.sparta.spartachallenge8282.payment.presentation.dto.request.PaymentRefundRequest;
import com.sparta.spartachallenge8282.payment.presentation.dto.response.PaymentCancelResponse;
import com.sparta.spartachallenge8282.payment.presentation.dto.response.PaymentCreateResponse;
import com.sparta.spartachallenge8282.payment.presentation.dto.response.PaymentRefundResponse;
import com.sparta.spartachallenge8282.payment.presentation.dto.response.PaymentResponse;
import com.sparta.spartachallenge8282.payment.domain.PaymentStatus;
import com.sparta.spartachallenge8282.payment.application.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 결제(Payment) API 컨트롤러.
 *
 * <p>명세: {@code docs/api/api-spec.md} 10장. 에러 코드는 Payment 도메인(60000번대).
 * 응답은 {@link ApiResponse} envelope, 목록은 {@link PageResponse} 로 통일한다.
 *
 * <p><b>권한 표기</b>: 인증 주체의 authority 는 {@code UserRole.getAuthority()} 결과인
 * {@code ROLE_} 접두사 형태다({@code ROLE_CUSTOMER} 등, {@code JwtAuthFilter} 참고).
 * 따라서 {@code hasAnyAuthority} 에도 접두사를 포함한 {@code 'ROLE_CUSTOMER'} 형태로 검증한다
 * (프로젝트 다른 컨트롤러와 동일 컨벤션).
 * 본인/본인 가게 소유 여부 등 데이터 기반 세부 권한은 서비스 계층에서 검증한다.
 */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ── 10.1 결제 생성 ──────────────────────────────────────────────────────────
    // 결제 생성은 주문 소유자(CUSTOMER)의 self-service 액션이다. 서비스에서 order.userId == 요청자
    // 소유자 검증을 강제하므로, 남의 주문을 결제할 수 없는 CUSTOMER 로 롤을 한정한다.
    @PostMapping("/api/v1/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ApiResponse<PaymentCreateResponse> createPayment(
            @Valid @RequestBody PaymentCreateRequest request,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        // Idempotency-Key 는 필수. 헤더 누락은 MissingRequestHeaderException(400) 으로,
        // 값이 공백이면 여기서 400 으로 거부한다(공백 키는 멱등 보장을 무력화하므로).
        if (idempotencyKey.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }
        PaymentCreateResponse data =
                paymentService.createPayment(request, user.userId(), idempotencyKey);
        return ApiResponse.success("결제가 완료되었습니다.", data);
    }

    // ── 10.2 주문의 결제 내역 조회 ───────────────────────────────────────────────
    @GetMapping("/api/v1/orders/{orderId}/payment")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_OWNER', 'ROLE_MANAGER', 'ROLE_MASTER')")
    public ApiResponse<PaymentResponse> getPaymentByOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiResponse.success("결제 내역 조회 성공", paymentService.getPaymentByOrder(orderId, user));
    }

    // ── 10.3 결제 단건 조회 ─────────────────────────────────────────────────────
    @GetMapping("/api/v1/payments/{paymentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_OWNER', 'ROLE_MANAGER', 'ROLE_MASTER')")
    public ApiResponse<PaymentResponse> getPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiResponse.success("결제 조회 성공", paymentService.getPayment(paymentId, user));
    }

    // ── 10.4 결제 전체 목록 조회(관리자) ─────────────────────────────────────────
    @GetMapping("/api/v1/payments")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_MASTER')")
    public ApiResponse<PageResponse<PaymentResponse>> getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentResponse> page = paymentService.getPayments(status, pageable);
        return ApiResponse.success("결제 목록 조회 성공", PageResponse.from(page));
    }

    // ── 10.5 결제 취소 ──────────────────────────────────────────────────────────
    @PatchMapping("/api/v1/payments/{paymentId}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_MANAGER', 'ROLE_MASTER')")
    public ApiResponse<PaymentCancelResponse> cancelPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentCancelRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiResponse.success("결제가 취소되었습니다.", paymentService.cancelPayment(paymentId, request, user));
    }

    // ── 10.6 결제 환불 ──────────────────────────────────────────────────────────
    @PatchMapping("/api/v1/payments/{paymentId}/refund")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_MANAGER', 'ROLE_MASTER')")
    public ApiResponse<PaymentRefundResponse> refundPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentRefundRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiResponse.success("결제가 환불되었습니다.", paymentService.refundPayment(paymentId, request, user));
    }

    // ── 10.7 특정 유저 결제 내역 조회(관리자) ────────────────────────────────────
    @GetMapping("/api/v1/users/{userId}/payments")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_MASTER')")
    public ApiResponse<PageResponse<PaymentResponse>> getUserPayments(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentResponse> page = paymentService.getUserPayments(userId, pageable);
        return ApiResponse.success("유저 결제 내역 조회 성공", PageResponse.from(page));
    }

    // ── 10.8 고객 본인 결제 목록 조회 ────────────────────────────────────────────
    @GetMapping("/api/v1/payments/me")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_MANAGER', 'ROLE_MASTER')")
    public ApiResponse<PageResponse<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentResponse> page = paymentService.getMyPayments(user, pageable);
        return ApiResponse.success("내 결제 목록 조회 성공", PageResponse.from(page));
    }
}
