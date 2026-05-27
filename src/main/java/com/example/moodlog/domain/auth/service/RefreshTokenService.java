package com.example.moodlog.domain.auth.service;

import com.example.moodlog.domain.auth.entity.RefreshToken;
import com.example.moodlog.domain.auth.repository.RefreshTokenRepository;
import com.example.moodlog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    // 새 refresh token 생성 + 저장
    @Transactional
    public RefreshToken createRefreshToken(User user, long durationSeconds) {

        // 기존 토큰이 있으면 업데이트, 없으면 새로 생성
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElseGet(() -> RefreshToken.builder()
                        .user(user)
                        .token(UUID.randomUUID().toString())
                        .expiryDate(Instant.now().plusSeconds(durationSeconds))
                        .build());

        // 토큰값과 만료일 갱신
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusSeconds(durationSeconds));

        return refreshTokenRepository.save(refreshToken);
    }

    // refresh token 조회
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
    }


    // 유효성 체크
    public boolean isValid(RefreshToken token) {
        return token.getExpiryDate().isAfter(Instant.now());
    }

    // 사용자 토큰 삭제 (로그아웃 등)
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}