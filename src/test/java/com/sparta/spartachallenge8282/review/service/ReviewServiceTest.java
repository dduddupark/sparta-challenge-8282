package com.sparta.spartachallenge8282.review.service;

import com.sparta.spartachallenge8282.review.dto.request.ReviewCreateRequestDto;
import com.sparta.spartachallenge8282.review.dto.response.ReviewResultResponseDto;
import com.sparta.spartachallenge8282.review.entity.Review;
import com.sparta.spartachallenge8282.review.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)   // Mockito 사용 선언
class ReviewServiceTest {

    @Mock   // 가짜 Repository (실제 DB 연결 안 함)
    private ReviewRepository reviewRepository;

    @InjectMocks   // 위의 Mock들을 자동으로 주입받는 진짜 테스트 대상
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReviewTest() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long userId = 1L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(
                orderId, 5, "정말 맛있었어요!", null
        );

        Review savedReview = Review.builder()
                .requestDto(requestDto)
                .userId(userId)
                .storeId(storeId)
                .build();

        // 테스트용으로 강제로 id 채워넣기 (실제 DB 저장 시뮬레이션)
        UUID fakeReviewId = UUID.randomUUID();
        ReflectionTestUtils.setField(savedReview, "id", fakeReviewId);

        when(reviewRepository.save(any(Review.class)))
                .thenReturn(savedReview);

        // when
        ReviewResultResponseDto result = reviewService.createReview(requestDto, userId, storeId);

        System.out.println("결과: " + result);

        // then
        assertThat(result).isNotNull();
        assertThat(result.reviewId()).isEqualTo(fakeReviewId);   // savedReview.getId() 대신 fakeReviewId로 비교
    }


}