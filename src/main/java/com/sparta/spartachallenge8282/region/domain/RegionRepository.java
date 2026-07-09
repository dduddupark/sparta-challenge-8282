package com.sparta.spartachallenge8282.region.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {
    boolean existsByNameAndDeletedAtIsNull(String name);
    Optional<Region> findByIdAndDeletedAtIsNull(UUID id);

    // 공개 조회 전용: 활성 항목만. 수정, 삭제는 비활성 포함 조회(findByIdAndDeletedAtIsNull)를 그대로 쓴다.
    Optional<Region> findByIdAndDeletedAtIsNullAndIsActiveTrue(UUID id);

    @Query("SELECT r FROM Region r " +
            "WHERE r.deletedAt IS NULL " +
            "AND (:keyword IS NULL OR r.name LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:isActive IS NULL OR r.isActive = :isActive)")
    Page<Region> searchRegions(@Param("keyword") String keyword, @Param("isActive") Boolean isActive, Pageable pageable);
}
