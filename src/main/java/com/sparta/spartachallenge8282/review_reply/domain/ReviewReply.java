package com.sparta.spartachallenge8282.review_reply.domain;


import com.sparta.spartachallenge8282.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 가게 사장님(OWNER)이 리뷰에 남기는 답글 엔티티.
 *
 * 리뷰(Review)와 1:1 관계다 - deletedAt이 NULL인 행 사이에서만 reviewId가 유니크하도록
 * DB partial index(uq_review_reply_review_id_active)로 강제한다 (soft delete 후 재작성 허용을 위함).
 * 자세한 배경은 review 패키지 내 SQL 기록 파일 참고.
 * 별도의 답글 단건 조회 API는 없으며, 리뷰 상세/목록 조회 응답에 포함되어 함께 내려간다.
 *
 * storeId 는 review.getStoreId() 에서 그대로 복사해와 저장한다.
 * 매 요청마다 Review → Store 로 조인해서 소유주를 확인하는 대신, 이 컬럼으로
 * "어느 가게의 답글인지"를 바로 조회/필터링할 수 있게 하기 위함이다.
 *
 * 상태 변경은 update() / BaseEntity.softDelete() 로만 수행한다.
 */

@Entity
@Table(name = "p_review_reply")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewReply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(nullable = false)
    private UUID reviewId;

    @Column(nullable = false)
    private UUID storeId;


    @Column(nullable = false, length = 500)
    private String content;

    @Builder
    private ReviewReply(UUID reviewId, UUID storeId, String content) {
        this.reviewId = reviewId;
        this.storeId = storeId;
        this.content = content;
    }

    public void update(String content) {
        this.content = content;
    }

}
