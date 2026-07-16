package com.sparta.spartachallenge8282.store.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID>, StoreRepositoryCustom {

    /**
     * 관리자는 삭제된 가게까지 조회
     */
    @Override
    @EntityGraph(attributePaths = {
            "category",
            "region"
    })
    Page<Store> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
            "category",
            "region"
    })
    Page<Store> findAllByOperationStatus(
            StoreOperationStatus operationStatus,
            Pageable pageable
    );

    /**
     * 관리자 상세 조회 - 삭제된 가게도 조회 가능해야 하므로 findById를 EntityGraph로 오버라이드
     */
    @Override
    @EntityGraph(attributePaths = {
            "owner",
            "category",
            "region"
    })
    Optional<Store> findById(UUID storeId);
    //--------------------------------------------------
    /**
     * OWNER 본인의 가게 목록 조회
     */
    @EntityGraph(attributePaths = {
            "category",
            "region"
    })
    Page<Store> findAllByOwner_IdAndDeletedAtIsNull(
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
     * OWNER 본인의 가게 목록을 상태별로 조회.
     * deletedAt 필터가 있으므로 CLOSED(삭제됨)는 자동으로 제외된다.
     */
    @EntityGraph(attributePaths = {
            "category",
            "region"
    })
    Page<Store> findAllByOwner_IdAndOperationStatusAndDeletedAtIsNull(
            Long ownerId,
            StoreOperationStatus operationStatus,
            Pageable pageable
    );
    //-------------------------------------

    /**
     * 가게 상세
     * ACTIVE 상태가 된 가게만 일반 유저에게 노출한다.
     */
    @EntityGraph(attributePaths = {
            "category",
            "region"
    })
    Optional<Store> findByIdAndOperationStatusAndDeletedAtIsNull(
            UUID storeId,
            StoreOperationStatus operationStatus
    );



    /**
     * 해당 가게가 이 OWNER 소유인지 존재 검증.
     *
     * <p>결제/메뉴/옵션 도메인의 OWNER 가게 스코프 권한 검증에 쓰인다
     * (본인 가게 결제만 조회/취소하도록 — {@code payment.order.storeId} 대조).
     * 엔티티 로딩 없이 존재 여부만 확인하는 경량 쿼리.
     */
    boolean existsByIdAndOwner_IdAndDeletedAtIsNull(
            UUID storeId,
            Long ownerId
    );

    /**
     * 삭제되지 않은 가게 존재 여부 검증.
     *
     * <p>메뉴/옵션 쓰기 권한 검증에서 먼저 가게 존재 여부를 확인할 때 사용한다.
     * 엔티티 로딩 없이 존재 여부만 확인하는 경량 쿼리.
     */
    boolean existsByIdAndDeletedAtIsNull(UUID storeId);






    boolean existsByCategory_IdAndDeletedAtIsNull(UUID categoryId);

    boolean existsByRegion_IdAndDeletedAtIsNull(UUID regionId);

    Optional<Store> findByIdAndDeletedAtIsNull(UUID storeId);
}
