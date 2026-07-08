package com.sparta.spartachallenge8282.review.entity;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import com.sparta.spartachallenge8282.review.dto.request.ReviewCreateRequestDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
    @Column(nullable = false, unique = true)
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
