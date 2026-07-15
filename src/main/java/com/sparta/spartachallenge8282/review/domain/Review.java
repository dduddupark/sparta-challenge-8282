package com.sparta.spartachallenge8282.review.domain;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewCreateRequestDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 배달 주문에 대한 리뷰 엔티티.
 * 주문(Order)과 1:1 관계다 - deletedAt이 NULL인 행 사이에서만 orderId가 유니크하도록
 * DB partial index(uq_review_order_id_active)로 강제한다 (soft delete 후 재작성 허용을 위함).
 * 자세한 배경은 review 패키지 내 SQL 기록 파일 참고.
 * 한 주문에 메뉴가 여러 개 담겨도 리뷰는 주문에 대한 것이다. (메뉴 단위가 아님)
 * storeId는 리뷰 작성 시 클라이언트가 보내지 않고, orderId로 조회한 Order에서 추출해 저장한다.
 * 가게 목록 조회 시 /stores/{storeId}/reviews로 바로 필터링할 수 있게 하기 위한 비정규화 컬럼이다.
 * 상태 변경은 update() / BaseEntity.softDelete()로만 수행한다.
 */

@Entity
@Table(name = "p_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    // 리뷰 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 주문 ID
    @Column(nullable = false)
    private UUID orderId;

    // 유저 ID
    @Column(nullable = false)
    private Long userId;

    // 가게 ID
    @Column(nullable = false)
    private UUID storeId;

    // 평점
    @Column(nullable = false)
    private Integer rating;

    // 내용
    private String content;

    // 이미지 -> 현재는 한 개만 등록되도록 구현, 여러 장 일 시 수정필요
    private String imageUrl;

    @Builder
    private Review(ReviewCreateRequestDto requestDto, Long userId, UUID storeId) {
        this.orderId = requestDto.orderId();
        this.userId = userId;
        this.storeId = storeId;
        this.rating = requestDto.rating();
        this.content = requestDto.content();
        this.imageUrl = requestDto.imageUrl();
    }

    // 리뷰 수정 - null인 필드는 기존 값 유지
    public void update(Integer rating, String content, String imageUrl) {
        if (rating != null) {
            this.rating = rating;
        }
        if (content != null) {
            this.content = content;
        }
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
    }
}
