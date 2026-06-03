package com.example.moodlog.domain.user.service;

import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.entity.UserProfile;
import com.example.moodlog.domain.user.repository.UserProfileRepository;
import com.example.moodlog.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileRepository userProfileRepository;

    // 회원가입
    public User signup(String email, String password, String nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();

        User savedUser = userRepository.save(user);

        // 회원가입 시 프로필 자동생성
        UserProfile profile = new UserProfile();
        profile.setUser(savedUser);
        profile.setNickname(nickname);
        profile.setCreatedAt(LocalDateTime.now());

        userProfileRepository.save(profile);

        return savedUser;
    }

    // ** (열거 공격) 로그인
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalStateException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;
    }

    // ID로 조회
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
    }
}
