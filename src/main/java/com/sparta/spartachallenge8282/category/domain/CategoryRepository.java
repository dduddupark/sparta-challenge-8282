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

    @Query("SELECT c FROM Category c " +
            "WHERE c.deletedAt IS NULL " +
            "AND (:keyword IS NULL OR c.name LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:isActive IS NULL OR c.isActive = :isActive)")
    Page<Category> searchCategories(@Param("keyword") String keyword,
                                    @Param("isActive") Boolean isActive,
                                    Pageable pageable);
}
