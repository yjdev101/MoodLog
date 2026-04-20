package com.example.moodlog.domain.user.security;

import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // id로 먼저 시도, 실패하면 email로 조회 (일반 로그인 호환)
        try {
            Long id = Long.parseLong(username);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
            return new CustomerUserDetails(user);
        } catch (NumberFormatException e) {
            // username이 숫자가 아니면 email로 조회 (기존 로그인 방식)
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
            return new CustomerUserDetails(user);
        }
    }
}
