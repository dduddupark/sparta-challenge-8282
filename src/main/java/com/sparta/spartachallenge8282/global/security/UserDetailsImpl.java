package com.sparta.spartachallenge8282.global.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security UserDetails
 * <p>
 * JWT 파싱 결과(userId, email, role)를 SecurityContext에 저장하여
 * 컨트롤러에서 @AuthenticationPrincipal로 사용할 수 있다.
 *
 * <pre>
 * @AuthenticationPrincipal UserDetailsImpl userDetails
 *
 * userDetails.userId()      → DB PK
 * userDetails.email()       → 로그인 이메일
 * userDetails.getUsername() → Spring Security에서 사용하는 username(email 반환)
 * userDetails.role()        → ROLE_CUSTOMER
 * </pre>
 */
public record UserDetailsImpl(Long userId, String email, String role) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return null;
    }  // JWT 방식이므로 사용 안 함

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
