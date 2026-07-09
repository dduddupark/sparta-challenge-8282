package com.sparta.spartachallenge8282.order.controller;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.order.dto.request.OrderCreateRequestDto;
import com.sparta.spartachallenge8282.order.dto.response.OrderCreateResponseDto;
import com.sparta.spartachallenge8282.order.dto.response.OrderDetailResponseDto;
import com.sparta.spartachallenge8282.order.dto.response.OrderItemResponseDto;
import com.sparta.spartachallenge8282.order.dto.response.OrderListResponseDto;
import com.sparta.spartachallenge8282.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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


    // 추후 user 로그인 기능 구현 이후 아래처럼 코드 변경
    // * - Long userId = userDetails.getUserId();
    private static final Long TEMP_CUSTOMER_ID = 1L;

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderCreateResponseDto>> createOrder(
            @Valid @RequestBody OrderCreateRequestDto request
    ) {
        OrderCreateResponseDto response = orderService.createOrder(
                TEMP_CUSTOMER_ID,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success("주문 생성 성공", response)
        );
    }

    /*
     * 주문 단건 조회
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> getOrder(
            @PathVariable UUID orderId
    ) {
        OrderDetailResponseDto response = orderService.getOrder(
                TEMP_CUSTOMER_ID,
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
            @PathVariable UUID orderId
    ) {
        List<OrderItemResponseDto> response = orderService.getOrderItems(
                TEMP_CUSTOMER_ID,
                orderId
        );

        return ResponseEntity.ok(
                ApiResponse.success("주문 상품 목록 조회 성공", response)
        );
    }

    // 주문 목록 조회 + 페이징
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderListResponseDto>>> getOrders(
            Pageable pageable
    ) {
        PageResponse<OrderListResponseDto> response = orderService.getOrders(
                TEMP_CUSTOMER_ID,
                pageable
        );

        return ResponseEntity.ok(
                ApiResponse.success("주문 목록 조회 성공", response)
        );
    }


    // 주문 취소
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> cancelOrder(
            @PathVariable UUID orderId
    ) {
        OrderDetailResponseDto response = orderService.cancelOrder(
                TEMP_CUSTOMER_ID,
                orderId
        );

        return ResponseEntity.ok(
                ApiResponse.success("주문 취소 성공", response)
        );
    }


}