package com.sparta.spartachallenge8282.review.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.order.domain.Order;
import com.sparta.spartachallenge8282.order.domain.OrderRepository;
import com.sparta.spartachallenge8282.order.domain.OrderStatus;
import com.sparta.spartachallenge8282.review.domain.Review;
import com.sparta.spartachallenge8282.review.domain.ReviewRepository;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewCreateRequestDto;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewUpdateRequestDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewResponseDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewResultResponseDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewSliceResponseDto;
import com.sparta.spartachallenge8282.review_reply.domain.ReviewReply;
import com.sparta.spartachallenge8282.review_reply.domain.ReviewReplyRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewReplyRepository reviewReplyRepository;

    @InjectMocks
    private ReviewService reviewService;

    // 실패 테스트에서 공통으로 예외 정보를 출력하는 헬퍼
    private void printException(Throwable e) {
        CustomException ex = (CustomException) e;

        System.out.println(
                "예외 발생: "
                        + ex.getErrorCode().getCode()
                        + " - "
                        + ex.getErrorCode().getMessage()
        );
    }

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReviewTest() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID fakeReviewId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(
                orderId,
                5,
                "정말 맛있었어요!",
                null
        );

        /*
         * 실제 Order 객체 대신 Mock을 사용해서
         * userId, orderStatus, storeId를 명확하게 설정한다.
         */
        Order order = org.mockito.Mockito.mock(Order.class);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(false);

        when(order.getUserId())
                .thenReturn(userId);

        when(order.getOrderStatus())
                .thenReturn(OrderStatus.COMPLETED);

        when(order.getStoreId())
                .thenReturn(storeId);

        /*
         * save()에 전달되는 Review 객체를 그대로 반환하면서
         * ID만 설정한다.
         */
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> {
                    Review review = invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            review,
                            "id",
                            fakeReviewId
                    );

                    return review;
                });

        Store store = org.mockito.Mockito.mock(Store.class);

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

        verify(reviewRepository).save(any(Review.class));

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

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(
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
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
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
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 생성 실패: 배달완료 상태가 아님")
    void createReviewTest_fail_not_complete() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(
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
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 생성 실패: 이미 리뷰가 존재함")
    void createReviewTest_fail_exist() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(
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

        when(reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(
                () -> reviewService.createReview(requestDto, userId)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
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

        Slice<Review> slice = new SliceImpl<>(
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
        System.out.println("결과: " + result);

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
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 상세 조회 성공")
    void getReviewTest() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        Review review = Review.builder()
                .requestDto(
                        new ReviewCreateRequestDto(
                                UUID.randomUUID(),
                                5,
                                "정말 맛있었어요!",
                                null
                        )
                )
                .userId(userId)
                .storeId(storeId)
                .build();

        ReflectionTestUtils.setField(
                review,
                "id",
                reviewId
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

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                .thenReturn(Optional.of(review));

        when(userRepository.findByIdAndDeletedAtIsNull(userId))
                .thenReturn(Optional.of(user));

        when(reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId))
                .thenReturn(Optional.empty());  // ← 추가

        // when
        ReviewResponseDto result =
                reviewService.getReview(reviewId);

        // then
        System.out.println("결과: " + result);

        assertThat(result).isNotNull();
        assertThat(result.reply()).isNull();   // ← 검증도 추가하면 좋음
    }

    @Test
    @DisplayName("리뷰 상세 조회 실패: 리뷰 없음")
    void getReviewTest_fail_review_not_found() {
        // given
        UUID reviewId = UUID.randomUUID();

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                () -> reviewService.getReview(reviewId)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 상세 조회 실패: 리뷰 작성자가 존재하지 않음")
    void getReviewTest_fail_user_not_found() {
        // given
        UUID reviewId = UUID.randomUUID();
        Long userId = 1L;

        Review review = Review.builder()
                .requestDto(
                        new ReviewCreateRequestDto(
                                UUID.randomUUID(),
                                5,
                                "정말 맛있었어요!",
                                null
                        )
                )
                .userId(userId)
                .storeId(UUID.randomUUID())
                .build();

        ReflectionTestUtils.setField(
                review,
                "id",
                reviewId
        );

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                .thenReturn(Optional.of(review));

        when(userRepository.findByIdAndDeletedAtIsNull(userId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                () -> reviewService.getReview(reviewId)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 삭제 성공: 본인")
    void deleteReviewTest_owner() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        Review review = Review.builder()
                .requestDto(
                        new ReviewCreateRequestDto(
                                UUID.randomUUID(),
                                5,
                                "삭제될 리뷰",
                                null
                        )
                )
                .userId(userId)
                .storeId(storeId)
                .build();

        ReflectionTestUtils.setField(
                review,
                "id",
                reviewId
        );

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                .thenReturn(Optional.of(review));

        /*
         * 리뷰 삭제 후 집계 갱신에 필요한 설정
         */
        Store store = org.mockito.Mockito.mock(Store.class);

        when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                .thenReturn(Optional.of(store));

        when(reviewRepository.calculateAverageRating(storeId))
                .thenReturn(4.0);

        when(reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId))
                .thenReturn(2L);

        // when
        reviewService.deleteReview(reviewId, userId, UserRole.CUSTOMER);

        // then
        System.out.println(
                "삭제 완료: reviewId="
                        + reviewId
                        + ", isDeleted="
                        + review.isDeleted()
        );

        assertThat(review.isDeleted()).isTrue();

        verify(store).updateReviewSummary(
                new BigDecimal("4.0"),
                2
        );
    }

    @Test
    @DisplayName("리뷰 삭제 성공: MANAGER 권한")
    void deleteReviewTest_manager() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        Long ownerId = 1L;
        Long managerId = 999L;

        Review review = Review.builder()
                .requestDto(
                        new ReviewCreateRequestDto(
                                UUID.randomUUID(),
                                5,
                                "삭제될 리뷰",
                                null
                        )
                )
                .userId(ownerId)
                .storeId(storeId)
                .build();

        ReflectionTestUtils.setField(
                review,
                "id",
                reviewId
        );

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                .thenReturn(Optional.of(review));

        /*
         * 리뷰 삭제 후 집계 갱신에 필요한 설정
         */
        Store store = org.mockito.Mockito.mock(Store.class);

        when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                .thenReturn(Optional.of(store));

        when(reviewRepository.calculateAverageRating(storeId))
                .thenReturn(4.0);

        when(reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId))
                .thenReturn(2L);

        // when
        reviewService.deleteReview(reviewId, managerId, UserRole.MANAGER);

        // then
        System.out.println(
                "삭제 완료: reviewId="
                        + reviewId
                        + ", isDeleted="
                        + review.isDeleted()
        );

        assertThat(review.isDeleted()).isTrue();

        verify(store).updateReviewSummary(
                new BigDecimal("4.0"),
                2
        );
    }

    @Test
    @DisplayName("리뷰 삭제 실패: 리뷰 없음")
    void deleteReviewTest_fail_not_found() {
        // given
        UUID reviewId = UUID.randomUUID();

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                () -> reviewService.deleteReview(reviewId, 1L, UserRole.CUSTOMER)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 삭제 실패: 본인도 아니고 권한도 없음")
    void deleteReviewTest_fail_no_permission() {
        // given
        UUID reviewId = UUID.randomUUID();

        Long ownerId = 1L;
        Long otherUserId = 999L;

        Review review = Review.builder()
                .requestDto(
                        new ReviewCreateRequestDto(
                                UUID.randomUUID(),
                                5,
                                "삭제될 리뷰",
                                null
                        )
                )
                .userId(ownerId)
                .storeId(UUID.randomUUID())
                .build();

        ReflectionTestUtils.setField(
                review,
                "id",
                reviewId
        );

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                .thenReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(
                () -> reviewService.deleteReview(reviewId, otherUserId, UserRole.CUSTOMER)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReviewTest() {
        // given
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        Review review = Review.builder()
                .requestDto(
                        new ReviewCreateRequestDto(
                                UUID.randomUUID(),
                                5,
                                "기존 리뷰",
                                null
                        )
                )
                .userId(userId)
                .storeId(storeId)
                .build();

        ReflectionTestUtils.setField(
                review,
                "id",
                reviewId
        );

        ReviewUpdateRequestDto updateDto =
                new ReviewUpdateRequestDto(
                        4,
                        "수정된 리뷰입니다",
                        null
                );

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                .thenReturn(Optional.of(review));

        /*
         * 리뷰 수정 후 집계 갱신에 필요한 설정
         */
        Store store = org.mockito.Mockito.mock(Store.class);

        when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                .thenReturn(Optional.of(store));

        when(reviewRepository.calculateAverageRating(storeId))
                .thenReturn(4.0);

        when(reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId))
                .thenReturn(3L);

        // when
        ReviewResultResponseDto result =
                reviewService.updateReview(
                        reviewId,
                        userId,
                        updateDto
                );

        // then
        System.out.println("결과: " + result);

        assertThat(result).isNotNull();
        assertThat(result.reviewId()).isEqualTo(reviewId);
        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getContent()).isEqualTo("수정된 리뷰입니다");

        verify(store).updateReviewSummary(
                new BigDecimal("4.0"),
                3
        );
    }

    @Test
    @DisplayName("리뷰 수정 실패: 리뷰 없음")
    void updateReviewTest_fail_not_found() {
        // given
        UUID reviewId = UUID.randomUUID();
        Long userId = 1L;

        ReviewUpdateRequestDto updateDto =
                new ReviewUpdateRequestDto(
                        4,
                        "수정된 리뷰입니다",
                        null
                );

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                () -> reviewService.updateReview(
                        reviewId,
                        userId,
                        updateDto
                )
        )
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 수정 실패: 본인 리뷰가 아님")
    void updateReviewTest_fail_not_owner() {
        // given
        UUID reviewId = UUID.randomUUID();

        Long ownerId = 1L;
        Long otherUserId = 999L;

        Review review = Review.builder()
                .requestDto(
                        new ReviewCreateRequestDto(
                                UUID.randomUUID(),
                                5,
                                "기존 리뷰",
                                null
                        )
                )
                .userId(ownerId)
                .storeId(UUID.randomUUID())
                .build();

        ReflectionTestUtils.setField(
                review,
                "id",
                reviewId
        );

        ReviewUpdateRequestDto updateDto =
                new ReviewUpdateRequestDto(
                        4,
                        "수정된 리뷰입니다",
                        null
                );

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                .thenReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(
                () -> reviewService.updateReview(
                        reviewId,
                        otherUserId,
                        updateDto
                )
        )
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 상세 조회 성공: 답글이 있으면 함께 반환된다")
    void getReviewTest_withReply() {
        UUID reviewId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "정말 맛있었어요!", null))
                .userId(userId)
                .storeId(storeId)
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);

        User user = User.builder()
                .email("test@test.com")
                .password("encoded-pw")
                .nickname("맛집탐험가")
                .address("서울시 종로구")
                .role(UserRole.CUSTOMER)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        ReviewReply reply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(storeId)
                .content("감사합니다!")
                .build();

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(reply));

        ReviewResponseDto result = reviewService.getReview(reviewId);
        System.out.println("결과: " + result);

        assertThat(result.reply()).isNotNull();
        assertThat(result.reply().content()).isEqualTo("감사합니다!");
    }
}
