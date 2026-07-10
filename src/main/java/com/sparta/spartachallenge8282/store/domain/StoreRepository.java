package com.sparta.spartachallenge8282.store.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    @EntityGraph(attributePaths = {
            "category"
    })
    Page<Store> findAllByOwner_Id(
            Long ownerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "owner",
            "category",
            "region"
    })
    Optional<Store> findByIdAndOwner_Id(
            UUID storeId,
            Long ownerId
    );

    @Override
    @EntityGraph(attributePaths = {
            "owner",
            "category",
            "region"
    })
    Page<Store> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
            "owner",
            "category",
            "region"
    })
    Page<Store> findAllByStoreStatus(
            StoreStatus storeStatus,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = {
            "owner",
            "category",
            "region"
    })
    Optional<Store> findById(UUID storeId);
}
