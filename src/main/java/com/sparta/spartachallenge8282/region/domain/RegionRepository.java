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

    @Query("SELECT r FROM Region r " +
            "WHERE r.deletedAt IS NULL " +
            "AND (:keyword IS NULL OR r.name LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:isActive IS NULL OR r.isActive = :isActive)")
    Page<Region> searchRegions(@Param("keyword") String keyword, @Param("isActive") Boolean isActive, Pageable pageable);
}
