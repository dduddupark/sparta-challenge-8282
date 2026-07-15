package com.sparta.spartachallenge8282.review_reply.presentation.dto.response;

import com.sparta.spartachallenge8282.review_reply.domain.ReviewReply;
import org.springframework.data.domain.Slice;

import java.util.List;

/**
 * 답글 슬라이스 응답 DTO.
 * 가게별 답글 목록을 페이징(Slice) 처리한 응답 데이터
 */
public record ReviewReplySliceResponseDto(
        List<ReviewReplyListItemResponseDto> content,
        boolean hasNext
) {
    public static ReviewReplySliceResponseDto from(Slice<ReviewReply> slice) {
        List<ReviewReplyListItemResponseDto> content = slice.getContent().stream()
                .map(ReviewReplyListItemResponseDto::from)
                .toList();

        return new ReviewReplySliceResponseDto(content, slice.hasNext());
    }
}
