package com.sparta.spartachallenge8282.ai_history.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

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


    // 요청 프롬프트
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
