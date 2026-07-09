package com.sparta.spartachallenge8282.review.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.review.dto.request.ReviewCreateRequestDto;
import com.sparta.spartachallenge8282.review.dto.request.ReviewUpdateRequestDto;
import com.sparta.spartachallenge8282.review.dto.response.ReviewResponseDto;
import com.sparta.spartachallenge8282.review.dto.response.ReviewResultResponseDto;
import com.sparta.spartachallenge8282.review.dto.response.ReviewSliceResponseDto;
import com.sparta.spartachallenge8282.review.entity.Review;
import com.sparta.spartachallenge8282.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional
    public ReviewResultResponseDto createReview(ReviewCreateRequestDto requestDto, Long userId, UUID storeId) {
        Review review = Review.builder()
                .requestDto(requestDto)
                .userId(userId)
                .storeId(storeId)
                .build();

        //생성이 완료되면 생성된 리뷰의 아이디를 반환
        return ReviewResultResponseDto.from(reviewRepository.save(review).getId());
    }

    @Transactional(readOnly = true)
    public ReviewSliceResponseDto getReviewsByStore(UUID storeId, Pageable pageable) {

        // TODO: Store 완성되면 존재하는 가게인지 검증 추가 (STORE_NOT_FOUND)

        Slice<Review> slice = reviewRepository.findByStoreIdAndDeletedAtIsNull(storeId, pageable);
        return ReviewSliceResponseDto.from(slice);
    }

    @Transactional(readOnly = true)
    public ReviewResponseDto getReview(UUID reviewId) {

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        return ReviewResponseDto.from(review);
    }

    @Transactional
    public ReviewResultResponseDto updateReview(UUID reviewId, Long userId, ReviewUpdateRequestDto requestDto) {

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }

        review.update(requestDto.rating(), requestDto.content(), requestDto.imageUrl());

        return ReviewResultResponseDto.from(review.getId());
    }

    @Transactional
    public void deleteReview(UUID reviewId, Long userId) {

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }

        review.softDelete(userId);
    }
}
