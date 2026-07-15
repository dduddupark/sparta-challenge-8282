package com.sparta.spartachallenge8282.option.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuOptionRepository extends JpaRepository<MenuOption, UUID> {

    /** 단건 조회/수정 시: 삭제되지 않은 옵션만. */
    Optional<MenuOption> findByIdAndDeletedAtIsNull(UUID id);

    List<MenuOption> findAllByOptionGroupIdAndDeletedAtIsNull(UUID optionGroupId);

    /** 메뉴 계단식 삭제용: 여러 옵션 그룹의 삭제되지 않은 옵션을 한 번에 조회한다. */
    List<MenuOption> findAllByOptionGroupIdInAndDeletedAtIsNull(List<UUID> optionGroupIds);

    /** 옵션 그룹별 옵션 검색. 공개 조회는 Service 에서 isActive=true 로 호출한다. */
    @Query("SELECT o FROM MenuOption o " +
            "WHERE o.deletedAt IS NULL " +
            "AND o.optionGroupId = :optionGroupId " +
            "AND (:keyword IS NULL OR o.name LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:isActive IS NULL OR o.isActive = :isActive)")
    Page<MenuOption> searchOptions(@Param("optionGroupId") UUID optionGroupId,
                                   @Param("keyword") String keyword,
                                   @Param("isActive") Boolean isActive,
                                   Pageable pageable);

    /**
     * 주문 생성 시 선택한 옵션 ID 목록을 한 번에 조회한다.
     * 삭제된 옵션은 조회하지 않는다.
     * 왜? : findById()를 반복하면 단건 조회 쿼리가 여러 번 나가기 때문에
     */
    List<MenuOption> findAllByIdInAndDeletedAtIsNull(
            Collection<UUID> ids
    );
}
