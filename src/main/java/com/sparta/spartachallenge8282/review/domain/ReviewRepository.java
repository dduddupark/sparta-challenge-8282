package com.sparta.spartachallenge8282.review.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Review 조회용 Repository.
 * 모든 조회는 deletedAt IS NULL 조건을 기본으로 한다 (soft delete).
 * 가게별 목록 조회는 count 쿼리가 필요 없는 Slice로 페이징한다
 * (전체 개수, 전체 페이지 수는 응답에 없음, hasNext만 제공).
 */

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByIdAndDeletedAtIsNull(UUID id);
    Slice<Review> findByStoreIdAndDeletedAtIsNull(UUID storeId, Pageable pageable);

    boolean existsByOrderId(UUID orderId);


    @Query("""
            SELECT COALESCE(AVG(r.rating), 0)
            FROM Review r
            WHERE r.storeId = :storeId
              AND r.deletedAt is NULL""")
    Double calculateAverageRating(@Param("storeId") UUID storeId);

    long countByStoreIdAndDeletedAtIsNull(UUID storeId);
}
