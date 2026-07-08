package com.sparta.spartachallenge8282.review.dto.response;

import com.sparta.spartachallenge8282.review.entity.Review;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.stream.Collectors;
/**
 * 리뷰 슬라이스 응답 DTO.
 * 가게별 리뷰 목록을 페이징(Slice) 처리한 응답 데이터
 */

public record ReviewSliceResponseDto(
        List<ReviewListItemResponseDto> content,
        boolean hasNext
) {

    public static ReviewSliceResponseDto from(Slice<Review> slice) {
        List<ReviewListItemResponseDto> content = slice.getContent().stream()
                .map(ReviewListItemResponseDto::from)
                .collect(Collectors.toList());

        return new ReviewSliceResponseDto(
                content,
                slice.hasNext()
        );
    }
}
