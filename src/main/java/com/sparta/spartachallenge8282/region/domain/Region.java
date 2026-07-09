package com.sparta.spartachallenge8282.region.domain;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "p_region")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseEntity {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private boolean isServiceAvailable;

    // 파라미터는 wrapper(Integer/Boolean) — 미입력(null)이면 기본값으로 채우기 위함. 필드는 not-null이라 primitive.
    @Builder
    public Region(String name, Integer sortOrder, Boolean isActive, Boolean isServiceAvailable) {
        this.name = name;
        this.sortOrder = (sortOrder != null) ? sortOrder : 0;
        this.isActive = (isActive != null) ? isActive : true;
        this.isServiceAvailable = (isServiceAvailable != null) ? isServiceAvailable : false;
    }

    public void changeSortOrder(Integer sortOrder) {
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    public void updateName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public void changeActive(Boolean isActive) {
        if (isActive != null) {
            this.isActive = isActive;
        }
    }
    public void changeServiceAvailable(Boolean isServiceAvailable) {
        if (isServiceAvailable != null) {
            this.isServiceAvailable = isServiceAvailable;
        }
    }

}
