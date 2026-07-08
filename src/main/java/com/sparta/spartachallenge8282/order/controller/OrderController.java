package com.sparta.spartachallenge8282.order.controller;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.order.dto.request.OrderCreateRequestDto;
import com.sparta.spartachallenge8282.order.dto.response.OrderCreateResponseDto;
import com.sparta.spartachallenge8282.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}