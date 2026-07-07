package com.sparta.spartachallenge8282.global.common;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 엔티티의 공통 베이스 클래스.
 *
 * <p>Spring Data JPA Auditing으로 자동 관리되는 필드:
 * <ul>
 *   <li>createdAt  - 생성 시각 (not null, 이후 변경 불가)</li>
 *   <li>createdBy  - 생성한 사용자 ID (not null, 이후 변경 불가)</li>
 *   <li>updatedAt  - 마지막 수정 시각</li>
 *   <li>updatedBy  - 마지막 수정한 사용자 ID</li>
 * </ul>
 *
 * <p>소프트 딜리트 필드 (서비스에서 직접 설정):
 * <ul>
 *   <li>deletedAt  - 삭제 시각 (null 이면 미삭제)</li>
 *   <li>deletedBy  - 삭제한 사용자 ID</li>
 * </ul>
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 생성 정보 ──────────────────────────────────────────────────────────────

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(nullable = false, updatable = false)
    private Long createdBy;

    // ── 수정 정보 ──────────────────────────────────────────────────────────────

    @LastModifiedDate
    @Column
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column
    private Long updatedBy;

    // ── 소프트 딜리트 ──────────────────────────────────────────────────────────

    @Column
    private LocalDateTime deletedAt;

    @Column
    private Long deletedBy;

    /**
     * 소프트 딜리트 처리.
     * @param userId 삭제를 요청한 사용자 ID
     */
    public void softDelete(Long userId) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = userId;
    }

    /** 삭제 여부 확인 */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
