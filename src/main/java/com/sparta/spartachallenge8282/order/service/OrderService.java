package com.sparta.spartachallenge8282.order.service;

import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.order.dto.response.OrderDetailResponseDto;
import com.sparta.spartachallenge8282.order.dto.response.OrderItemResponseDto;
import com.sparta.spartachallenge8282.order.dto.response.OrderListResponseDto;
import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.repository.OrderRepository;
import com.sparta.spartachallenge8282.order.dto.request.OrderCreateRequestDto;
import com.sparta.spartachallenge8282.order.dto.response.OrderCreateResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final int DEFAULT_DELIVERY_FEE = 3000;
    private static final int DEFAULT_DISCOUNT_AMOUNT = 0;

    private final OrderRepository orderRepository;

    /*
     * 주문 생성
     * 현재 단계에서 메뉴 금액은 임시로 0원 처리
     */
    public OrderCreateResponseDto createOrder(
            Long userId,
            OrderCreateRequestDto request
    ) {
        int menuTotalPrice = calculateMenuTotalPrice(request);
        int discountAmount = DEFAULT_DISCOUNT_AMOUNT;
        int deliveryFee = DEFAULT_DELIVERY_FEE;

        Order order = Order.create(
                createOrderNumber(),
                userId,
                request.storeId(),
                menuTotalPrice,
                discountAmount,
                deliveryFee,
                request.deliveryAddress(),
                request.deliveryDetailAddress(),
                request.requestMessage()
        );

        Order savedOrder = orderRepository.save(order);

        return OrderCreateResponseDto.from(savedOrder);
    }

    /*
     * 메뉴 총 금액 계산
     * 지금은 MenuRepository가 없으므로 임시값.
     * 나중에는 menuId로 메뉴를 조회해서 가격 * 수량으로 계산해야 한다.
     */
    private int calculateMenuTotalPrice(OrderCreateRequestDto request) {
        return 0;
    }

    /*
     * 주문번호 생성
     * 예시 형식: A240703001
     * 현재는 동시성 고려 없이 날짜 기반 임시 번호로 만든다.
     * 실무에서는 별도 채번 정책이나 시퀀스가 필요하다.
     */
    private String createOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        long randomNumber = System.currentTimeMillis() % 1000;

        return "A" + date + String.format("%03d", randomNumber);
    }

    /*
     * 주문 단건 조회
     * 현재 단계에서는 로그인 연동 전이므로 Controller에서 전달한 임시 userId를 기준으로
     * CUSTOMER가 본인의 주문만 조회한다고 가정
     */
    @Transactional(readOnly = true)
    public OrderDetailResponseDto getOrder(
            Long userId,
            UUID orderId
    ) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        validateOrderOwner(order, userId);

        return OrderDetailResponseDto.from(order);
    }

    /*
     * 주문 접근 권한 검증
     * 현재는 CUSTOMER 기준만 검증
     * 추후 로그인/권한 연동 후 CUSTOMER, OWNER, MANAGER 권한별 검증으로 확장
     */
    private void validateOrderOwner(Order order, Long userId) {
        if (!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    // 주문 상품 목록 조회
    @Transactional(readOnly = true)
    public List<OrderItemResponseDto> getOrderItems(
            Long userId,
            UUID orderId
    ) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        validateOrderOwner(order, userId);

        return order.getOrderItems()
                .stream()
                .map(OrderItemResponseDto::from)
                .toList();
    }

    // 주문 목록 조회 + 페이징
    @Transactional(readOnly = true)
    public PageResponse<OrderListResponseDto> getOrders(
            Long userId,
            Pageable pageable
    ) {
        Page<OrderListResponseDto> orders = orderRepository
                .findAllByUserIdAndDeletedAtIsNull(userId, pageable)
                .map(OrderListResponseDto::from);

        return PageResponse.from(orders);
    }
}