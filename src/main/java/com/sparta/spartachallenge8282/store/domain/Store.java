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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "p_store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseEntity {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * 어떤 신청을 통해 생성된 가게인지 기록
     *
     * 한 신청으로 가게가 두 번 생성되지 않도록 unique 설정
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "store_application_id",
            nullable = false,
            unique = true
    )
    private StoreApplication storeApplication;

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

    /**
     * 승인 후 실제 가게의 운영 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "operation_status",
            nullable = false,
            length = 30
    )
    private StoreOperationStatus operationStatus;

    @Builder
    private Store(
            StoreApplication storeApplication,
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
        this.storeApplication = storeApplication;
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
        /*
         * 승인 직후에는 운영 준비 상태이며
         * 고객에게 노출되지 않고 주문도 받지 않는다.
         */
        this.operationStatus = StoreOperationStatus.PREPARING;
        this.isOpen = false;
    }

    /**
     * 승인된 신청서로 실제 store를 생성
     */
    public static Store from(StoreApplication application) {
        return Store.builder()
                .storeApplication(application)
                .owner(application.getApplicant())
                .category(application.getCategory())
                .region(application.getRegion())
                .storeName(application.getStoreName())
                .storeTel(application.getStoreTel())
                .storeImage(application.getStoreImage())
                .address(application.getAddress())
                .minOrderPrice(application.getMinOrderPrice())
                .deliveryFee(application.getDeliveryFee())
                .freeDeliveryAmount(application.getFreeDeliveryAmount())
                .openTime(application.getOpenTime())
                .closeTime(application.getCloseTime())
                .build();
    }

    public void update(
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
            LocalTime closeTime)
    {
        if (category != null) {
            this.category = category;
        }

        if (region != null) {
            this.region = region;
        }

        if (storeName != null && !storeName.isBlank()) {
            this.storeName = storeName;
        }

        if (storeTel != null && !storeTel.isBlank()) {
            this.storeTel = storeTel;
        }

        if (storeImage != null) {
            this.storeImage = storeImage;
        }

        if (address != null && !address.isBlank()) {
            this.address = address;
        }

        if (minOrderPrice != null) {
            this.minOrderPrice = minOrderPrice;
        }

        if (deliveryFee != null) {
            this.deliveryFee = deliveryFee;
        }

        if (freeDeliveryAmount != null) {
            this.freeDeliveryAmount = freeDeliveryAmount;
        }

        if (openTime != null) {
            this.openTime = openTime;
        }

        if (closeTime != null) {
            this.closeTime = closeTime;
        }
    }

    public void activate(){
        this.operationStatus = StoreOperationStatus.ACTIVE;
    }


    public void changeOpenStatus(boolean isOpen) {
        this.isOpen = isOpen;
    }


    public void requestDelete(){
        this.operationStatus = StoreOperationStatus.CLOSE_REQUESTED;
        this.isOpen = false;
    }

    public void approveDelete(Long adminId) {
        this.operationStatus = StoreOperationStatus.CLOSED;
        this.isOpen = false;
        softDelete(adminId);
    }
}