package com.sparta.spartachallenge8282.review.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ResReviewResultDto {

    private final UUID reviewId;

    @Builder
    public ResReviewResultDto(UUID reviewId) {
        this.reviewId = reviewId;
    }

    public static ResReviewResultDto from(UUID reviewId) {
        return ResReviewResultDto.builder()
                .reviewId(reviewId)
                .build();
    }

}
