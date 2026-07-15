package com.sparta.spartachallenge8282.review_reply.presentation.dto.response;

import com.sparta.spartachallenge8282.review_reply.domain.ReviewReply;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 답글 목록 조회용 아이템 DTO.
 */
public record ReviewReplyListItemResponseDto (
        UUID reviewId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReviewReplyListItemResponseDto from(ReviewReply reply) {
        return new ReviewReplyListItemResponseDto(
                reply.getReviewId(),
                reply.getContent(),
                reply.getCreatedAt(),
                reply.getUpdatedAt()
        );
    }
}
