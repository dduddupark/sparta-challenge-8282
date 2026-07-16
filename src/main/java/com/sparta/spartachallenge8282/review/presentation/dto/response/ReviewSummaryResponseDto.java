package com.sparta.spartachallenge8282.review.presentation.dto.response;

public record ReviewSummaryResponseDto(
        String summary
) {
    public static ReviewSummaryResponseDto from(String summary) {
        return new ReviewSummaryResponseDto(summary);
    }
}