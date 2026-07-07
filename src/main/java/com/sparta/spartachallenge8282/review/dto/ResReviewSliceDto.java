package com.sparta.spartachallenge8282.review.dto;

import com.sparta.spartachallenge8282.review.entity.Review;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ResReviewSliceDto {

    private List<ResReviewListItemDto> content;
    private boolean hasNext;

    public static ResReviewSliceDto from(Slice<Review> slice) {
        List<ResReviewListItemDto> content = slice.getContent().stream()
                .map(ResReviewListItemDto::from)
                .collect(Collectors.toList());

        return ResReviewSliceDto.builder()
                .content(content)
                .hasNext(slice.hasNext())
                .build();
    }

}
