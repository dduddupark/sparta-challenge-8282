package com.sparta.spartachallenge8282.user.entity;

import com.sparta.spartachallenge8282.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 엔티티
 * <p>
 * 소프트 딜리트를 지원하기 위해 JPA의 unique 제약은 사용하지 않는다.
 * PostgreSQL에서는 반드시 Partial Unique Index
 * (email WHERE deleted_at IS NULL)를 생성해야 한다.
 */
@Entity
@Table(name = "p_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email; // 로그인 아이디 겸용 (재가입 지원을 위해 unique = true 제거)

    @Column(nullable = false, length = 255)
    private String password; // 암호화된 비밀번호

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(nullable = false, length = 100)
    private String address; // 배달 주소

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.CUSTOMER; // 기본값 CUSTOMER

    @Column(columnDefinition = "TEXT")
    private String refreshToken; // 만료 시간 대비 TEXT 타입 지정

    @Builder
    public User(String email, String password, String nickname, String address, UserRole role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.address = address;
        this.role = (role != null) ? role : UserRole.CUSTOMER;
    }

    // ── 비즈니스 편의 메서드 ───────────────────────────────────────────────────

    /**
     * 내 정보 수정 (닉네임, 주소)
     */
    public void updateProfile(String nickname, String address) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (address != null && !address.isBlank()) {
            this.address = address;
        }
    }

    /**
     * 비밀번호 변경
     */
    public void updatePassword(String encodedPassword) {
        if (encodedPassword != null && !encodedPassword.isBlank()) {
            this.password = encodedPassword;
        }
    }

    /**
     * 리프레시 토큰 저장/갱신
     */
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * 로그아웃 시 리프레시 토큰 비우기
     */
    public void clearRefreshToken() {
        this.refreshToken = null;
    }

    /**
     * 역할 변경 (MASTER/MANAGER 전용)
     */
    public void updateRole(UserRole newRole) {
        if (newRole != null) {
            this.role = newRole;
        }
    }
}
