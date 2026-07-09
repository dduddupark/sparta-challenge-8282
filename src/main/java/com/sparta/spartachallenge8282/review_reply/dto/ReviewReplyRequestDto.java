package com.sparta.spartachallenge8282.review_reply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 리뷰 답글 생성 요청 DTO
 *
 * */

public record ReviewReplyRequestDto (
        @NotBlank(message = "답글 내용은 필수 입니다.")
        @Size(max = 500, message = "답글은 500자를 초과할 수 없습니다.")
        String content
    ){
}
