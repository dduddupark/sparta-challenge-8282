package com.sparta.spartachallenge8282.review_reply.domain;

import com.sparta.spartachallenge8282.global.config.JpaAuditingConfig; // 실제 패키지 경로로 확인 필요
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ReviewReplyRepositoryTest {

    @Autowired
    private ReviewReplyRepository reviewReplyRepository;

    @Test
    @DisplayName("existsByReviewIdAndDeletedAtIsNull: 저장된 답글이면 true")
    void existsByReviewIdAndDeletedAtIsNullTest_true() {
        UUID reviewId = UUID.randomUUID();
        ReviewReply reply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(UUID.randomUUID())
                .content("답글입니다")
                .build();
        reviewReplyRepository.save(reply);

        boolean exists = reviewReplyRepository.existsByReviewIdAndDeletedAtIsNull(reviewId);
        System.out.println("결과: existsByReviewIdAndDeletedAtIsNull(" + reviewId + ") = " + exists);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByReviewIdAndDeletedAtIsNull: 답글 없으면 false")
    void existsByReviewIdAndDeletedAtIsNullTest_false() {
        UUID reviewId = UUID.randomUUID();
        boolean exists = reviewReplyRepository.existsByReviewIdAndDeletedAtIsNull(reviewId);
        System.out.println("결과: existsByReviewIdAndDeletedAtIsNull(" + reviewId + ") = " + exists);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByReviewIdAndDeletedAtIsNull: 삭제된 답글은 false (1:1 재작성 가능해야 함)")
    void existsByReviewIdAndDeletedAtIsNullTest_false_afterDelete() {
        UUID reviewId = UUID.randomUUID();
        ReviewReply reply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(UUID.randomUUID())
                .content("삭제될 답글")
                .build();
        ReviewReply saved = reviewReplyRepository.save(reply);
        saved.softDelete(1L);
        reviewReplyRepository.save(saved);

        boolean exists = reviewReplyRepository.existsByReviewIdAndDeletedAtIsNull(reviewId);
        System.out.println("결과: existsByReviewIdAndDeletedAtIsNull(" + reviewId + ") = " + exists);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByReviewIdAndDeletedAtIsNull: 삭제 안 된 답글 조회됨")
    void findByReviewIdAndDeletedAtIsNullTest_found() {
        UUID reviewId = UUID.randomUUID();
        ReviewReply reply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(UUID.randomUUID())
                .content("조회될 답글")
                .build();
        reviewReplyRepository.save(reply);

        Optional<ReviewReply> result = reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId);
        System.out.println("결과: " + result.map(ReviewReply::getContent));

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("조회될 답글");
    }

    @Test
    @DisplayName("findByReviewIdAndDeletedAtIsNull: 삭제된 답글은 조회 안 됨")
    void findByReviewIdAndDeletedAtIsNullTest_notFound_afterDelete() {
        UUID reviewId = UUID.randomUUID();
        ReviewReply reply = ReviewReply.builder()
                .reviewId(reviewId)
                .storeId(UUID.randomUUID())
                .content("삭제될 답글")
                .build();
        ReviewReply saved = reviewReplyRepository.save(reply);
        saved.softDelete(1L);
        reviewReplyRepository.save(saved);

        Optional<ReviewReply> result = reviewReplyRepository.findByReviewIdAndDeletedAtIsNull(reviewId);
        System.out.println("결과: " + result);

        assertThat(result).isEmpty();
    }
}