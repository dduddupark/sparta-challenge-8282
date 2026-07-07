package com.sparta.spartachallenge8282.review.entity;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import com.sparta.spartachallenge8282.review.dto.ReqCreateReviewDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "p_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private UUID storeId;

    @Column(nullable = false)
    private Integer rating;

    private String content;

    private String imageUrl;


    @Builder
    private Review(ReqCreateReviewDto requestDto, Long userId, UUID storeId) {
        this.orderId = requestDto.getOrderId();
        this.userId = userId;
        this.storeId = storeId;
        this.rating = requestDto.getRating();
        this.content = requestDto.getContent();
        this.imageUrl = requestDto.getImageUrl();
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
