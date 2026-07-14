package com.sparta.spartachallenge8282.ai_history.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 메뉴 설명 생성 요청/응답 이력 로그.
 *
 * <로그성 테이블이라 BaseEntity를 상속하지 않고, 생성 시각만
 * @CreationTimestamp로 관리한다. soft delete나
 * 수정 이력 추적이 필요 없는 순수 기록용 엔티티이기 때문이다.
 *
 * Gemini 호출이 실패해도 이 이력은 남는다 - response는 null,
 * isSuccess는 false로 저장되어 "실패했다는 사실" 자체가 기록된다.
 */

@Entity
@Table(name="p_ai_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiHistory {

    // AI호출 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 메뉴 ID (메뉴를 먼저 저장 후 AI를 호출)
    @Column(nullable = false)
    private UUID menuId;

    // 요청한 유저 ID
    @Column(nullable = false)
    private Long requestedBy;

    // 요청 프롬프트 (자동/수동 여부와 무관하게 최종 프롬프트 그대로 저장)
    @Column(nullable = false, length = 1000)
    private String prompt;

    // AI 응답 (실패 시 null)
    @Column(length = 1000)
    private String response;

    // AI 호출 성공 여부
    @Column(nullable = false)
    private boolean isSuccess;

    // 생성 시각 -> BaseEntity를 상속하지 않아 직접 관리
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private AiHistory(UUID menuId, Long requestedBy, String prompt, String response, boolean isSuccess) {
        this.menuId = menuId;
        this.requestedBy = requestedBy;
        this.prompt = prompt;
        this.response = response;
        this.isSuccess = isSuccess;
    }
}
