package com.sparta.spartachallenge8282.review_reply.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 답글 작성/수정 공용 요청 DTO.
 *
 * 필드가 content 하나뿐이라 작성(POST)과 수정(PATCH)에 동일하게 사용한다.
 * 답글 내용은 명세상 필수값이라 @NotBlank 로 빈 값·공백만 있는 값을 막는다.
 */

public record ReviewReplyRequestDto (
        @NotBlank(message = "답글 내용은 필수 입니다.")
        @Size(max = 500, message = "답글은 500자를 초과할 수 없습니다.")
        String content
    ){
}
