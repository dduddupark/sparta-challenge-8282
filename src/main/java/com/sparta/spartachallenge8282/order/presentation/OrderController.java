package com.sparta.spartachallenge8282.order.presentation;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.order.presentation.dto.request.OrderCreateRequestDto;
import com.sparta.spartachallenge8282.order.presentation.dto.request.OrderStatusUpdateRequestDto;
import com.sparta.spartachallenge8282.order.presentation.dto.response.*;
import com.sparta.spartachallenge8282.order.application.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 주문 API Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {


    private final OrderService orderService;

    // 강한 결합이 아닌 msa로 변환할 때 확장성을 고려한 코드
//    @PostMapping
//    public ResponseEntity<ApiResponse<OrderCreateResponseDto>> createOrder(
//            @AuthenticationPrincipal UserDetailsImpl userDetails,
//            @Valid @RequestBody OrderCreateRequestDto request
//    ) {
//        OrderCreateResponseDto response = orderService.createOrder(
//                userDetails.userId(),
//                request
//        );
//
//        return ResponseEntity.ok(
//                ApiResponse.success("주문 생성 성공", response)
//        );
//    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderCreateResponseDto>> createOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,

            // 중복 결제 방지용 키
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            ) String idempotencyKey,

            @Valid @RequestBody OrderCreateRequestDto request
    ) {
        OrderCreateResponseDto response = orderService.createOrder(
                userDetails.userId(),
                request,
                idempotencyKey
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "주문 및 결제 생성 성공",
                        response
                )
        );
    }

    /*
     * 주문 단건 조회
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> getOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID orderId
    ) {
        OrderDetailResponseDto response = orderService.getOrder(
                userDetails.userId(),
                orderId
        );

        return ResponseEntity.ok(
                ApiResponse.success("주문 단건 조회 성공", response)
        );
    }

    /*
     * 주문 상품 목록 조회
     */
    @GetMapping("/{orderId}/items")
    public ResponseEntity<ApiResponse<List<OrderItemResponseDto>>> getOrderItems(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID orderId
    ) {
        List<OrderItemResponseDto> response = orderService.getOrderItems(
                userDetails.userId(),
                orderId
        );

        return ResponseEntity.ok(
                ApiResponse.success("주문 상품 목록 조회 성공", response)
        );
    }

    // 주문 목록 조회 + 페이징
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderListResponseDto>>> getOrders(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            Pageable pageable
    ) {
        PageResponse<OrderListResponseDto> response = orderService.getOrders(
                userDetails.userId(),
                pageable
        );

        return ResponseEntity.ok(
                ApiResponse.success("주문 목록 조회 성공", response)
        );
    }


    // 주문 취소
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> cancelOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID orderId
    ) {
        OrderDetailResponseDto response = orderService.cancelOrder(
                userDetails.userId(),
                orderId
        );

        return ResponseEntity.ok(
                ApiResponse.success("주문 취소 성공", response)
        );
    }

    // 주문 상태 변경
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderStatusUpdateResponseDto>> updateOrderStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequestDto request
    ) {
        OrderStatusUpdateResponseDto response = orderService.updateOrderStatus(
                userDetails.userId(),
                userDetails.role(),
                orderId,
                request.orderStatus(),
                request.reason()
        );

        return ResponseEntity.ok(
                ApiResponse.success("주문 상태 변경 성공", response)
        );
    }

    /**
     * 주문 상태 변경 이력 조회
     */
    @GetMapping("/{orderId}/status-history")
    public ResponseEntity<
            ApiResponse<List<OrderStatusHistoryResponseDto>>
            > getOrderStatusHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID orderId
    ) {
        List<OrderStatusHistoryResponseDto> response =
                orderService.getOrderStatusHistory(
                        userDetails.userId(),
                        userDetails.role(),
                        orderId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "주문 상태 변경 이력 조회 성공",
                        response
                )
        );
    }
}