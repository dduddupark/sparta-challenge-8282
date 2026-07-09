package com.sparta.spartachallenge8282.category.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsByNameAndDeletedAtIsNull(String name);

    Optional<Category> findByIdAndDeletedAtIsNull(UUID id);

    // 공개 조회 전용: 활성 항목만. 수정, 삭제는 비활성 포함 조회(findByIdAndDeletedAtIsNull)를 그대로 쓴다.
    Optional<Category> findByIdAndDeletedAtIsNullAndIsActiveTrue(UUID id);

    @Query("SELECT c FROM Category c " +
            "WHERE c.deletedAt IS NULL " +
            "AND (:keyword IS NULL OR c.name LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:isActive IS NULL OR c.isActive = :isActive)")
    Page<Category> searchCategories(@Param("keyword") String keyword,
                                    @Param("isActive") Boolean isActive,
                                    Pageable pageable);
}
