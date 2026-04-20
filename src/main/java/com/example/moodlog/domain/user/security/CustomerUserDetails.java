package com.example.moodlog.domain.user.security;

import com.example.moodlog.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CustomerUserDetails implements UserDetails {

    private final User user;

    public Long getId() {
        return user.getId();
    }

    public String getNickName() {
        return user.getNickname();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE_USER 고정, 필요시 enum 등으로 확장 가능
        return List.of(() -> "ROLE_USER");
    }

    @Override
    public String getPassword() { return user.getPassword(); }

    @Override
    public String getUsername() { return String.valueOf(user.getId()); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
