package com.sparta.spartachallenge8282.review_reply.presentation.dto;

import com.sparta.spartachallenge8282.review_reply.domain.ReviewReply;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 답글 작성/수정 공용 응답 DTO.
 *
 * createdAt·updatedAt 을 모두 포함해 작성/수정 응답에 함께 쓴다.
 * 작성 직후에는 두 값이 동일하고, 수정 후에는 updatedAt 만 갱신된다.
 */

public record ReviewReplyResponseDto (
    UUID reviewId,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ReviewReplyResponseDto from(ReviewReply reply) {
        return new ReviewReplyResponseDto(
                reply.getReviewId(),
                reply.getContent(),
                reply.getCreatedAt(),
                reply.getUpdatedAt()
        );
    }
}
