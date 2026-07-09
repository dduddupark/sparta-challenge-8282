package com.sparta.spartachallenge8282.review_reply.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.review.entity.Review;
import com.sparta.spartachallenge8282.review.repository.ReviewRepository;
import com.sparta.spartachallenge8282.review_reply.dto.ReviewReplyRequestDto;
import com.sparta.spartachallenge8282.review_reply.dto.ReviewReplyResponseDto;
import com.sparta.spartachallenge8282.review_reply.entity.ReviewReply;
import com.sparta.spartachallenge8282.review_reply.repository.ReviewReplyRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewReplyService {

    private final ReviewReplyRepository reviewReplyRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public ReviewReplyResponseDto createReply(@Valid ReviewReplyRequestDto requestDto, UUID reviewId, Long userId, UUID storeId) {

        Review eview = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPLY_TARGET_REVIEW_NOT_FOUND));

        // TODO: storeId 검증 (review.getStoreId()와 일치하는지, 진짜 소유주인지)

        // 1:1 제약 - 이미 답글 있는지 확인
        if (reviewReplyRepository.existsByReviewIdAndDeletedAtIsNull(reviewId)) {
            throw new CustomException(ErrorCode.REPLY_ALREADY_EXISTS);
        }

        ReviewReply reviewReply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(storeId)
                .content(requestDto.content())
                .build();

        return ReviewReplyResponseDto.from(reviewReplyRepository.save(reviewReply));
    }

    @Transactional
    public ReviewReplyResponseDto updateReply(@Valid ReviewReplyRequestDto requestDto, UUID reviewId, Long userId, UUID storeId) {
        ReviewReply reviewReply = reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(()->new CustomException(ErrorCode.REPLY_NOT_FOUND));

        if(!reviewReply.getStoreId().equals(storeId)){
            throw new CustomException(ErrorCode.NOT_REPLY_OWNER);
        }

        reviewReply.update(requestDto.content());
        return ReviewReplyResponseDto.from(reviewReply);
    }

    @Transactional
    public void deleteReply(UUID reviewId, Long userId, UUID storeId) {
        ReviewReply reviewReply = reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(()->new CustomException(ErrorCode.REPLY_NOT_FOUND));

        if(!reviewReply.getReviewId().equals(reviewId)) {
            throw new CustomException(ErrorCode.NOT_REPLY_OWNER);
        }

        reviewReply.softDelete(userId); // 임시로 Null 처리, User ID 를 넣어야함
    }


}
