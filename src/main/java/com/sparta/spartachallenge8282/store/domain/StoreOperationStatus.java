package com.sparta.spartachallenge8282.store.domain;

import com.sparta.spartachallenge8282.global.common.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StoreOperationStatus implements BaseEnum {

    PREPARING("PREPARING", "운영 준비"),
    ACTIVE("ACTIVE", "운영 중"),
    CLOSE_REQUESTED("CLOSE_REQUESTED", "폐점 요청"),
    CLOSED("CLOSED", "폐점 완료");

    private final String code;
    private final String description;
}
