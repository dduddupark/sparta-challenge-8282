package com.sparta.spartachallenge8282.user.repository;

import com.sparta.spartachallenge8282.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 도메인 Repository 인터페이스.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 로그인 및 일반 사용자 조회
     */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    /**
     * 회원 정보 수정 및 단건 사용자 조회
     */
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 회원가입 시 이메일 중복 체크 (탈퇴하지 않은 유저 중에서만 중복 여부 확인)
     */
    boolean existsByEmailAndDeletedAtIsNull(String email);

    /**
     * [관리자용] 사용자 목록 전체 조회
     */
    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    /**
     * [관리자용] 탈퇴한 회원 목록 전체 조회
     */
    Page<User> findAllByDeletedAtIsNotNull(Pageable pageable);
}
