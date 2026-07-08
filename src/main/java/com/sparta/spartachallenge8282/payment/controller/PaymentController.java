package com.sparta.spartachallenge8282.payment.controller;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.payment.dto.request.PaymentCancelRequest;
import com.sparta.spartachallenge8282.payment.dto.request.PaymentCreateRequest;
import com.sparta.spartachallenge8282.payment.dto.request.PaymentRefundRequest;
import com.sparta.spartachallenge8282.payment.dto.response.PaymentCancelResponse;
import com.sparta.spartachallenge8282.payment.dto.response.PaymentCreateResponse;
import com.sparta.spartachallenge8282.payment.dto.response.PaymentRefundResponse;
import com.sparta.spartachallenge8282.payment.dto.response.PaymentResponse;
import com.sparta.spartachallenge8282.payment.entity.PaymentStatus;
import com.sparta.spartachallenge8282.payment.service.PaymentService;
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
 * <p><b>권한 표기</b>: 롤은 {@code UserRole} enum 이름(CUSTOMER/OWNER/MANAGER/MASTER)을 그대로
 * 권한 문자열로 사용한다({@link UserDetailsImpl} 참고). 따라서 {@code hasRole}(=ROLE_ 접두사) 대신
 * {@code hasAnyAuthority} 로 검증한다. 실제 토큰의 롤 클레임 표기가 확정되면 재검토 필요.
 * 본인/본인 가게 소유 여부 등 데이터 기반 세부 권한은 서비스 계층에서 검증한다.
 */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ── 10.1 결제 생성 ──────────────────────────────────────────────────────────
    @PostMapping("/api/v1/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'MASTER')")
    public ApiResponse<PaymentCreateResponse> createPayment(
            @Valid @RequestBody PaymentCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        PaymentCreateResponse data =
                paymentService.createPayment(request, user.getUserId(), idempotencyKey);
        return ApiResponse.success("결제가 완료되었습니다.", data);
    }

    // ── 10.2 주문의 결제 내역 조회 ───────────────────────────────────────────────
    @GetMapping("/api/v1/orders/{orderId}/payment")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    public ApiResponse<PaymentResponse> getPaymentByOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiResponse.success("결제 내역 조회 성공", paymentService.getPaymentByOrder(orderId, user));
    }

    // ── 10.3 결제 단건 조회 ─────────────────────────────────────────────────────
    @GetMapping("/api/v1/payments/{paymentId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'OWNER', 'MANAGER', 'MASTER')")
    public ApiResponse<PaymentResponse> getPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiResponse.success("결제 조회 성공", paymentService.getPayment(paymentId, user));
    }

    // ── 10.4 결제 전체 목록 조회(관리자) ─────────────────────────────────────────
    @GetMapping("/api/v1/payments")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'MASTER')")
    public ApiResponse<PageResponse<PaymentResponse>> getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentResponse> page = paymentService.getPayments(status, pageable);
        return ApiResponse.success("결제 목록 조회 성공", PageResponse.from(page));
    }

    // ── 10.5 결제 취소 ──────────────────────────────────────────────────────────
    @PatchMapping("/api/v1/payments/{paymentId}/cancel")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'MASTER')")
    public ApiResponse<PaymentCancelResponse> cancelPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentCancelRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiResponse.success("결제가 취소되었습니다.", paymentService.cancelPayment(paymentId, request, user));
    }

    // ── 10.6 결제 환불 ──────────────────────────────────────────────────────────
    @PatchMapping("/api/v1/payments/{paymentId}/refund")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'MASTER')")
    public ApiResponse<PaymentRefundResponse> refundPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentRefundRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiResponse.success("결제가 환불되었습니다.", paymentService.refundPayment(paymentId, request, user));
    }

    // ── 10.7 특정 유저 결제 내역 조회(관리자) ────────────────────────────────────
    @GetMapping("/api/v1/users/{userId}/payments")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'MASTER')")
    public ApiResponse<PageResponse<PaymentResponse>> getUserPayments(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentResponse> page = paymentService.getUserPayments(userId, pageable);
        return ApiResponse.success("유저 결제 내역 조회 성공", PageResponse.from(page));
    }

    // ── 10.8 고객 본인 결제 목록 조회 ────────────────────────────────────────────
    @GetMapping("/api/v1/payments/me")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'MASTER')")
    public ApiResponse<PageResponse<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentResponse> page = paymentService.getMyPayments(user, pageable);
        return ApiResponse.success("내 결제 목록 조회 성공", PageResponse.from(page));
    }
}
