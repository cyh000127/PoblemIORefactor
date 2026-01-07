package com.problemio.global.auth;

import com.problemio.user.domain.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    // 권한 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = user.getRole();
        if (role == null || role.isEmpty()) {
            role = "ROLE_USER"; 
        }
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    // 계정 만료 여부 (true: 만료 안 됨)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // 계정 잠김 여부 (true: 안 잠김)
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // 비밀번호 만료 여부 (true: 만료 안 됨)
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 계정 활성화 여부 (true: 활성화) -> 탈퇴한 유저면 false 처리
    @Override
    public boolean isEnabled() {
        return !user.isDeleted();
    }
}