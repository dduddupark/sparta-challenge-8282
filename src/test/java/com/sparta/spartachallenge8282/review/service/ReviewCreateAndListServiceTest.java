package com.sparta.spartachallenge8282.review.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.enums.OrderStatus;
import com.sparta.spartachallenge8282.order.repository.OrderRepository;
import com.sparta.spartachallenge8282.review.application.ReviewService;
import com.sparta.spartachallenge8282.review.domain.Review;
import com.sparta.spartachallenge8282.review.domain.ReviewRepository;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewCreateRequestDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewResultResponseDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewSliceResponseDto;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.user.domain.User;
import com.sparta.spartachallenge8282.user.domain.UserRepository;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewCreateAndListServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReviewTest() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID fakeReviewId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto =
                new ReviewCreateRequestDto(
                        orderId,
                        5,
                        "정말 맛있었어요!",
                        null
                );

        Order order = Order.create(
                "ORD-0001",
                userId,
                storeId,
                10000,
                0,
                3000,
                "서울시 종로구",
                "3층",
                "문 앞에 놔주세요"
        );

        ReflectionTestUtils.setField(
                order,
                "orderStatus",
                OrderStatus.COMPLETED
        );

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(reviewRepository.existsByOrderId(orderId))
                .thenReturn(false);

        Review savedReview = Review.builder()
                .requestDto(requestDto)
                .userId(userId)
                .storeId(storeId)
                .build();

        ReflectionTestUtils.setField(
                savedReview,
                "id",
                fakeReviewId
        );

        when(reviewRepository.save(any(Review.class)))
                .thenReturn(savedReview);

        /*
         * 리뷰 저장 후 가게 리뷰 집계 갱신에 필요한 설정
         */
        Store store = mock(Store.class);

        when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                .thenReturn(Optional.of(store));

        when(reviewRepository.calculateAverageRating(storeId))
                .thenReturn(5.0);

        when(reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId))
                .thenReturn(1L);

        // when
        ReviewResultResponseDto result =
                reviewService.createReview(requestDto, userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.reviewId()).isEqualTo(fakeReviewId);

        verify(reviewRepository)
                .save(any(Review.class));

        verify(storeRepository)
                .findByIdAndDeletedAtIsNull(storeId);

        verify(store).updateReviewSummary(
                new BigDecimal("5.0"),
                1
        );
    }

    @Test
    @DisplayName("리뷰 생성 실패: 주문을 찾을 수 없음")
    void createReviewTest_fail_not_order() {
        // given
        UUID orderId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto =
                new ReviewCreateRequestDto(
                        orderId,
                        5,
                        "정말 맛있었어요!",
                        null
                );

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                () -> reviewService.createReview(requestDto, userId)
        )
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

        ReviewCreateRequestDto requestDto =
                new ReviewCreateRequestDto(
                        orderId,
                        5,
                        "정말 맛있었어요!",
                        null
                );

        Order order = Order.create(
                "ORD-0002",
                ownerId,
                storeId,
                10000,
                0,
                3000,
                "서울시 종로구",
                "3층",
                null
        );

        ReflectionTestUtils.setField(
                order,
                "orderStatus",
                OrderStatus.COMPLETED
        );

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(
                () -> reviewService.createReview(
                        requestDto,
                        otherUserId
                )
        )
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("리뷰 생성 실패: 배달완료 상태가 아님")
    void createReviewTest_fail_not_complete() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto =
                new ReviewCreateRequestDto(
                        orderId,
                        5,
                        "정말 맛있었어요!",
                        null
                );

        Order order = Order.create(
                "ORD-0003",
                userId,
                storeId,
                10000,
                0,
                3000,
                "서울시 종로구",
                "3층",
                null
        );

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(
                () -> reviewService.createReview(requestDto, userId)
        )
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("리뷰 생성 실패: 이미 리뷰가 존재함")
    void createReviewTest_fail_exist() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto =
                new ReviewCreateRequestDto(
                        orderId,
                        5,
                        "정말 맛있었어요!",
                        null
                );

        Order order = Order.create(
                "ORD-0004",
                userId,
                storeId,
                10000,
                0,
                3000,
                "서울시 종로구",
                "3층",
                null
        );

        ReflectionTestUtils.setField(
                order,
                "orderStatus",
                OrderStatus.COMPLETED
        );

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(reviewRepository.existsByOrderId(orderId))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(
                () -> reviewService.createReview(requestDto, userId)
        )
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("가게 리뷰 목록 조회 성공")
    void getReviewsByStoreTest() {
        // given
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        Pageable pageable = PageRequest.of(0, 10);

        Review review = Review.builder()
                .requestDto(
                        new ReviewCreateRequestDto(
                                UUID.randomUUID(),
                                5,
                                "맛있어요",
                                null
                        )
                )
                .userId(userId)
                .storeId(storeId)
                .build();

        Slice<Review> slice =
                new SliceImpl<>(
                        List.of(review),
                        pageable,
                        false
                );

        User user = User.builder()
                .email("test@test.com")
                .password("encoded-pw")
                .nickname("맛집탐험가")
                .address("서울시 종로구")
                .role(UserRole.CUSTOMER)
                .build();

        ReflectionTestUtils.setField(
                user,
                "id",
                userId
        );

        when(storeRepository.existsById(storeId))
                .thenReturn(true);

        when(
                reviewRepository.findByStoreIdAndDeletedAtIsNull(
                        storeId,
                        pageable
                )
        ).thenReturn(slice);

        when(userRepository.findAllById(List.of(userId)))
                .thenReturn(List.of(user));

        // when
        ReviewSliceResponseDto result =
                reviewService.getReviewsByStore(
                        storeId,
                        pageable
                );

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("가게 리뷰 목록 조회 실패: 가게 없음")
    void getReviewsByStoreTest_fail_store_not_found() {
        // given
        UUID storeId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(storeRepository.existsById(storeId))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(
                () -> reviewService.getReviewsByStore(
                        storeId,
                        pageable
                )
        )
                .isInstanceOf(CustomException.class);
    }
}