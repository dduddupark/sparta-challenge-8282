package com.sparta.spartachallenge8282.order.service;

import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;
import com.sparta.spartachallenge8282.order.dto.request.OrderItemRequestDto;
import com.sparta.spartachallenge8282.order.dto.response.*;
import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.entity.OrderItem;
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
import com.sparta.spartachallenge8282.payment.application.PaymentService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    //store별 배달비 정책 결정 전까지 사용하는 임시 배달비
    private static final int DEFAULT_DELIVERY_FEE = 3000;
    //쿠폰/프로모션 기능 연동 전 임시 할인 금액
    private static final int DEFAULT_DISCOUNT_AMOUNT = 0;

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    /**
     * 주문 취소 시 연결된 결제의 상태를 변경하기 위한 서비스
     * OrderService가 PaymentRepository나 Payment 엔티티를 직접 다루지 않고,
     * Payment 도메인에 결제 환불/취소 요쳥
     */
    private final PaymentService paymentService;

    /*주문 생성 시 실제 메뉴 정보 조회
     * MenuRepository의 역할:
     * - 메뉴 존재 여부 확인
     * - 메뉴 가격, 이름, 가게, 판매 상태 제공*/
    private final MenuRepository menuRepository;

    /*
    주문 생성 흐름 생성(메서드 이용한 함수 호출)
    orderitem생성 -> 메뉴 총액 계산 -> 주문 생성 -> 연관관계 -> 저장
     */
    public OrderCreateResponseDto createOrder(
            Long userId,
            OrderCreateRequestDto request
    ) {
        // 메뉴 검증 및 dto 변환
        // 리스트를 이용하여 여러 메뉴 조회
        List<OrderItem> orderItems = createOrderItems(request);

        // 금액 계산.
        int menuTotalPrice =
                calculateMenuTotalPrice(orderItems);

        // 위 두 정보를 이용해 주문 생성
        Order order = createOrderEntity(
                userId,
                request,
                menuTotalPrice
        );

        // 양방향 연관관계 설정
        orderItems.forEach(order::addOrderItem);

        Order savedOrder = orderRepository.save(order);

        return OrderCreateResponseDto.from(savedOrder);
    }

    /*
    요청 DTO의 상품 목록을 실제 OrderItem 목록으로 변환
     */
    private List<OrderItem> createOrderItems(
            OrderCreateRequestDto request
    ) {
        return request.orderItems()
                .stream()
                .map(itemRequest ->
                        createOrderItem(
                                request.storeId(),
                                itemRequest
                        )
                )
                .toList();
    }
    // 주문 당시 메뉴 정보를 orderitem에 주입
    private OrderItem createOrderItem(
            UUID requestStoreId,
            OrderItemRequestDto itemRequest
    ) {
        Menu menu = findOrderableMenu(
                itemRequest.menuId(),
                requestStoreId
        );

        return new OrderItem(
                menu.getId(),
                menu.getName(),
                menu.getPrice(),
                itemRequest.quantity()
        );
    }

    //삭제 되지 않은 주문인지 검증
    private Menu findOrderableMenu(
            UUID menuId,
            UUID requestStoreId
    ) {
        Menu menu = menuRepository.findByIdAndDeletedAtIsNull(menuId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.MENU_NOT_FOUND)
                );

        validateMenuStore(menu, requestStoreId);
        validateMenuVisibility(menu);
        validateMenuStatus(menu);

        return menu;
    }
    // 메뉴가 요청하는 가게가 맞는지 판단
    private void validateMenuStore(
            Menu menu,
            UUID requestStoreId
    ) {
        if (!menu.getStoreId().equals(requestStoreId)) {
            throw new CustomException(
                    ErrorCode.MENU_STORE_MISMATCH
            );
        }
    }
    // 숨김 처리 된 메뉴인지 판단
    private void validateMenuVisibility(Menu menu) {
        if (menu.isHidden()) {
            throw new CustomException(
                    ErrorCode.HIDDEN_MENU_NOT_ORDERABLE
            );
        }
    }
    // 현재 판매 중인 메뉴인지 판단
    private void validateMenuStatus(Menu menu) {
        if (menu.getStatus() != MenuStatus.ON_SALE) {
            throw new CustomException(
                    ErrorCode.MENU_NOT_ORDERABLE
            );
        }
    }

    // 메뉴 총 금액 계산
    private int calculateMenuTotalPrice(
            List<OrderItem> orderItems
    ) {
        return orderItems.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();
    }

    // 주문 정보와 서버에서 계산한 금액으로 최종 주문 생성
    private Order createOrderEntity(
            Long userId,
            OrderCreateRequestDto request,
            int menuTotalPrice
    ) {
        return Order.create(
                createOrderNumber(),
                userId,
                request.storeId(),
                menuTotalPrice,
                DEFAULT_DISCOUNT_AMOUNT,
                DEFAULT_DELIVERY_FEE,
                request.deliveryAddress(),
                request.deliveryDetailAddress(),
                request.requestMessage()
        );
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
        //삭제 되지 않은 주문 조회
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        // 로그인한 본인 주문인지 확인
        validateOrderOwner(order, userId);
        // 고객 취소는 PENDING 상태에서만 허용
        validateCancelable(order);
        // 주문 생성 후 5분 이내만 취소 가능
        validateCancelTimeLimit(order);

        // PAYMENT가 존재하면 : PAID >  REFUNDED
        // PAYMENT가 없으면 : 아무 작업 없이 종료
        paymentService.refundByOrder(
                order.getId(),
                "고객 주문 취소"
        );

        // PENDING -> CANCELED
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
        /// 1.주문 조회
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        /// 2. 주문 상태 변경 권한 검증
        validateStatusUpdatePermission(order, userId, userRole);
        /// 3. 상태 변경 가능한지 검증
        validateStatusTransition(order.getOrderStatus(), nextStatus);

        /// 4. 이력 저장을 위한 변경 전 상태 보관
        OrderStatus previousStatus = order.getOrderStatus();

        /// 5. 취소를 요청했다면 PAYMENT 취소 처리
        /// * 그 외에 요청(ACCEPTED, COOKING, DELIVERING, COMPLETED)은 PAYMENT 상태를 변경하지 않는다.
        processPaymentByOrderStatus(
                order.getId(),
                nextStatus,
                reason
        );

        /// 6. 실제 주문 상태 변경
        order.changeStatus(nextStatus);

        /// 7. 상태 변경 이력 생성
        OrderStatusHistory history = OrderStatusHistory.create(
                order,
                previousStatus,
                nextStatus,
                userId,
                userRole,
                reason
        );

        /// 8. 상태 변경 이력 저장
        orderStatusHistoryRepository.save(history);

        return OrderStatusUpdateResponseDto.from(order);
    }

    /**
     * 주문 상태 변경에 따라 Payment 상태 변경이 필요한 경우 처리
     * 가게 소유주가 변경
     */
    private void processPaymentByOrderStatus(
            UUID orderId,
            OrderStatus nextStatus,
            String reason
    ) {
        // 취소가 아닌 상태 변경은 Payment에 영향을 주지 않는다.
        if (nextStatus != OrderStatus.CANCELED) {
            return;
        }

        String cancelReason =
                reason == null || reason.isBlank()
                        ? "가게 주문 취소"
                        : reason;

        paymentService.cancelByOrder(
                orderId,
                cancelReason
        );
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