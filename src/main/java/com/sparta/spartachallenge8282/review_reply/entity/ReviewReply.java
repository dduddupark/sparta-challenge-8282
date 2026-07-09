package com.sparta.spartachallenge8282.review_reply.entity;


import com.sparta.spartachallenge8282.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_review_reply")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewReply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(nullable = false, unique = true)
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
