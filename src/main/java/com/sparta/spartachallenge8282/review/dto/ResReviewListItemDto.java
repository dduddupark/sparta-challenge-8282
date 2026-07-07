package com.sparta.spartachallenge8282.review.dto;


import com.sparta.spartachallenge8282.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder    // 중요한 정보(user_id 등)이 들어가지 않으므로 클래스에 선언하여 사용함.
public class ResReviewListItemDto {

    private UUID reviewId;
    private String userNickname;
    private Integer rating;
    private String content;
    private String imageUrl;
    private Object reply; // 임시 답글 객체
    private LocalDateTime createdAt;

    public static ResReviewListItemDto from(Review review) {
        return ResReviewListItemDto.builder()
                .reviewId(review.getId())
                .userNickname(null)
                .rating(review.getRating())
                .content(review.getContent())
                .imageUrl(review.getImageUrl())
                .reply(null)
                .createdAt(review.getCreatedAt())
                .build();
    }

}
