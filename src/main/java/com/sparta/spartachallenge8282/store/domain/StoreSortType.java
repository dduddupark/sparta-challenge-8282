package com.sparta.spartachallenge8282.store.domain;

import com.sparta.spartachallenge8282.global.common.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StoreSortType implements BaseEnum {


    LATEST("LATEST", "최신 등록 순"),
    RATING_DESC("RATING_DESC", "평점 높은 순"),
    REVIEW_COUNT_DESC("REVIEW_COUNT_DESC", "리뷰 많은 순"),
    DELIVERY_FEE_ASC("DELIVERY_FEE_ASC", "배달비 낮은 순");

    private final String code;
    private final String description;
}



