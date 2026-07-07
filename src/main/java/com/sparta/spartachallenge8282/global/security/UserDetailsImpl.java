package com.sparta.spartachallenge8282.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security UserDetails 구현체.
 * JWT 파싱 결과(userId, email, role)를 SecurityContext에서 참조할 수 있도록 담는다.
 *
 * <pre>
 * 컨트롤러에서 사용:
 *   @AuthenticationPrincipal UserDetailsImpl userDetails
 *   userDetails.getUserId()   → Long
 *   userDetails.getEmail()    → String
 * </pre>
 */
@Getter
public class UserDetailsImpl implements UserDetails {

    private final Long userId;
    private final String email;
    private final String role;

    public UserDetailsImpl(Long userId, String email, String role) {
        this.userId = userId;
        this.email  = email;
        this.role   = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return null; }  // JWT 방식이므로 사용 안 함
    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isAccountNonLocked()    { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()             { return true; }
}
