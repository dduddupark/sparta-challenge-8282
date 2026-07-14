package com.sparta.spartachallenge8282.order.service;

import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.order.dto.response.*;
import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.entity.OrderStatusHistory;
import com.sparta.spartachallenge8282.order.enums.OrderStatus;
import com.sparta.spartachallenge8282.order.repository.OrderRepository;
import com.sparta.spartachallenge8282.order.dto.request.OrderCreateRequestDto;
import com.sparta.spartachallenge8282.order.repository.OrderStatusHistoryRepository;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

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

    //주문 취소
    /*
    * todo: 검증 고민 사항들
    * 1. 가게가 5분이 넘도록 주문을 확인하지 않을 때? -> 자동 취소?
    * 2. 주문 취소되면서 payment 환불 처리
    * 3. 가능성이 낮지만, 고객과 가게가 동시에 취소하는 경우..? -> lock?
     */
    @Transactional
    public OrderDetailResponseDto cancelOrder(
            Long userId,
            UUID orderId
    ) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        validateOrderOwner(order, userId);
        validateCancelable(order);
        validateCancelTimeLimit(order);

        order.cancel();

        return OrderDetailResponseDto.from(order);
    }

    /*
     * 주문 취소 가능 상태 검증
     * 현재는 PENDING 상태에서만 고객 취소 허용
     */
    private void validateCancelable(Order order) {
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }

    //주문 생성 후 5분 이내 취소 가능 검증
    private void validateCancelTimeLimit(Order order) {
        LocalDateTime cancelDeadline = order.getCreatedAt().plusMinutes(5);

        if (LocalDateTime.now().isAfter(cancelDeadline)) {
            throw new CustomException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }
    }

    //주문 상태 변경 메서드
    @Transactional
    public OrderStatusUpdateResponseDto updateOrderStatus(
            Long userId,
            String userRole,
            UUID orderId,
            OrderStatus nextStatus,
            String reason
    ) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        validateStatusUpdatePermission(order, userId, userRole);
        validateStatusTransition(order.getOrderStatus(), nextStatus);

        OrderStatus previousStatus = order.getOrderStatus();

        order.changeStatus(nextStatus);

        OrderStatusHistory history = OrderStatusHistory.create(
                order,
                previousStatus,
                nextStatus,
                userId,
                userRole,
                reason
        );

        orderStatusHistoryRepository.save(history);

        return OrderStatusUpdateResponseDto.from(order);
    }

    // 권한 문자열을 UserRole Enum으로 변환 메서드
    private UserRole parseUserRole(String authority) {
        if (authority == null || !authority.startsWith("ROLE_")) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        String roleName = authority.substring("ROLE_".length());

        try {
            return UserRole.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    // Enum으로 변환하여 권한 검증 메서드
    private void validateStatusUpdatePermission(
            Order order,
            Long userId,
            String authority
    ) {
        UserRole userRole = parseUserRole(authority);

        if (userRole != UserRole.OWNER
                && userRole != UserRole.MANAGER
                && userRole != UserRole.MASTER) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        // TODO: Store 도메인 연동 후 OWNER가 본인 가게 주문만 변경 가능하도록 검증
    }

    //상태 전이 검증 메서드
    private void validateStatusTransition(
            OrderStatus currentStatus,
            OrderStatus nextStatus
    ) {
        if (currentStatus == nextStatus) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        boolean isValid = switch (currentStatus) {
            case PENDING -> nextStatus == OrderStatus.ACCEPTED
                    || nextStatus == OrderStatus.CANCELED;

            case ACCEPTED -> nextStatus == OrderStatus.COOKING
                    || nextStatus == OrderStatus.CANCELED;

            case COOKING -> nextStatus == OrderStatus.DELIVERING
                    || nextStatus == OrderStatus.CANCELED;

            case DELIVERING -> nextStatus == OrderStatus.COMPLETED;

            case COMPLETED, CANCELED -> false;
        };

        if (!isValid) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }
}