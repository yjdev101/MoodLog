package com.example.moodlog.domain.auth.repository;

import com.example.moodlog.domain.auth.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    boolean existsByJtiAndExpiresAtAfter(String jti, Instant now);

    void deleteByExpiresAtBefore(Instant now);
}
