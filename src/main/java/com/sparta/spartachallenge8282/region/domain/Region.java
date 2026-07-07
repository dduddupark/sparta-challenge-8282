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
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private Boolean isServiceAvailable;

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

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void openService() {
        this.isServiceAvailable = true;
    }

    public void closeService() {
        this.isServiceAvailable = false;
    }

}
