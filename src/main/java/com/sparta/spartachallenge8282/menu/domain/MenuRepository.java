package com.sparta.spartachallenge8282.menu.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuRepository extends JpaRepository<Menu, UUID> {

    /** 단건 조회/수정 시: 삭제되지 않은 메뉴만. */
    Optional<Menu> findByIdAndDeletedAtIsNull(UUID id);

    // 삭제 시에는 "없는 것"과 "이미 삭제된 것"을 구분하기 위해 JpaRepository 기본 findById(삭제 포함)를 그대로 쓴다.

    /**
     * 가게별 메뉴 목록 검색 (페이징).
     *
     * <p>{@code includeHidden=false}(공개 조회)면 숨김 메뉴를 제외한다. keyword/status/badge 는 null 이면 해당 조건을 건너뛴다.
     */
    @Query("SELECT m FROM Menu m " +
            "WHERE m.deletedAt IS NULL " +
            "AND m.storeId = :storeId " +
            "AND (:includeHidden = true OR m.isHidden = false) " +
            "AND (:keyword IS NULL OR m.name LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:status IS NULL OR m.status = :status) " +
            "AND (:badge IS NULL OR m.badge = :badge)")
    Page<Menu> searchMenus(@Param("storeId") UUID storeId,
                           @Param("keyword") String keyword,
                           @Param("status") MenuStatus status,
                           @Param("badge") MenuBadge badge,
                           @Param("includeHidden") boolean includeHidden,
                           Pageable pageable);

    /**
     * STORE
     * 가게 활성화 조건
     * 가게에 매뉴 1개이상 존재
     */
    boolean existsByStoreIdAndDeletedAtIsNull(UUID storeId);
}
