package com.sparta.spartachallenge8282.review.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.enums.OrderStatus;
import com.sparta.spartachallenge8282.order.repository.OrderRepository;
import com.sparta.spartachallenge8282.review.dto.request.ReviewCreateRequestDto;
import com.sparta.spartachallenge8282.review.dto.response.ReviewResultResponseDto;
import com.sparta.spartachallenge8282.review.entity.Review;
import com.sparta.spartachallenge8282.review.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)   // Mockito 사용 선언
class ReviewServiceTest {

    @Mock   // 가짜 Repository (실제 DB 연결 안 함)
    private ReviewRepository reviewRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks   // 위의 Mock들을 자동으로 주입받는 진짜 테스트 대상
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReviewTest() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(
                orderId, 5, "정말 맛있었어요!", null
        );

        Order order = Order.create(
                "ORD-0001", userId, storeId,
                10000, 0, 3000,
                "서울시 종로구", "3층", "문 앞에 놔주세요"
        );

        ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.COMPLETED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByOrderId(orderId)).thenReturn(false);

        Review savedReview = Review.builder()
                .requestDto(requestDto)
                .userId(userId)
                .storeId(storeId)
                .build();

        // 테스트용으로 강제로 id 채워넣기 (실제 DB 저장 시뮬레이션)
        UUID fakeReviewId = UUID.randomUUID();
        ReflectionTestUtils.setField(savedReview, "id", fakeReviewId);

        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // when
        ReviewResultResponseDto result = reviewService.createReview(requestDto, userId);

        System.out.println("결과: " + result);

        // then
        assertThat(result).isNotNull();
        assertThat(result.reviewId()).isEqualTo(fakeReviewId);   // savedReview.getId() 대신 fakeReviewId로 비교
    }
    @Test
    @DisplayName("리뷰 생성 실패: 주문을 찾을 수 없음")
    void createReviewTest_fail_not_order() {
        // given
        UUID orderId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(
                orderId, 5, "정말 맛있었어요!", null
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(requestDto, userId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("리뷰 생성 실패: 본인 주문이 아님")
    void createReviewTest_fail_not_user() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;
        Long otherUserId = 999L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(
                orderId, 5, "정말 맛있었어요!", null
        );

        Order order = Order.create(
                "ORD-0002", ownerId, storeId,
                10000, 0, 3000,
                "서울시 종로구", "3층", null
        );
        ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.COMPLETED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // when & then - otherUserId가 리뷰 작성 시도
        assertThatThrownBy(() -> reviewService.createReview(requestDto, otherUserId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("리뷰 생성 실패: 배달완료 상태가 아님")
    void createReviewTest_fail_not_complete() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(
                orderId, 5, "정말 맛있었어요!", null
        );

        // Order 상태를 PENDING 그대로 둠 (COMPLETED 아님)
        Order order = Order.create(
                "ORD-0003", userId, storeId,
                10000, 0, 3000,
                "서울시 종로구", "3층", null
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(requestDto, userId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("리뷰 생성 실패: 이미 리뷰가 존재함")
    void createReviewTest_fail_exist() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(
                orderId, 5, "정말 맛있었어요!", null
        );

        Order order = Order.create(
                "ORD-0004", userId, storeId,
                10000, 0, 3000,
                "서울시 종로구", "3층", null
        );
        ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.COMPLETED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByOrderId(orderId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(requestDto, userId))
                .isInstanceOf(CustomException.class);
    }
}
