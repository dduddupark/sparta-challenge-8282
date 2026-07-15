package com.sparta.spartachallenge8282.review_reply.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.review.domain.Review;
import com.sparta.spartachallenge8282.review.domain.ReviewRepository;
import com.sparta.spartachallenge8282.review_reply.presentation.dto.request.ReviewReplyRequestDto;
import com.sparta.spartachallenge8282.review_reply.presentation.dto.response.ReviewReplyResponseDto;
import com.sparta.spartachallenge8282.review_reply.presentation.dto.response.ReviewReplySliceResponseDto;
import com.sparta.spartachallenge8282.review_reply.domain.ReviewReply;
import com.sparta.spartachallenge8282.review_reply.domain.ReviewReplyRepository;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 답글(ReviewReply) 생성/수정/삭제 비즈니스 로직.
 *
 * 권한 검증 흐름: reviewId → Review 조회 → review.getStoreId() → Store 조회
 * → store.getOwner().getId() 가 요청자(userId)와 일치하는지 확인한다.
 * storeId 는 클라이언트가 보내지 않으며, 항상 Review 에서 얻어온다.
 *
 * 작성 시에는 리뷰 존재 여부(REPLY_TARGET_REVIEW_NOT_FOUND)와 1:1 제약
 * (REPLY_ALREADY_EXISTS)을 함께 검증한다. 수정/삭제는 답글 자체의 존재 여부
 * (REPLY_NOT_FOUND)만 확인하면 되므로 ReviewReplyRepository 로 바로 조회한다.
 *
 */

@Service
@RequiredArgsConstructor

public class ReviewReplyService {

    private final ReviewReplyRepository reviewReplyRepository;
    private final ReviewRepository reviewRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public ReviewReplyResponseDto createReply(ReviewReplyRequestDto requestDto, UUID reviewId, Long userId) {

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPLY_TARGET_REVIEW_NOT_FOUND));

        Store store = storeRepository.findById(review.getStoreId())
                .orElseThrow(()->new CustomException(ErrorCode.STORE_NOT_FOUND));

        // Store Error 코드에서 NOT_STORE_OWNER(20008, HttpStatus.FORBIDDEN, "본인 가게가 아닙니다.") 추가가 필요한지 논의
        if(!store.getOwner().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 1:1 제약 - 이미 답글 있는지 확인
        if (reviewReplyRepository.existsByReviewIdAndDeletedAtIsNull(reviewId)) {
            throw new CustomException(ErrorCode.REPLY_ALREADY_EXISTS);
        }

        ReviewReply reviewReply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(review.getStoreId())
                .content(requestDto.content())
                .build();

        return ReviewReplyResponseDto.from(reviewReplyRepository.save(reviewReply));
    }

    @Transactional
    public ReviewReplyResponseDto updateReply(ReviewReplyRequestDto requestDto, UUID reviewId, Long userId) {
        ReviewReply reviewReply = reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(()->new CustomException(ErrorCode.REPLY_NOT_FOUND));

        Store store = storeRepository.findById(reviewReply.getStoreId())
                .orElseThrow(()->new CustomException(ErrorCode.STORE_NOT_FOUND));

        if(!store.getOwner().getId().equals(userId)){
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        reviewReply.update(requestDto.content());
        return ReviewReplyResponseDto.from(reviewReply);
    }

    @Transactional
    public void deleteReply(UUID reviewId, Long userId, UserRole role) {
        ReviewReply reviewReply = reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(()->new CustomException(ErrorCode.REPLY_NOT_FOUND));

        Store store = storeRepository.findById(reviewReply.getStoreId())
                .orElseThrow(()->new CustomException(ErrorCode.STORE_NOT_FOUND));

        boolean isOwner = store.getOwner().getId().equals(userId);
        boolean isManagerOrMaster = role == UserRole.MANAGER || role == UserRole.MASTER;

        if (!isOwner && !isManagerOrMaster) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        reviewReply.softDelete(userId);
    }

    @Transactional(readOnly = true)
    public ReviewReplySliceResponseDto getRepliesByStore(UUID storeId, Pageable pageable) {

        if (!storeRepository.existsById(storeId)) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }

        Slice<ReviewReply> slice = reviewReplyRepository.findByStoreIdAndDeletedAtIsNull(storeId, pageable);

        return ReviewReplySliceResponseDto.from(slice);
    }

}
