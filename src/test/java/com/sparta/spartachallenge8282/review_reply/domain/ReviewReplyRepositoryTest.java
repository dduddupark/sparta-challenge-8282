package com.sparta.spartachallenge8282.review_reply.domain;

import com.sparta.spartachallenge8282.global.config.JpaAuditingConfig; // 실제 패키지 경로로 확인 필요
import com.sparta.spartachallenge8282.global.config.QueryDslConfig;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaAuditingConfig.class,
        QueryDslConfig.class
})
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

    @Test
    @DisplayName("findByStoreIdAndDeletedAtIsNull: 삭제 안 된 답글만 조회")
    void findByStoreIdAndDeletedAtIsNullTest() {
        UUID storeId = UUID.randomUUID();

        ReviewReply active = ReviewReply.builder()
                .reviewId(UUID.randomUUID())
                .storeId(storeId)
                .content("살아있는 답글")
                .build();
        reviewReplyRepository.save(active);

        ReviewReply deleted = ReviewReply.builder()
                .reviewId(UUID.randomUUID())
                .storeId(storeId)
                .content("삭제된 답글")
                .build();
        ReviewReply savedDeleted = reviewReplyRepository.save(deleted);
        savedDeleted.softDelete(1L);
        reviewReplyRepository.save(savedDeleted);

        Pageable pageable = PageRequest.of(0, 10);
        Slice<ReviewReply> result = reviewReplyRepository.findByStoreIdAndDeletedAtIsNull(storeId, pageable);
        System.out.println("결과: " + result.getContent().size() + "건 조회됨");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("살아있는 답글");
    }

    @Test
    @DisplayName("findByStoreIdAndDeletedAtIsNull: 답글 없는 가게는 빈 결과")
    void findByStoreIdAndDeletedAtIsNullTest_empty() {
        UUID storeId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Slice<ReviewReply> result = reviewReplyRepository.findByStoreIdAndDeletedAtIsNull(storeId, pageable);
        System.out.println("결과: " + result.getContent().size() + "건 조회됨");

        assertThat(result.getContent()).isEmpty();
    }
}