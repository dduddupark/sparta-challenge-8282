package com.sparta.spartachallenge8282.review.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.order.domain.Order;
import com.sparta.spartachallenge8282.order.domain.OrderStatus;
import com.sparta.spartachallenge8282.order.domain.OrderRepository;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewCreateRequestDto;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewUpdateRequestDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewResponseDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewResultResponseDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewSliceResponseDto;
import com.sparta.spartachallenge8282.review.domain.Review;
import com.sparta.spartachallenge8282.review.domain.ReviewRepository;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.user.domain.User;
import com.sparta.spartachallenge8282.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 리뷰 생성/조회/수정/삭제 비즈니스 로직.
 * 생성 시 storeId를 파라미터로 받지 않는다 - orderId로 Order를 조회해
 * 1) 본인 주문인지(REVIEW_NOT_ORDER_OWNER)
 * 2) 배달완료(OrderStatus.COMPLETED) 상태인지(REVIEW_NOT_DELIVERED)
 * 3) 이미 리뷰가 있는 주문인지(REVIEW_ALREADY_EXISTS)
 * 를 순서대로 검증한 뒤, order.getStoreId()로 storeId를 얻어 저장한다.
 * 목록/상세 조회는 인증이 필요 없다 - 생성/수정/삭제만 로그인한 본인 확인이 필요하다.
 * 삭제는 본인 또는 MANAGER/MASTER 권한을 가진 경우에도 가능하다.
 */

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public ReviewResultResponseDto createReview(ReviewCreateRequestDto requestDto, Long userId) {

        Order order = orderRepository.findById(requestDto.orderId())
                .orElseThrow(()-> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if(!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.REVIEW_NOT_ORDER_OWNER);
        }

        if(order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new CustomException(ErrorCode.REVIEW_NOT_DELIVERED);
        }

        if(reviewRepository.existsByOrderId(requestDto.orderId())) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.builder()
                .requestDto(requestDto)
                .userId(userId)
                .storeId(order.getStoreId())
                .build();

        Review savedReview = reviewRepository.save(review);

        //리뷰 생성 후 리뷰 집계를 갱신
        refreshStoreReviewSummary(review.getStoreId());

        //생성이 완료되면 생성된 리뷰의 아이디를 반환
        return ReviewResultResponseDto.from(savedReview.getId());
    }

    @Transactional(readOnly = true)
    public ReviewSliceResponseDto getReviewsByStore(UUID storeId, Pageable pageable) {

        if(!storeRepository.existsById(storeId)) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }

        Slice<Review> slice = reviewRepository.findByStoreIdAndDeletedAtIsNull(storeId, pageable);

        List<Long> userIds = slice.getContent().stream()
                .map(Review::getUserId)
                .distinct()
                .toList();

        Map<Long, String> nicknameMap = userRepository.findAllById(userIds).stream()
                .filter(user -> !user.isDeleted())
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return ReviewSliceResponseDto.from(slice, nicknameMap);
    }

    @Transactional(readOnly = true)
    public ReviewResponseDto getReview(UUID reviewId) {

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        User user = userRepository.findByIdAndDeletedAtIsNull(review.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return ReviewResponseDto.from(review, user.getNickname());
    }

    @Transactional
    public ReviewResultResponseDto updateReview(UUID reviewId, Long userId, ReviewUpdateRequestDto requestDto) {

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }

        review.update(requestDto.rating(), requestDto.content(), requestDto.imageUrl());

        //리뷰 수정 후 리뷰 집계를 갱신
        refreshStoreReviewSummary(review.getStoreId());

        return ReviewResultResponseDto.from(review.getId());
    }

    @Transactional
    public void deleteReview(UUID reviewId, Long userId, String role) {

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        boolean isOwner = review.getUserId().equals(userId);
        boolean isManagerOrMaster = "MANAGER".equals(role) || "MASTER".equals(role);

        if (!isOwner && !isManagerOrMaster) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }

        review.softDelete(userId);

        //리뷰 삭제 후 리뷰 집계를 갱신
        refreshStoreReviewSummary(review.getStoreId());
    }


    /**
     * 리뷰 집계 갱신
     */
    private void refreshStoreReviewSummary(UUID storeId) {
        Store store = storeRepository
                .findByIdAndDeletedAtIsNull(storeId)
                .orElseThrow(
                        () -> new CustomException(ErrorCode.STORE_NOT_FOUND)
                );

        Double averageRating = reviewRepository.calculateAverageRating(storeId);
        long reviewCount = reviewRepository.countByStoreIdAndDeletedAtIsNull(storeId);

        BigDecimal rating = BigDecimal
                .valueOf(averageRating == null ? 0.0 : averageRating)
                .setScale(1, RoundingMode.HALF_EVEN);

        store.updateReviewSummary(rating, Math.toIntExact(reviewCount));


    }
}
