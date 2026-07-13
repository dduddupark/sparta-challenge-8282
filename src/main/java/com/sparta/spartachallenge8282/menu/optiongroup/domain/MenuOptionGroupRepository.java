package com.sparta.spartachallenge8282.menu.optiongroup.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MenuOptionGroupRepository extends JpaRepository<MenuOptionGroup, UUID> {

    /** 단건 조회/수정 시: 삭제되지 않은 옵션 그룹만. */
    Optional<MenuOptionGroup> findByIdAndDeletedAtIsNull(UUID id);

    /** 메뉴별 옵션 그룹 검색. 공개 조회는 Service 에서 isActive=true 로 호출한다. */
    @Query("SELECT g FROM MenuOptionGroup g " +
            "WHERE g.deletedAt IS NULL " +
            "AND g.menuId = :menuId " +
            "AND (:keyword IS NULL OR g.name LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:isActive IS NULL OR g.isActive = :isActive)")
    Page<MenuOptionGroup> searchOptionGroups(@Param("menuId") UUID menuId,
                                             @Param("keyword") String keyword,
                                             @Param("isActive") Boolean isActive,
                                             Pageable pageable);
}
