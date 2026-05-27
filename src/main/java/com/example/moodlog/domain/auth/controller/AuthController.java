package com.example.moodlog.domain.auth.controller;

import com.example.moodlog.domain.auth.entity.RefreshToken;
import com.example.moodlog.domain.auth.service.RefreshTokenService;
import com.example.moodlog.domain.auth.dto.LoginRequest;
import com.example.moodlog.domain.auth.dto.LoginResponse;
import com.example.moodlog.domain.auth.dto.RefreshRequest;
import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.service.UserService;
import com.example.moodlog.security.jwt.JwtTokenProvider;
import com.example.moodlog.security.oauth2.OAuthTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final OAuthTokenStore oAuthTokenStore;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 1. 로그인 검증
        User user = userService.login(request.getEmail(), request.getPassword());

        // 2. access token 생성
        String accessToken = jwtTokenProvider.createToken(String.valueOf(user.getId()));

        // 3. refresh token 생성 (DB 저장)
        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user, 60 * 60 * 24 * 7);

        // 4. 반환
        return ResponseEntity.ok(
                new LoginResponse(
                        accessToken,
                        refreshToken.getToken()
                )
        );
    }

    // 소셜 로그인 후 일회용 코드를 실제 토큰으로 교환
    @GetMapping("/oauth-token")
    public ResponseEntity<?> exchangeOAuthCode(@RequestParam String code) {
        return oAuthTokenStore.consume(code)
                .map(entry -> ResponseEntity.ok(new LoginResponse(entry.accessToken(), entry.refreshToken())))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {

        String refreshToken = request.getRefreshToken();

        // refresh token 조회
        RefreshToken token = refreshTokenService.findByToken(refreshToken);

        // 만료 체크
        if (!refreshTokenService.isValid(token)) {
            return ResponseEntity.status(401).body("Refersh token이 만료되었습니다.");
        }

        // 사용자 가져오기
        User user = token.getUser();

        // 새 access token 생성
        String newAccessToken =
                jwtTokenProvider.createToken(String.valueOf(user.getId()));

        return ResponseEntity.ok(newAccessToken);
    }

}
