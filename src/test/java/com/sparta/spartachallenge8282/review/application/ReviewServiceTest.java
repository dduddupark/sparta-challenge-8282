package com.sparta.spartachallenge8282.review.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.order.entity.Order;
import com.sparta.spartachallenge8282.order.enums.OrderStatus;
import com.sparta.spartachallenge8282.order.repository.OrderRepository;
import com.sparta.spartachallenge8282.review.domain.Review;
import com.sparta.spartachallenge8282.review.domain.ReviewRepository;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewCreateRequestDto;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewUpdateRequestDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewResponseDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewResultResponseDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewSliceResponseDto;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private ReviewService reviewService;

    // 실패 테스트에서 공통으로 예외 정보를 출력하는 헬퍼
    private void printException(Throwable e) {
        CustomException ex = (CustomException) e;
        System.out.println("예외 발생: " + ex.getErrorCode().getCode() + " - " + ex.getErrorCode().getMessage());
    }

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReviewTest() {
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

        UUID fakeReviewId = UUID.randomUUID();
        ReflectionTestUtils.setField(savedReview, "id", fakeReviewId);

        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewResultResponseDto result = reviewService.createReview(requestDto, userId);
        System.out.println("결과: " + result);

        assertThat(result).isNotNull();
        assertThat(result.reviewId()).isEqualTo(fakeReviewId);
    }

    @Test
    @DisplayName("리뷰 생성 실패: 주문을 찾을 수 없음")
    void createReviewTest_fail_not_order() {
        UUID orderId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(
                orderId, 5, "정말 맛있었어요!", null
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(requestDto, userId))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 생성 실패: 본인 주문이 아님")
    void createReviewTest_fail_not_user() {
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

        assertThatThrownBy(() -> reviewService.createReview(requestDto, otherUserId))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 생성 실패: 배달완료 상태가 아님")
    void createReviewTest_fail_not_complete() {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(
                orderId, 5, "정말 맛있었어요!", null
        );

        Order order = Order.create(
                "ORD-0003", userId, storeId,
                10000, 0, 3000,
                "서울시 종로구", "3층", null
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> reviewService.createReview(requestDto, userId))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 생성 실패: 이미 리뷰가 존재함")
    void createReviewTest_fail_exist() {
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

        assertThatThrownBy(() -> reviewService.createReview(requestDto, userId))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("가게 리뷰 목록 조회 성공")
    void getReviewsByStoreTest() {
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "맛있어요", null))
                .userId(userId)
                .storeId(storeId)
                .build();

        Slice<Review> slice = new SliceImpl<>(List.of(review), pageable, false);

        User user = User.builder()
                .email("test@test.com")
                .password("encoded-pw")
                .nickname("맛집탐험가")
                .address("서울시 종로구")
                .role(UserRole.CUSTOMER)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        when(storeRepository.existsById(storeId)).thenReturn(true);
        when(reviewRepository.findByStoreIdAndDeletedAtIsNull(storeId, pageable)).thenReturn(slice);
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));

        ReviewSliceResponseDto result = reviewService.getReviewsByStore(storeId, pageable);
        System.out.println("결과: " + result);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("가게 리뷰 목록 조회 실패: 가게 없음")
    void getReviewsByStoreTest_fail_store_not_found() {
        UUID storeId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(storeRepository.existsById(storeId)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getReviewsByStore(storeId, pageable))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 상세 조회 성공")
    void getReviewTest() {
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

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        ReviewResponseDto result = reviewService.getReview(reviewId);
        System.out.println("결과: " + result);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("리뷰 상세 조회 실패: 리뷰 없음")
    void getReviewTest_fail_review_not_found() {
        UUID reviewId = UUID.randomUUID();

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReview(reviewId))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 상세 조회 실패: 리뷰 작성자가 존재하지 않음")
    void getReviewTest_fail_user_not_found() {
        UUID reviewId = UUID.randomUUID();
        Long userId = 1L;

        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "정말 맛있었어요!", null))
                .userId(userId)
                .storeId(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReview(reviewId))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 삭제 성공: 본인")
    void deleteReviewTest_owner() {
        UUID reviewId = UUID.randomUUID();
        Long userId = 1L;

        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "삭제될 리뷰", null))
                .userId(userId)
                .storeId(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, userId, "CUSTOMER");
        System.out.println("삭제 완료: reviewId=" + reviewId + ", isDeleted=" + review.isDeleted());
    }

    @Test
    @DisplayName("리뷰 삭제 성공: MANAGER 권한")
    void deleteReviewTest_manager() {
        UUID reviewId = UUID.randomUUID();
        Long ownerId = 1L;
        Long managerId = 999L;

        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "삭제될 리뷰", null))
                .userId(ownerId)
                .storeId(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));

        reviewService.deleteReview(reviewId, managerId, "MANAGER");
        System.out.println("삭제 완료: reviewId=" + reviewId + ", isDeleted=" + review.isDeleted());
    }

    @Test
    @DisplayName("리뷰 삭제 실패: 리뷰 없음")
    void deleteReviewTest_fail_not_found() {
        UUID reviewId = UUID.randomUUID();

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, 1L, "CUSTOMER"))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 삭제 실패: 본인도 아니고 권한도 없음")
    void deleteReviewTest_fail_no_permission() {
        UUID reviewId = UUID.randomUUID();
        Long ownerId = 1L;
        Long otherUserId = 999L;

        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "삭제될 리뷰", null))
                .userId(ownerId)
                .storeId(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, otherUserId, "CUSTOMER"))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReviewTest() {
        UUID reviewId = UUID.randomUUID();
        Long userId = 1L;

        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "기존 리뷰", null))
                .userId(userId)
                .storeId(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);

        ReviewUpdateRequestDto updateDto = new ReviewUpdateRequestDto(4, "수정된 리뷰입니다", null);

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));

        ReviewResultResponseDto result = reviewService.updateReview(reviewId, userId, updateDto);
        System.out.println("결과: " + result);

        assertThat(result).isNotNull();
        assertThat(result.reviewId()).isEqualTo(reviewId);
    }

    @Test
    @DisplayName("리뷰 수정 실패: 리뷰 없음")
    void updateReviewTest_fail_not_found() {
        UUID reviewId = UUID.randomUUID();
        Long userId = 1L;
        ReviewUpdateRequestDto updateDto = new ReviewUpdateRequestDto(4, "수정된 리뷰입니다", null);

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(reviewId, userId, updateDto))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("리뷰 수정 실패: 본인 리뷰가 아님")
    void updateReviewTest_fail_not_owner() {
        UUID reviewId = UUID.randomUUID();
        Long ownerId = 1L;
        Long otherUserId = 999L;

        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "기존 리뷰", null))
                .userId(ownerId)
                .storeId(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);

        ReviewUpdateRequestDto updateDto = new ReviewUpdateRequestDto(4, "수정된 리뷰입니다", null);

        when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(reviewId, otherUserId, updateDto))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }
}