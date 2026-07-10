package com.sparta.spartachallenge8282.review.repository;

import com.sparta.spartachallenge8282.review.entity.Review;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByIdAndDeletedAtIsNull(UUID id);
    Slice<Review> findByStoreIdAndDeletedAtIsNull(UUID storeId, Pageable pageable);

    boolean existsByOrderId(UUID orderId);
}
