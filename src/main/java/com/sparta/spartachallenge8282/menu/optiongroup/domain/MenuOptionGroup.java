package com.sparta.spartachallenge8282.menu.optiongroup.domain;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * 메뉴 옵션 그룹 (예: "음료 선택", "사이드 선택").
 *
 * <p>메뉴에 종속된다. Menu 존재는 Service 에서 검증하고, 여기선 {@code menuId} 를 단순 UUID 로 참조한다.
 * {@code min_select}/{@code max_select} 범위는 DB {@code @Check} + Service 검증(INVALID_OPTION_SELECT_RANGE)로 방어한다.
 */
@Entity
@Table(name = "p_menu_option_group")
@Check(constraints = "min_select >= 0 and max_select >= min_select")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuOptionGroup extends BaseEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "menu_id", nullable = false)
    private UUID menuId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean isRequired;

    @Column(nullable = false)
    private int minSelect;

    @Column(nullable = false)
    private int maxSelect;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean isActive;

    @Builder
    public MenuOptionGroup(UUID menuId, String name, Boolean isRequired,
                           Integer minSelect, Integer maxSelect, Integer sortOrder, Boolean isActive) {
        this.menuId = menuId;
        this.name = name;
        this.isRequired = (isRequired != null) ? isRequired : false;
        this.minSelect = (minSelect != null) ? minSelect : 0;
        this.maxSelect = (maxSelect != null) ? maxSelect : 1;
        this.sortOrder = (sortOrder != null) ? sortOrder : 0;
        this.isActive = (isActive != null) ? isActive : true;
    }

    /** 부분 수정 — null 필드는 변경하지 않는다. */
    public void updateInfo(String name, Integer minSelect, Integer maxSelect, Integer sortOrder) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (minSelect != null) {
            this.minSelect = minSelect;
        }
        if (maxSelect != null) {
            this.maxSelect = maxSelect;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    public void changeRequired(Boolean isRequired) {
        if (isRequired != null) {
            this.isRequired = isRequired;
        }
    }

    public void changeActive(Boolean isActive) {
        if (isActive != null) {
            this.isActive = isActive;
        }
    }
}
