package com.sparta.spartachallenge8282.review.dto;

import com.sparta.spartachallenge8282.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ResReviewDto {

    private UUID reviewId;
    private UUID storeId;
    private Integer rating;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;

    public static ResReviewDto from(Review review) {
        return ResReviewDto.builder()
                .reviewId(review.getId())
                .storeId(review.getStoreId())
                .rating(review.getRating())
                .content(review.getContent())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt()) // BaseEntity 필드명 확인 필요!
                .build();
    }

}
