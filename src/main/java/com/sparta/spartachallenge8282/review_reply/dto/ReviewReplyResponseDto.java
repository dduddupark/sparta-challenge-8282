package com.sparta.spartachallenge8282.review_reply.dto;

import com.sparta.spartachallenge8282.review_reply.entity.ReviewReply;

import java.time.LocalDateTime;
import java.util.UUID;

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
