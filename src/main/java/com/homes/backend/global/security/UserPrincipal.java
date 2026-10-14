package com.homes.backend.global.security;

import com.homes.backend.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String role;

    // 생성자: User 엔티티를 받아서 시큐리티 규격 상자로 변환
    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    // 아래 메서드들은 스프링 시큐리티가 내부적으로 검증할 때 쓰는 필수 규칙들
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // "ROLE_" 접두사는 스프링 시큐리티의 hasRole() 표현식이 요구하는 규칙
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email; //이메일로 로그인하니까 username 자리에 이메일
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}