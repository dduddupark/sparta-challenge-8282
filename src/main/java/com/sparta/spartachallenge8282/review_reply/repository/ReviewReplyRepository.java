package com.sparta.spartachallenge8282.review_reply.repository;

import com.sparta.spartachallenge8282.review_reply.entity.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, UUID> {
    Optional<ReviewReply> findByReviewIdAndDeletedAtIsNull(UUID reviewId);
    boolean existsByReviewIdAndDeletedAtIsNull(UUID reviewId);
}
