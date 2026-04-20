package com.example.moodlog.domain.user.repository;

import com.example.moodlog.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회 (로그인용)
    Optional<User> findByEmail(String email);

    // 이메일 중복 체크
    boolean existsByEmail(String email);

    // 닉네임으로 조회

    Optional<User> findByNickname(String nickname);

    // OAuth2
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}