package com.sparta.spartachallenge8282.review_reply.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, UUID> {
    Optional<ReviewReply> findByReviewIdAndDeletedAtIsNull(UUID reviewId);
    boolean existsByReviewIdAndDeletedAtIsNull(UUID reviewId);
    Slice<ReviewReply> findByStoreIdAndDeletedAtIsNull(UUID storeId, Pageable pageable);
}
