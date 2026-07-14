package com.sparta.spartachallenge8282.user.domain;

import com.sparta.spartachallenge8282.global.common.BaseEnum;

/**
 * 사용자 역할 Enum.
 *
 * <ul>
 *   <li>CUSTOMER - 일반 고객 (셀프 가입 가능)</li>
 *   <li>OWNER    - 가게 주인 (셀프 가입 가능)</li>
 *   <li>MANAGER  - 서비스 관리자 (셀프 가입 불가, MASTER가 생성)</li>
 *   <li>MASTER   - 최종 관리자 (셀프 가입 불가)</li>
 * </ul>
 */
public enum UserRole implements BaseEnum {
    CUSTOMER("CUSTOMER", "일반 고객"),
    OWNER("OWNER", "가게 주인"),
    MANAGER("MANAGER", "서비스 관리자"),
    MASTER("MASTER", "최종 관리자");

    private final String code;
    private final String description;

    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    /**
     * Spring Security 권한 문자열 반환 (ex. "ROLE_CUSTOMER")
     */
    public String getAuthority() {
        return "ROLE_" + this.code;
    }
}
