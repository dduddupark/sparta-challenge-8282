package com.sparta.spartachallenge8282.review.application;

import com.sparta.spartachallenge8282.order.domain.Order;
import com.sparta.spartachallenge8282.order.domain.OrderStatus;
import com.sparta.spartachallenge8282.order.domain.OrderRepository;
import com.sparta.spartachallenge8282.review.domain.Review;
import com.sparta.spartachallenge8282.review.domain.ReviewRepository;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewCreateRequestDto;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewUpdateRequestDto;
import com.sparta.spartachallenge8282.review_reply.domain.ReviewReplyRepository;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.user.domain.UserRepository;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewSummaryTest {

    private ReviewRepository reviewRepository;
    private OrderRepository orderRepository;
    private UserRepository userRepository;
    private StoreRepository storeRepository;
    private ReviewReplyRepository reviewReplyRepository;

    private ReviewService reviewService;

    private Long userId;
    private UUID orderId;
    private UUID storeId;
    private UUID reviewId;

    @BeforeEach
    void setUp() {
        /*
         * Mockito 객체를 직접 생성한다.
         * MockitoExtension과 @InjectMocks를 사용하지 않으므로
         * Repository가 null이 되는 문제를 피할 수 있다.
         */
        reviewRepository = mock(ReviewRepository.class);
        orderRepository = mock(OrderRepository.class);
        userRepository = mock(UserRepository.class);
        storeRepository = mock(StoreRepository.class);
        reviewReplyRepository = mock(ReviewReplyRepository.class);

        reviewService = new ReviewService(
                reviewRepository,
                orderRepository,
                userRepository,
                storeRepository,
                reviewReplyRepository
        );

        userId = 1L;
        orderId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("리뷰 생성 시 가게 리뷰 집계")
    class CreateReviewSummary {

        @Test
        @DisplayName("리뷰 생성 후 가게 평균 평점과 리뷰 수를 갱신한다")
        void createReview_updatesStoreReviewSummary() {
            // given
            ReviewCreateRequestDto requestDto =
                    mock(ReviewCreateRequestDto.class);

            Order order = mock(Order.class);
            Review savedReview = mock(Review.class);
            Store store = mock(Store.class);

            when(requestDto.orderId()).thenReturn(orderId);
            when(requestDto.rating()).thenReturn(5);
            when(requestDto.content()).thenReturn("맛있어요.");
            when(requestDto.imageUrl()).thenReturn(null);

            when(orderRepository.findById(orderId))
                    .thenReturn(Optional.of(order));

            when(order.getUserId()).thenReturn(userId);
            when(order.getOrderStatus()).thenReturn(OrderStatus.COMPLETED);
            when(order.getStoreId()).thenReturn(storeId);

            when(reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId))
                    .thenReturn(false);

            when(reviewRepository.save(any(Review.class)))
                    .thenReturn(savedReview);

            when(savedReview.getId()).thenReturn(reviewId);
            when(savedReview.getStoreId()).thenReturn(storeId);

            when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                    .thenReturn(Optional.of(store));

            when(reviewRepository.calculateAverageRating(storeId))
                    .thenReturn(4.5);

            when(reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId))
                    .thenReturn(2L);

            // when
            reviewService.createReview(requestDto, userId);

            // then
            verify(reviewRepository).save(any(Review.class));

            verify(store).updateReviewSummary(
                    new BigDecimal("4.5"),
                    2
            );
        }

        @Test
        @DisplayName("첫 번째 리뷰 생성 시 평점과 리뷰 수를 5.0과 1로 갱신한다")
        void createReview_firstReview_updatesSummary() {
            // given
            ReviewCreateRequestDto requestDto =
                    mock(ReviewCreateRequestDto.class);

            Order order = mock(Order.class);
            Review savedReview = mock(Review.class);
            Store store = mock(Store.class);

            when(requestDto.orderId()).thenReturn(orderId);
            when(requestDto.rating()).thenReturn(5);

            when(orderRepository.findById(orderId))
                    .thenReturn(Optional.of(order));

            when(order.getUserId()).thenReturn(userId);
            when(order.getOrderStatus()).thenReturn(OrderStatus.COMPLETED);
            when(order.getStoreId()).thenReturn(storeId);

            when(reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId))
                    .thenReturn(false);

            when(reviewRepository.save(any(Review.class)))
                    .thenReturn(savedReview);

            when(savedReview.getId()).thenReturn(reviewId);

            when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                    .thenReturn(Optional.of(store));

            when(reviewRepository.calculateAverageRating(storeId))
                    .thenReturn(5.0);

            when(reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId))
                    .thenReturn(1L);

            // when
            reviewService.createReview(requestDto, userId);

            // then
            verify(store).updateReviewSummary(
                    new BigDecimal("5.0"),
                    1
            );
        }

        @Test
        @DisplayName("리뷰를 저장한 다음 평점과 리뷰 수를 집계한다")
        void createReview_savesBeforeUpdatingSummary() {
            // given
            ReviewCreateRequestDto requestDto =
                    mock(ReviewCreateRequestDto.class);

            Order order = mock(Order.class);
            Review savedReview = mock(Review.class);
            Store store = mock(Store.class);

            when(requestDto.orderId()).thenReturn(orderId);
            when(requestDto.rating()).thenReturn(5);

            when(orderRepository.findById(orderId))
                    .thenReturn(Optional.of(order));

            when(order.getUserId()).thenReturn(userId);
            when(order.getOrderStatus()).thenReturn(OrderStatus.COMPLETED);
            when(order.getStoreId()).thenReturn(storeId);

            when(reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId))
                    .thenReturn(false);

            when(reviewRepository.save(any(Review.class)))
                    .thenReturn(savedReview);

            when(savedReview.getId()).thenReturn(reviewId);

            when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                    .thenReturn(Optional.of(store));

            when(reviewRepository.calculateAverageRating(storeId))
                    .thenReturn(5.0);

            when(reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId))
                    .thenReturn(1L);

            // when
            reviewService.createReview(requestDto, userId);

            // then
            InOrder inOrder = inOrder(
                    reviewRepository,
                    storeRepository,
                    store
            );

            inOrder.verify(reviewRepository)
                    .save(any(Review.class));

            inOrder.verify(storeRepository)
                    .findByIdAndDeletedAtIsNull(storeId);

            inOrder.verify(reviewRepository)
                    .calculateAverageRating(storeId);

            inOrder.verify(reviewRepository)
                    .countByStoreIdAndDeletedAtIsNull(storeId);

            inOrder.verify(store)
                    .updateReviewSummary(
                            new BigDecimal("5.0"),
                            1
                    );
        }
    }

    @Nested
    @DisplayName("리뷰 수정 시 가게 리뷰 집계")
    class UpdateReviewSummary {

        @Test
        @DisplayName("리뷰 평점을 수정하면 변경된 평균 평점과 리뷰 수를 갱신한다")
        void updateReview_updatesStoreReviewSummary() {
            // given
            ReviewUpdateRequestDto requestDto =
                    mock(ReviewUpdateRequestDto.class);

            Review review = mock(Review.class);
            Store store = mock(Store.class);

            when(requestDto.rating()).thenReturn(3);
            when(requestDto.content()).thenReturn("수정된 리뷰");
            when(requestDto.imageUrl()).thenReturn(null);

            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                    .thenReturn(Optional.of(review));

            when(review.getUserId()).thenReturn(userId);
            when(review.getStoreId()).thenReturn(storeId);
            when(review.getId()).thenReturn(reviewId);

            when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                    .thenReturn(Optional.of(store));

            when(reviewRepository.calculateAverageRating(storeId))
                    .thenReturn(3.75);

            when(reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId))
                    .thenReturn(4L);

            // when
            reviewService.updateReview(
                    reviewId,
                    userId,
                    requestDto
            );

            // then
            verify(review).update(
                    3,
                    "수정된 리뷰",
                    null
            );

            /*
             * 3.75를 소수점 한 자리까지 HALF_EVEN으로 반올림하면 3.8이다.
             */
            verify(store).updateReviewSummary(
                    new BigDecimal("3.8"),
                    4
            );
        }

        @Test
        @DisplayName("리뷰 정보를 수정한 다음 가게 리뷰 집계를 갱신한다")
        void updateReview_updatesReviewBeforeSummary() {
            // given
            ReviewUpdateRequestDto requestDto =
                    mock(ReviewUpdateRequestDto.class);

            Review review = mock(Review.class);
            Store store = mock(Store.class);

            when(requestDto.rating()).thenReturn(4);
            when(requestDto.content()).thenReturn("수정 내용");
            when(requestDto.imageUrl()).thenReturn(null);

            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                    .thenReturn(Optional.of(review));

            when(review.getUserId()).thenReturn(userId);
            when(review.getStoreId()).thenReturn(storeId);
            when(review.getId()).thenReturn(reviewId);

            when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                    .thenReturn(Optional.of(store));

            when(reviewRepository.calculateAverageRating(storeId))
                    .thenReturn(4.0);

            when(reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId))
                    .thenReturn(2L);

            // when
            reviewService.updateReview(
                    reviewId,
                    userId,
                    requestDto
            );

            // then
            InOrder inOrder = inOrder(
                    review,
                    storeRepository,
                    reviewRepository,
                    store
            );

            inOrder.verify(review).update(
                    4,
                    "수정 내용",
                    null
            );

            inOrder.verify(storeRepository)
                    .findByIdAndDeletedAtIsNull(storeId);

            inOrder.verify(reviewRepository)
                    .calculateAverageRating(storeId);

            inOrder.verify(reviewRepository)
                    .countByStoreIdAndDeletedAtIsNull(storeId);

            inOrder.verify(store)
                    .updateReviewSummary(
                            new BigDecimal("4.0"),
                            2
                    );
        }
    }

    @Nested
    @DisplayName("리뷰 삭제 시 가게 리뷰 집계")
    class DeleteReviewSummary {

        @Test
        @DisplayName("리뷰를 삭제하면 남은 리뷰의 평균 평점과 리뷰 수를 갱신한다")
        void deleteReview_updatesStoreReviewSummary() {
            // given
            Review review = mock(Review.class);
            Store store = mock(Store.class);

            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                    .thenReturn(Optional.of(review));

            when(review.getUserId()).thenReturn(userId);
            when(review.getStoreId()).thenReturn(storeId);

            when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                    .thenReturn(Optional.of(store));

            when(reviewRepository.calculateAverageRating(storeId))
                    .thenReturn(4.0);

            when(reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId))
                    .thenReturn(2L);

            // when
            reviewService.deleteReview(
                    reviewId,
                    userId,
                    UserRole.CUSTOMER
            );

            // then
            verify(review).softDelete(userId);

            verify(store).updateReviewSummary(
                    new BigDecimal("4.0"),
                    2
            );
        }

        @Test
        @DisplayName("마지막 리뷰를 삭제하면 평점 0.0과 리뷰 수 0으로 갱신한다")
        void deleteReview_lastReview_updatesSummaryToZero() {
            // given
            Review review = mock(Review.class);
            Store store = mock(Store.class);

            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                    .thenReturn(Optional.of(review));

            when(review.getUserId()).thenReturn(userId);
            when(review.getStoreId()).thenReturn(storeId);

            when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                    .thenReturn(Optional.of(store));

            /*
             * 남은 리뷰가 없으면 AVG 결과는 null이다.
             */
            when(reviewRepository.calculateAverageRating(storeId))
                    .thenReturn(null);

            when(reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId))
                    .thenReturn(0L);

            // when
            reviewService.deleteReview(
                    reviewId,
                    userId,
                    UserRole.CUSTOMER
            );

            // then
            verify(review).softDelete(userId);

            verify(store).updateReviewSummary(
                    new BigDecimal("0.0"),
                    0
            );
        }

        @Test
        @DisplayName("리뷰를 소프트 삭제한 다음 가게 리뷰 집계를 갱신한다")
        void deleteReview_softDeletesBeforeSummary() {
            // given
            Review review = mock(Review.class);
            Store store = mock(Store.class);

            when(reviewRepository.findByIdAndDeletedAtIsNull(reviewId))
                    .thenReturn(Optional.of(review));

            when(review.getUserId()).thenReturn(userId);
            when(review.getStoreId()).thenReturn(storeId);

            when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                    .thenReturn(Optional.of(store));

            when(reviewRepository.calculateAverageRating(storeId))
                    .thenReturn(0.0);

            when(reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId))
                    .thenReturn(0L);

            // when
            reviewService.deleteReview(
                    reviewId,
                    userId,
                    UserRole.CUSTOMER
            );

            // then
            InOrder inOrder = inOrder(
                    review,
                    storeRepository,
                    reviewRepository,
                    store
            );

            inOrder.verify(review)
                    .softDelete(userId);

            inOrder.verify(storeRepository)
                    .findByIdAndDeletedAtIsNull(storeId);

            inOrder.verify(reviewRepository)
                    .calculateAverageRating(storeId);

            inOrder.verify(reviewRepository)
                    .countByStoreIdAndDeletedAtIsNull(storeId);

            inOrder.verify(store)
                    .updateReviewSummary(
                            new BigDecimal("0.0"),
                            0
                    );
        }
    }
}