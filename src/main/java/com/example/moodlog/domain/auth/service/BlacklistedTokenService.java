package com.example.moodlog.domain.auth.service;

import com.example.moodlog.domain.auth.entity.BlacklistedToken;
import com.example.moodlog.domain.auth.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BlacklistedTokenService {

    private final BlacklistedTokenRepository repository;

    @Transactional
    public void blacklist(String jti, Instant expiresAt) {
        BlacklistedToken token = BlacklistedToken.builder()
                .jti(jti)
                .expiresAt(expiresAt)
                .build();
        repository.save(token);
    }

    public boolean isBlacklisted(String jti) {
        return repository.existsByJtiAndExpiresAtAfter(jti, Instant.now());
    }

    // 만료된 항목 주기적 정리 (매 1시간)
    @Transactional
    @Scheduled(fixedRate = 3_600_000)
    public void cleanupExpired() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }
}
