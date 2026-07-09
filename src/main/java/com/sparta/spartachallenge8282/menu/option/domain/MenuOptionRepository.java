package com.sparta.spartachallenge8282.menu.option.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuOptionRepository extends JpaRepository<MenuOption, UUID> {

    /** 단건 조회/수정 시: 삭제되지 않은 옵션만. */
    Optional<MenuOption> findByIdAndDeletedAtIsNull(UUID id);

    /** 옵션 그룹별 옵션 목록 (정렬 순서). */
    List<MenuOption> findAllByOptionGroupIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID optionGroupId);
}
