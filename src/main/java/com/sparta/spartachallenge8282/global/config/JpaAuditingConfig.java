package com.sparta.spartachallenge8282.global.config;

import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing 활성화 설정.
 *
 * <p>AuditorAware 빈을 등록하여 @CreatedBy / @LastModifiedBy 필드에
 * 현재 인증된 사용자의 ID(Long)를 자동으로 주입한다.
 *
 * <p>비인증 요청(회원가입·로그인 등)에서는 Optional.empty()를 반환하므로
 * createdBy 컬럼이 null 허용인지 확인 후 조정한다.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
                return Optional.empty();
            }

            return Optional.of(userDetails.getUserId());
        };
    }
}
