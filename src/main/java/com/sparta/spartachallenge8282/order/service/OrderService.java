package com.sparta.spartachallenge8282.order.service;

import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.repository.OrderRepository;
import com.sparta.spartachallenge8282.order.dto.request.OrderCreateRequestDto;
import com.sparta.spartachallenge8282.order.dto.response.OrderCreateResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
}