package com.sparta.spartachallenge8282.menu.optiongroup.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuOptionGroupRepository extends JpaRepository<MenuOptionGroup, UUID> {

    /** 단건 조회/수정 시: 삭제되지 않은 옵션 그룹만. */
    Optional<MenuOptionGroup> findByIdAndDeletedAtIsNull(UUID id);

    /** 메뉴별 옵션 그룹 목록 (정렬 순서). */
    List<MenuOptionGroup> findAllByMenuIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID menuId);
}
