package com.sparta.spartachallenge8282.store.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {


    /**
     * OWNER 본인의 가게 목록 조회
     */
    @EntityGraph(attributePaths = {
            "category"
    })
    Page<Store> findAllByOwnerIdAndDeletedAtIsNull(
            Long ownerId,
            Pageable pageable
    );

    /**
     * OWNER 본인의 가게 상세 조회
     */
    @EntityGraph(attributePaths = {
            "owner",
            "category",
            "region"
    })
    Optional<Store> findByIdAndOwner_IdAndDeletedAtIsNull(
            UUID storeId,
            Long ownerId
    );

    /**
     * 해당 가게가 이 OWNER 소유인지 존재 검증.
     *
     * <p>결제 도메인의 OWNER 가게 스코프 권한 검증에 쓰인다
     * (본인 가게 결제만 조회/취소하도록 — {@code payment.order.storeId} 대조).
     * 엔티티 로딩 없이 존재 여부만 확인하는 경량 쿼리.
     */
    boolean existsByIdAndOwner_IdAndDeletedAtIsNull(
            UUID storeId,
            Long ownerId
    );


    /**
     * ACTIVE 상태가 된 가게만 일반 유저에게 노출한다.
     */
    //전체 조회
    Page<Store> findAllByDeletedAtIsNull(Pageable pageable);

    //상태에 따른 조회
    @EntityGraph(attributePaths = {
            "category",
            "region"
    })
    Page<Store> findAllByOperationStatusAndDeletedAtIsNull(
            StoreOperationStatus operationStatus,
            Pageable pageable
    );

    /**
     * ACTIVE 상태가 된 가게만 일반 유저에게 노출한다.
     */
    @EntityGraph(attributePaths = {
            "owner",
            "category",
            "region"
    })
    Optional<Store> findByIdAndOperationStatusAndDeletedAtIsNull(
            UUID storeId,
            StoreOperationStatus operationStatus
    );

    boolean existsByCategory_IdAndDeletedAtIsNull(UUID categoryId);

    boolean existsByRegion_IdAndDeletedAtIsNull(UUID regionId);

    Optional<Store> findByIdAndDeletedAtIsNull(UUID storeId);
}
