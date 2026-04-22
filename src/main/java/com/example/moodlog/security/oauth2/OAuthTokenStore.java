package com.example.moodlog.security.oauth2;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthTokenStore {

    private final ConcurrentHashMap<String, TokenEntry> store = new ConcurrentHashMap<>();

    public String generateCode(String accessToken, String refreshToken) {
        String code = UUID.randomUUID().toString();
        store.put(code, new TokenEntry(accessToken, refreshToken, Instant.now().plusSeconds(30)));
        return code;
    }

    public Optional<TokenEntry> consume(String code) {
        TokenEntry entry = store.remove(code);
        if (entry == null || entry.isExpired()) return Optional.empty();
        return Optional.of(entry);
    }

    public record TokenEntry(String accessToken, String refreshToken, Instant expiresAt) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
