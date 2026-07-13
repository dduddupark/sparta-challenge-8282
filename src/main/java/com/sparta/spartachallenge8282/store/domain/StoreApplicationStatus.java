package com.sparta.spartachallenge8282.store.domain;

import com.sparta.spartachallenge8282.global.common.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StoreApplicationStatus implements BaseEnum {

    PENDING("PENDING", "등록 신청"),
    APPROVED("APPROVED", "승인"),
    REJECTED("REJECTED", "거절");

    private final String code;
    private final String description;
}
