package com.sparta.spartachallenge8282.user.domain;

import com.sparta.spartachallenge8282.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

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
     * 토큰 재발급 시 동시성 제어를 위한 조회
     * 메서드명을 구분하기 위해 @Query로 JPQL을 명시하고 PESSIMISTIC_WRITE 락을 적용한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                select u
                from User u
                where u.email = :email
                  and u.deletedAt is null
            """)
    Optional<User> findByEmailAndDeletedAtIsNullForUpdate(@org.springframework.data.repository.query.Param("email") String email);

    /**
     * 회원 정보 수정 및 단건 사용자 조회
     */
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 회원가입 시 이메일 중복 체크 (탈퇴하지 않은 유저 중에서만 중복 여부 확인)
     */
    boolean existsByEmailAndDeletedAtIsNull(String email);

    /**
     * 사용자 존재 여부 확인 (탈퇴하지 않은 유저 대상)
     */
    boolean existsByIdAndDeletedAtIsNull(Long id);

    /**
     * [관리자용] 사용자 목록 전체 조회
     */
    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    /**
     * [관리자용] 탈퇴한 회원 목록 전체 조회
     */
    Page<User> findAllByDeletedAtIsNotNull(Pageable pageable);

}
