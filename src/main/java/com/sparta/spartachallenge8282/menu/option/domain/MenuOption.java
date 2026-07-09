package com.sparta.spartachallenge8282.menu.option.domain;

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
 * 옵션 그룹에 속한 실제 선택 옵션 (예: 콜라, 사이다, 감자튀김).
 *
 * <p>옵션 그룹에 종속된다. 그룹 존재는 Service 에서 검증하고 {@code optionGroupId} 를 단순 UUID 로 참조한다.
 * {@code additional_price >= 0} 은 DB {@code @Check} + Service 검증(INVALID_OPTION_PRICE)로 방어한다.
 */
@Entity
@Table(name = "p_menu_option")
@Check(constraints = "additional_price >= 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuOption extends BaseEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "option_group_id", nullable = false)
    private UUID optionGroupId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int additionalPrice;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean isActive;

    @Builder
    public MenuOption(UUID optionGroupId, String name, Integer additionalPrice, Integer sortOrder, Boolean isActive) {
        this.optionGroupId = optionGroupId;
        this.name = name;
        this.additionalPrice = (additionalPrice != null) ? additionalPrice : 0;
        this.sortOrder = (sortOrder != null) ? sortOrder : 0;
        this.isActive = (isActive != null) ? isActive : true;
    }

    /** 부분 수정 — null 필드는 변경하지 않는다. */
    public void updateInfo(String name, Integer additionalPrice, Integer sortOrder) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (additionalPrice != null) {
            this.additionalPrice = additionalPrice;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    public void changeActive(Boolean isActive) {
        if (isActive != null) {
            this.isActive = isActive;
        }
    }
}
