package com.sparta.spartachallenge8282.review.domain;

import com.sparta.spartachallenge8282.global.config.JpaAuditingConfig;
import com.sparta.spartachallenge8282.global.config.QueryDslConfig;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewCreateRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest   // JPA 관련 빈만 로드, 인메모리 DB(H2) 사용, 각 테스트 후 롤백
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)@Import({
        JpaAuditingConfig.class,
        QueryDslConfig.class
})
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    @DisplayName("existsByOrderIdAndDeletedAtIsNull: 저장된 주문ID로 조회하면 true")
    void existsByOrderIdAndDeletedAtIsNullTest_true() {
        // given
        UUID orderId = UUID.randomUUID();
        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(orderId, 5, "맛있어요", null))
                .userId(1L)
                .storeId(UUID.randomUUID())
                .build();
        reviewRepository.save(review);

        // when
        boolean exists = reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId);
        System.out.println("결과: existsByOrderIdAndDeletedAtIsNull(" + orderId + ") = " + exists);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByOrderIdAndDeletedAtIsNull: 저장 안 된 주문ID로 조회하면 false")
    void existsByOrderIdAndDeletedAtIsNullTest_false() {
        // when
        UUID orderId = UUID.randomUUID();
        boolean exists = reviewRepository.existsByOrderIdAndDeletedAtIsNull(UUID.randomUUID());
        System.out.println("결과: existsByOrderIdAndDeletedAtIsNull(" + orderId + ") = " + exists);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByStoreIdAndDeletedAtIsNull: 삭제 안 된 리뷰만 조회")
    void findByStoreIdAndDeletedAtIsNullTest() {
        // given
        UUID storeId = UUID.randomUUID();

        Review activeReview = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "정상 리뷰", null))
                .userId(1L)
                .storeId(storeId)
                .build();
        reviewRepository.save(activeReview);

        Review deletedReview = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 3, "삭제될 리뷰", null))
                .userId(2L)
                .storeId(storeId)
                .build();
        reviewRepository.save(deletedReview);
        deletedReview.softDelete(2L);   // soft delete 처리
        reviewRepository.save(deletedReview);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Slice<Review> result = reviewRepository.findByStoreIdAndDeletedAtIsNull(storeId, pageable);
        System.out.println("결과: " + result.getContent());

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("정상 리뷰");
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull: 삭제되지 않은 리뷰는 조회됨")
    void findByIdAndDeletedAtIsNullTest_found() {
        // given
        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "조회될 리뷰", null))
                .userId(1L)
                .storeId(UUID.randomUUID())
                .build();
        Review saved = reviewRepository.save(review);

        // when
        Optional<Review> result = reviewRepository.findByIdAndDeletedAtIsNull(saved.getId());
        System.out.println("결과: " + result);

        // then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull: 삭제된 리뷰는 조회 안 됨")
    void findByIdAndDeletedAtIsNullTest_notFound_afterDelete() {
        // given
        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(UUID.randomUUID(), 5, "삭제될 리뷰", null))
                .userId(1L)
                .storeId(UUID.randomUUID())
                .build();
        Review saved = reviewRepository.save(review);
        saved.softDelete(1L);
        reviewRepository.save(saved);

        // when
        Optional<Review> result = reviewRepository.findByIdAndDeletedAtIsNull(saved.getId());
        System.out.println("결과: " + result);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByOrderIdAndDeletedAtIsNull: 삭제된 리뷰는 false (재작성 가능해야 함)")
    void existsByOrderIdAndDeletedAtIsNullTest_false_afterDelete() {
        // given
        UUID orderId = UUID.randomUUID();
        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(orderId, 5, "삭제될 리뷰", null))
                .userId(1L)
                .storeId(UUID.randomUUID())
                .build();
        Review saved = reviewRepository.save(review);
        saved.softDelete(1L);
        reviewRepository.save(saved);

        // when
        boolean exists = reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId);
        System.out.println("결과: existsByOrderIdAndDeletedAtIsNull(" + orderId + ") = " + exists);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("삭제된 리뷰가 있는 주문으로 재작성해도 실제 저장까지 성공한다 (partial unique index 검증)")
    void reCreateReviewAfterSoftDelete_shouldSaveSuccessfully() {
        // given
        UUID orderId = UUID.randomUUID();
        Review review = Review.builder()
                .requestDto(new ReviewCreateRequestDto(orderId, 5, "첫 리뷰", null))
                .userId(1L)
                .storeId(UUID.randomUUID())
                .build();
        Review saved = reviewRepository.save(review);
        saved.softDelete(1L);
        reviewRepository.saveAndFlush(saved);

        System.out.println("=== 삭제 완료: orderId=" + orderId + ", deletedAt=" + saved.getDeletedAt() + " ===");

        // when
        Review newReview = Review.builder()
                .requestDto(new ReviewCreateRequestDto(orderId, 4, "다시 쓴 리뷰", null))
                .userId(1L)
                .storeId(UUID.randomUUID())
                .build();

        try {
            Review result = reviewRepository.saveAndFlush(newReview);
            System.out.println("=== 재작성 성공! 새 리뷰 ID: " + result.getId() + " ===");
        } catch (Exception e) {
            System.out.println("=== 재작성 실패! 예외 발생: " + e.getClass().getSimpleName() + " ===");
            System.out.println("=== 메시지: " + e.getMessage() + " ===");
            throw e;   // 테스트가 실패로 표시되도록 다시 던짐
        }
    }
}