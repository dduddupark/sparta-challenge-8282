package com.sparta.spartachallenge8282.store.domain;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.global.common.BaseEntity;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "p_store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Store extends BaseEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(name = "store_tel", nullable = false, length = 20)
    private String storeTel;

    //추후 이미지 설정
    @Column(name = "store_image")
    private String storeImage;

    @Column(nullable = false)
    private String address;

    @Column(name = "min_order_price", nullable = false)
    private Integer minOrderPrice;

    @Column(name = "delivery_fee", nullable = false)
    private Integer deliveryFee;

    @Column(name = "free_delivery_amount")
    private Integer freeDeliveryAmount;

    @Column(name = "store_rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal storeRating;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(name = "is_open", nullable = false)
    private boolean isOpen;

    @Enumerated(EnumType.STRING)
    @Column(name = "store_status", nullable = false)
    private StoreStatus storeStatus;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Builder
    private Store(
            User owner,
            Category category,
            Region region,
            String storeName,
            String storeTel,
            String storeImage,
            String address,
            Integer minOrderPrice,
            Integer deliveryFee,
            Integer freeDeliveryAmount,
            LocalTime openTime,
            LocalTime closeTime
    ) {
        this.owner = owner;
        this.category = category;
        this.region = region;
        this.storeName = storeName;
        this.storeTel = storeTel;
        this.storeImage = storeImage;
        this.address = address;
        this.minOrderPrice = minOrderPrice;
        this.deliveryFee = deliveryFee;
        this.freeDeliveryAmount = freeDeliveryAmount;
        this.openTime = openTime;
        this.closeTime = closeTime;

        // 기본값
        this.storeRating = BigDecimal.ZERO;
        this.reviewCount = 0;
        this.isOpen = false;
        this.storeStatus = StoreStatus.PENDING;

        this.approvedAt = null;
        this.rejectedAt = null;
        this.rejectionReason = null;
    }

    /**
     * 가게 등록 신청 승인
     */
    public void approve() {
        this.storeStatus = StoreStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();

        this.rejectedAt = null;
        this.rejectionReason = null;
    }

    /**
     * 가게 등록 신청 거절
     */
    public void reject(String rejectionReason) {
        this.storeStatus = StoreStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        this.rejectionReason = rejectionReason;

        this.approvedAt = null;
    }

    public void changeOpenStatus(boolean isOpen) {
        this.isOpen = isOpen;
    }

    /**
     * 승인 또는 거절은 PENDING 상태에서만 가능
     */
    private void validateStoreStatus() {
        if(this.storeStatus != StoreStatus.PENDING){
            throw new CustomException(ErrorCode.INVALID_STORE_STATUS);
        }
    }


}