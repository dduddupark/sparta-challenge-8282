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
     * ACTIVE 상태가 된 가게만 일반 유저에게 노출한다.
     */
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


}
