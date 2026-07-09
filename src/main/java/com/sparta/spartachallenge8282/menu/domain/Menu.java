package com.sparta.spartachallenge8282.menu.domain;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 가게의 판매 메뉴 엔티티.
 *
 * <p>메뉴는 가게(store)에 종속된다. Store 엔티티가 아직 없어 {@code storeId} 를 단순 UUID 컬럼으로
 * 선개발한다 — 컬럼명이 동일하므로 이후 {@code @ManyToOne} 연관관계로 전환해도 스키마 변경이 없다.
 *
 * <p>가격 음수 방어는 3중이다: DTO {@code @Min(0)} → Service {@code INVALID_MENU_PRICE}
 * → DB {@code @Check(price >= 0)}. {@code ddl-auto=update} 는 기존 테이블에 CHECK 를 추가하지 못하므로
 * 이 제약은 테이블 최초 생성 시점에 반영되어야 한다.
 *
 * <p>{@code is_hidden}(고객 노출 토글)과 {@code deleted_at}(소프트 삭제)은 별개 개념이다.
 */
@Entity
@Table(name = "p_menu")
@Check(constraints = "price >= 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseEntity {

    @Id
    @UuidGenerator
    private UUID id;

    // order 접점: 주문 생성 시 "주문한 메뉴가 이 가게 소속인지" 검증에 사용된다 (order 도메인의 MENU_STORE_MISMATCH).
    //            메뉴 쓰기 권한(OWNER 본인 가게)도 이 storeId 로 소유권을 확인한다 (NO_MENU_PERMISSION, auth 브랜치).
    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // order 접점: 주문 시점 가격을 OrderItem.menuPrice(스냅샷)로 복사한다 — 이후 가격이 바뀌어도 과거 주문 내역은 불변.
    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MenuStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MenuBadge badge;

    // order 접점: 숨김(true) 메뉴는 주문할 수 없다 (order 도메인의 HIDDEN_MENU_NOT_ORDERABLE). 노출 토글일 뿐 삭제(deletedAt)와는 별개.
    @Column(nullable = false)
    private boolean isHidden;

    @Column(nullable = false)
    private boolean isAiGenerated;

    // 파라미터는 wrapper — 기본값이 있는 필드(sortOrder/status/badge/플래그)는 미입력(null) 시 기본값으로 채운다.
    // storeId/name/price 는 필수값이라 기본값 없이 그대로 대입한다 (null/음수 검증은 DTO·Service 책임).
    @Builder
    public Menu(UUID storeId,
                String name,
                String description,
                Integer price,
                Integer sortOrder,
                MenuStatus status,
                MenuBadge badge,
                Boolean isHidden,
                Boolean isAiGenerated) {
        this.storeId = storeId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.sortOrder = (sortOrder != null) ? sortOrder : 0;
        this.status = (status != null) ? status : MenuStatus.ON_SALE;
        this.badge = (badge != null) ? badge : MenuBadge.NONE;
        this.isHidden = (isHidden != null) ? isHidden : false;
        this.isAiGenerated = (isAiGenerated != null) ? isAiGenerated : false;
    }

    /** 메뉴 기본 정보 수정 (부분 수정 — null 인 필드는 변경하지 않는다). */
    public void updateInfo(String name, String description, Integer price, Integer sortOrder) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (price != null) {
            this.price = price;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    public void changeStatus(MenuStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public void changeBadge(MenuBadge badge) {
        if (badge != null) {
            this.badge = badge;
        }
    }

    /** 고객 화면에서 숨김 처리. */
    public void hide() {
        this.isHidden = true;
    }

    /** 고객 화면에 다시 노출. */
    public void show() {
        this.isHidden = false;
    }
}
