package com.sparta.spartachallenge8282.store.domain;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface StoreApplicationRepository extends JpaRepository<StoreApplication, UUID> {

    //-----------------------------
    /**
     * 등록 신청한 본인 가게 목록 조회
     */
    @EntityGraph(attributePaths = {
           "applicant"
    })
    Page<StoreApplication> findAllByApplicant_Id(Long applicantId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "category", "region"
    })
    Page<StoreApplication> findAllByApplicant_IdAndStatus(
            Long applicantId,
            StoreApplicationStatus status,
            Pageable pageable
    );

    /**
     * 등록 신청한 본인 가게 상세 조회
     */
    @EntityGraph(attributePaths = {
            "applicant",
            "category",
            "region"
    })
    Optional<StoreApplication> findByIdAndApplicant_Id(UUID applicationId, Long userId);



    //-------------------------
    /**
     * 관리자의 상태별 등록 신청 목록 조회
     */
    @EntityGraph(attributePaths = {
            "applicant"
    })
    Page<StoreApplication> findAllByStatus(StoreApplicationStatus status, Pageable pageable);

    /**
     * 관리자의 등록 신청 상세 조회
     */
    @Override
    @EntityGraph(attributePaths = {
            "applicant",
            "category",
            "region"
    })
    Optional<StoreApplication> findById(UUID applicationId);


}
