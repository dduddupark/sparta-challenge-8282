package com.sparta.spartachallenge8282.store.domain;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.global.common.BaseEntity;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "p_store_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreApplication extends BaseEntity {

    @Id
    @UuidGenerator
    protected UUID id;

    /**
     * 가게 등록 신청자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

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

    @Column(name = "store_image")
    private String storeImage;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "min_order_price", nullable = false)
    private Integer minOrderPrice;

    @Column(name = "delivery_fee", nullable = false)
    private Integer deliveryFee;

    @Column(name = "free_delivery_amount")
    private Integer freeDeliveryAmount;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreApplicationStatus status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;


    @Builder
    private StoreApplication(
            User applicant,
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
    ){
        this.applicant = applicant;
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

        this.status = StoreApplicationStatus.PENDING;
    }


    /**
     * 가게 등록 신청 승인
     */
    public void approve() {
        this.status = StoreApplicationStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();

        this.rejectedAt = null;
        this.rejectionReason = null;
    }

    /**
     * 가게 등록 신청 거절
     */
    public void reject(String rejectionReason) {
        this.status = StoreApplicationStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        this.rejectionReason = rejectionReason;

        this.approvedAt = null;
    }

}
