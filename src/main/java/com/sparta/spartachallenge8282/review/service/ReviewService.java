package com.sparta.spartachallenge8282.review.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.review.dto.*;
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

    public ResReviewResultDto createReview(ReqCreateReviewDto requestDto, Long userId, UUID storeId) {
        Review review = Review.builder()
                .requestDto(requestDto)
                .userId(userId)
                .storeId(storeId)
                .build();

        return ResReviewResultDto.from(reviewRepository.save(review).getId());
    }

    @Transactional(readOnly = true)
    public ResReviewSliceDto getReviewsByStore(UUID storeId, Pageable pageable) {

        // TODO: Store 완성되면 존재하는 가게인지 검증 추가 (STORE_NOT_FOUND)

        Slice<Review> slice = reviewRepository.findByStoreIdAndDeletedAtIsNull(storeId, pageable);
        return ResReviewSliceDto.from(slice);
    }

    @Transactional(readOnly = true)
    public ResReviewDto getReview(UUID reviewId) {

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));// TODO: REVIEW_NOT_FOUND(80006)

        return ResReviewDto.from(review);
    }

    @Transactional
    public ResReviewResultDto updateReview(UUID reviewId, Long userId, ReqUpdateReviewDto requestDto) {

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND)); // TODO: REVIEW_NOT_FOUND(80006)

        if (!review.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED); // TODO: NOT_REVIEW_OWNER(80005)
        }

        review.update(requestDto.getRating(), requestDto.getContent(), requestDto.getImageUrl());

        return ResReviewResultDto.from(review.getId());
    }

    @Transactional
    public void deleteReview(UUID reviewId, Long userId) {

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND)); // TODO: REVIEW_NOT_FOUND(80006)

        if (!review.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED); // TODO: NOT_REVIEW_OWNER(80005)
        }

        review.softDelete(userId);
    }
}
